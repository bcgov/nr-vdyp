package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.VDYPUserResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;
import io.quarkus.security.identity.SecurityIdentity;

@ExtendWith(MockitoExtension.class)
class VDYPUserServiceTest {

	@Mock
	VDYPUserRepository userRepository;

	VDYPUserResourceAssembler assembler;

	@Mock
	UserTypeCodeLookup userTypeLookup;

	@Mock
	SecurityIdentity identity;

	@Mock
	JsonWebToken jwt;

	VDYPUserService service;

	@BeforeEach
	void setUp() {
		assembler = new VDYPUserResourceAssembler();
		service = new VDYPUserService(userRepository, assembler, userTypeLookup) {

		};
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_returnsNull_whenIdentityIsNull() {
		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(null);

		assertThat(result).isNull();
		verifyNoInteractions(userRepository, userTypeLookup);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_returnsNull_whenIdentityIsAnonymous() {
		when(identity.isAnonymous()).thenReturn(true);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result).isNull();
		verifyNoInteractions(userRepository, userTypeLookup);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_returnsNull_whenPrincipalNotJwt() {
		when(identity.isAnonymous()).thenReturn(false);
		Principal somePrincipal = mock(Principal.class);
		when(identity.getPrincipal()).thenReturn(somePrincipal);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result).isNull();
		verifyNoInteractions(userRepository, userTypeLookup);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_returnsExistingUser_whenFoundByOidc() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");

		VDYPUserEntity entity = new VDYPUserEntity();
		UUID internalID = UUID.randomUUID();
		entity.setVdypUserGUID(internalID);
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.of(entity));

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getVdypUserGUID()).isEqualTo(internalID.toString());

		verify(userRepository).findByOIDC("1234567890@fakeid");
		verifyNoInteractions(userTypeLookup);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_createsNewUser_whenNotFoundByOidc() {
		// identity + jwt basics
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("given_name")).thenReturn("Russell");
		when(jwt.getClaim("family_name")).thenReturn("Wilson");

		// no existing user
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.empty());

		// roles and user type
		Set<String> roles = Set.of("Admin", "Super User");
		when(identity.getRoles()).thenReturn(roles);

		UserTypeCodeModel userTypeCode = new UserTypeCodeModel();
		userTypeCode.setCode("ADMIN");
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(userTypeCode);

		// call
		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		// assertions
		assertThat(result).isNotNull();
		assertThat(result.getOidcGUID()).isEqualTo("1234567890@fakeid");
		assertThat(result.getFirstName()).isEqualTo("Russell");
		assertThat(result.getLastName()).isEqualTo("Wilson");
		assertThat(result.getUserTypeCode()).isEqualTo(userTypeCode);
	}

	@Test
	void createUser_throwsWhenOidcGuidBlank() {
		VDYPUserModel requested = new VDYPUserModel();
		requested.setOidcGUID("  "); // blank

		assertThatThrownBy(() -> service.createUser(requested)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid User Business Identifier");

		// no downstream calls at all
		verifyNoInteractions(userRepository);
	}

	@Test
	void createUser_throwsWhenUserAlreadyExists() {
		VDYPUserModel requested = new VDYPUserModel();
		requested.setOidcGUID("091234589@fakeidir");

		VDYPUserEntity existing = new VDYPUserEntity();

		when(userRepository.findByOIDC("091234589@fakeidir")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.createUser(requested)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("User already exists");

		verify(userRepository).findByOIDC("091234589@fakeidir");
		// no entity creation, no persist
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	void createUser_persistsAndReturnsModel_whenUserDoesNotExist() {
		// arrange
		VDYPUserModel requested = new VDYPUserModel();
		requested.setOidcGUID("091234589@fakeidir");
		requested.setFirstName("Russell");
		requested.setLastName("Wilson");

		when(userRepository.findByOIDC("091234589@fakeidir")).thenReturn(Optional.empty());

		VDYPUserModel returnedModel = new VDYPUserModel();
		returnedModel.setOidcGUID("091234589@fakeidir");
		returnedModel.setFirstName("Russell");
		returnedModel.setLastName("Wilson");

		// act
		VDYPUserModel result = service.createUser(requested);

		// assert behaviour & mapping
		assertThat(result).isEqualTo(returnedModel);
		assertThat(result.getOidcGUID()).isEqualTo("091234589@fakeidir");
	}

	@Test
	void getAllUsers_returnsAllUsers() {
		VDYPUserEntity entity1 = new VDYPUserEntity();
		VDYPUserEntity entity2 = new VDYPUserEntity();

		when(userRepository.listAll()).thenReturn(Stream.of(entity1, entity2).toList());
		List<VDYPUserModel> users = service.getAllUsers();
		assertThat(users).hasSizeGreaterThanOrEqualTo(2);
		assertThat(users).hasSizeLessThanOrEqualTo(2);
		assertThat(users).hasOnlyElementsOfType(VDYPUserModel.class);
	}

	@Test
	void getUserById_returnExistingUser() {
		UUID exists = UUID.randomUUID();
		VDYPUserEntity entity = new VDYPUserEntity();
		entity.setVdypUserGUID(exists);

		when(userRepository.findById(exists)).thenReturn(entity);
		assertThat(service.getUserById(exists).getVdypUserGUID()).isEqualTo( (exists.toString()));
	}

	@Test
	void getUserById_returnsNullUser() {
		UUID doesNotExist = UUID.randomUUID();
		when(userRepository.findById(doesNotExist)).thenReturn(null);
		assertThat(service.getUserById(doesNotExist)).isNull();
	}
}
