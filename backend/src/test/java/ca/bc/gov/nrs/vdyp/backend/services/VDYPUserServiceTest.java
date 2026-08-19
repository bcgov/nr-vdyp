package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import ca.bc.gov.nrs.vdyp.backend.data.entities.IdentityProviderCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.IdentityProviderCodeModel;
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
	IdentityProviderCodeLookup identityProviderLookup;

	@Mock
	SecurityIdentity identity;

	@Mock
	JsonWebToken jwt;

	VDYPUserService service;

	@BeforeEach
	void setUp() {
		assembler = new VDYPUserResourceAssembler();
		service = new VDYPUserService(userRepository, assembler, userTypeLookup, identityProviderLookup) {

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

		Set<String> roles = Set.of("User");
		when(identity.getRoles()).thenReturn(roles);
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(null);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getVdypUserGUID()).isEqualTo(internalID.toString());

		verify(userRepository).findByOIDC("1234567890@fakeid");
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_updatesUserTypeCode_onExistingUser_whenRoleChanged() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("identity_provider")).thenReturn(null);

		VDYPUserEntity entity = new VDYPUserEntity();
		UUID internalID = UUID.randomUUID();
		entity.setVdypUserGUID(internalID);
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.of(entity));

		Set<String> roles = Set.of("Admin");
		when(identity.getRoles()).thenReturn(roles);

		UserTypeCodeModel adminType = new UserTypeCodeModel();
		adminType.setCode(UserTypeCodeModel.ADMIN);
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(adminType);

		var adminEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity();
		adminEntity.setCode(UserTypeCodeModel.ADMIN);
		when(userTypeLookup.requireEntity(UserTypeCodeModel.ADMIN)).thenReturn(adminEntity);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getUserTypeCode().getCode()).isEqualTo(UserTypeCodeModel.ADMIN);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_doesNotUpdateUserTypeCode_onExistingUser_whenNoValidRole() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("identity_provider")).thenReturn(null);

		VDYPUserEntity entity = new VDYPUserEntity();
		UUID internalID = UUID.randomUUID();
		entity.setVdypUserGUID(internalID);
		var existingType = new ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity();
		existingType.setCode(UserTypeCodeModel.USER);
		entity.setUserTypeCode(existingType);
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.of(entity));

		Set<String> roles = Set.of("UnknownRole");
		when(identity.getRoles()).thenReturn(roles);
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(null);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getUserTypeCode().getCode()).isEqualTo(UserTypeCodeModel.USER);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_doesNotUpdateUserTypeCode_onExistingUser_whenSystemRole() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("identity_provider")).thenReturn(null);

		VDYPUserEntity entity = new VDYPUserEntity();
		UUID internalID = UUID.randomUUID();
		entity.setVdypUserGUID(internalID);
		var existingType = new ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity();
		existingType.setCode(UserTypeCodeModel.USER);
		entity.setUserTypeCode(existingType);
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.of(entity));

		Set<String> roles = Set.of("System");
		when(identity.getRoles()).thenReturn(roles);

		UserTypeCodeModel systemType = new UserTypeCodeModel();
		systemType.setCode(UserTypeCodeModel.SYSTEM);
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(systemType);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getUserTypeCode().getCode()).isEqualTo(UserTypeCodeModel.USER);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_returnsNull_whenNoValidRoleFound() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");

		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.empty());

		Set<String> roles = Set.of("UnknownRole");
		when(identity.getRoles()).thenReturn(roles);
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(null);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result).isNull();
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_createsNewUser_whenNotFoundByOidc() {
		// identity + jwt basics
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("given_name")).thenReturn("Russell");
		when(jwt.getClaim("family_name")).thenReturn("Wilson");
		when(jwt.getClaim("identity_provider")).thenReturn(null);

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
	void ensureVDYPUserFromSecurityIdentity_createsNewUser_setsIdentityProviderCode() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("given_name")).thenReturn("Russell");
		when(jwt.getClaim("family_name")).thenReturn("Wilson");
		when(jwt.getClaim("identity_provider")).thenReturn("azureidir");

		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.empty());

		Set<String> roles = Set.of("Admin");
		when(identity.getRoles()).thenReturn(roles);

		UserTypeCodeModel userTypeCode = new UserTypeCodeModel();
		userTypeCode.setCode("ADMIN");
		when(userTypeLookup.getUserTypeCodeFromExternalRoles(roles)).thenReturn(userTypeCode);

		IdentityProviderCodeModel idpCode = new IdentityProviderCodeModel();
		idpCode.setCode(IdentityProviderCodeModel.IDIR);
		when(identityProviderLookup.getIdentityProviderCodeFromClaim("azureidir")).thenReturn(Optional.of(idpCode));

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result).isNotNull();
		assertThat(result.getIdentityProviderCode()).isEqualTo(idpCode);
	}

	@Test
	void ensureVDYPUserFromSecurityIdentity_updatesIdentityProviderCode_onExistingUser() {
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.getPrincipal()).thenReturn(jwt);
		when(jwt.getName()).thenReturn("1234567890@fakeid");
		when(jwt.getClaim("identity_provider")).thenReturn("bceidbusiness");

		VDYPUserEntity entity = new VDYPUserEntity();
		UUID internalID = UUID.randomUUID();
		entity.setVdypUserGUID(internalID);
		when(userRepository.findByOIDC("1234567890@fakeid")).thenReturn(Optional.of(entity));

		IdentityProviderCodeModel idpCode = new IdentityProviderCodeModel();
		idpCode.setCode(IdentityProviderCodeModel.BCEID);
		when(identityProviderLookup.getIdentityProviderCodeFromClaim("bceidbusiness"))
				.thenReturn(Optional.of(idpCode));

		IdentityProviderCodeEntity idpEntity = new IdentityProviderCodeEntity();
		idpEntity.setCode(IdentityProviderCodeModel.BCEID);
		when(identityProviderLookup.requireEntity(IdentityProviderCodeModel.BCEID)).thenReturn(idpEntity);

		VDYPUserModel result = service.ensureVDYPUserFromSecurityIdentity(identity);

		assertThat(result.getVdypUserGUID()).isEqualTo(internalID.toString());
		assertThat(result.getIdentityProviderCode().getCode()).isEqualTo(IdentityProviderCodeModel.BCEID);
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

		// act
		VDYPUserModel result = service.createUser(requested);

		// assert behaviour & mapping
		assertThat(result.getOidcGUID()).isEqualTo("091234589@fakeidir");
	}

	@Test
	void getAllUsers_returnsAllUsers() {
		VDYPUserEntity entity1 = new VDYPUserEntity();
		VDYPUserEntity entity2 = new VDYPUserEntity();

		when(userRepository.listAll()).thenReturn(Stream.of(entity1, entity2).toList());
		List<VDYPUserModel> users = service.getAllUsers();
		assertThat(users) //
				.hasSizeGreaterThanOrEqualTo(2) //
				.hasSizeLessThanOrEqualTo(2) //
				.hasOnlyElementsOfType(VDYPUserModel.class);
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

	@Test
	void getSystemUser_returnsSystemUser() {
		UserTypeCodeModel systemUserType = new UserTypeCodeModel();
		systemUserType.setCode(UserTypeCodeModel.SYSTEM);

		when(userTypeLookup.requireModel(UserTypeCodeModel.SYSTEM)).thenReturn(systemUserType);

		VDYPUserModel systemUser = service.getSystemUser();

		assertThat(systemUser).isNotNull();
		assertThat(systemUser.getFirstName()).isEqualTo("System");
		assertThat(systemUser.getLastName()).isEqualTo("User");

		VDYPUserModel systemUser2 = service.getSystemUser();

		assertSame(systemUser, systemUser2);
	}
}
