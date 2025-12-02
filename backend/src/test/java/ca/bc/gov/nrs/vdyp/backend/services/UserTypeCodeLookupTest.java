package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.UserTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.UserTypeCodeRepository;

public class UserTypeCodeLookupTest {
	UserTypeCodeRepository repository;

	UserTypeCodeResourceAssembler assembler;

	UserTypeCodeLookup lookup;

	UserTypeCodeEntity admin = new UserTypeCodeEntity();
	UserTypeCodeEntity superUser = new UserTypeCodeEntity();
	UserTypeCodeEntity user = new UserTypeCodeEntity();

	@BeforeEach
	void setup() {
		repository = Mockito.mock(UserTypeCodeRepository.class);
		assembler = new UserTypeCodeResourceAssembler();

		admin.setUserTypeCode("ADMIN");
		admin.setDisplayOrder(BigDecimal.ZERO);
		superUser.setUserTypeCode("SUPERUSER");
		superUser.setDisplayOrder(BigDecimal.ONE);
		user.setUserTypeCode("USER");
		user.setDisplayOrder(BigDecimal.TEN);

		// when(repository.findById("ADMIN")).thenReturn(admin);
		// when(repository.findById("SUPERUSER")).thenReturn(superUser);
		// when(repository.findById("USER")).thenReturn(user);
		when(repository.listAll()).thenReturn(List.of(admin, superUser, user));
		lookup = new UserTypeCodeLookup(repository, assembler);
		lookup.init();

	}

	// This test is written to match the current business rule but if we are going to require a provisioned user we
	// should change the logic
	static List<Arguments> roleAndExpectedCode() {
		return List.of(
				Arguments.of(Set.of(), "USER"), //
				Arguments.of(Set.of(""), "USER"), //
				Arguments.of(Set.of("Admin"), "ADMIN"), //
				Arguments.of(Set.of("Super User"), "SUPERUSER"), //
				Arguments.of(Set.of("User"), "USER"), //
				Arguments.of(Set.of("Unknown"), "USER"), //
				Arguments.of(Set.of("User", "Super User"), "SUPERUSER"), //
				Arguments.of(Set.of("User", "Admin"), "ADMIN"), //
				Arguments.of(Set.of("Super User", "User", "Admin"), "ADMIN"), //
				Arguments.of(Set.of("Super User", "User"), "SUPERUSER")
		);

	}

	@ParameterizedTest
	@MethodSource("roleAndExpectedCode")
	void test_getUserTypeCodeFromExternalRoles_NoRoles(Set<String> userRoles, String expectedTypeCode) {
		UserTypeCodeModel type = lookup.getUserTypeCodeFromExternalRoles(userRoles);

		assertThat(type.getUserTypeCode()).isEqualTo(expectedTypeCode);
	}

	@Test
	void test_require_NonExistant_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> lookup.require(""));
	}

	@Test
	void test_findNull_returnsEmpty() {
		assertThat(lookup.find(null)).isSameAs(Optional.empty());
	}

	// This test is written to match the current business rule but if we are going to require a provisioned user we
	// should change the logic
	static List<Arguments> normalizeString() {
		return List.of(
				Arguments.of(null, null), //
				Arguments.of("", ""), //
				Arguments.of("UsEr", "USER"), //
				Arguments.of("USER ", "USER"), //
				Arguments.of(" USER", "USER"), //
				Arguments.of(" USER ", "USER"), //
				Arguments.of(" user ", "USER")
		);

	}

	@ParameterizedTest
	@MethodSource("normalizeString")
	void test_normalize(String string, String normalizedString) {
		assertThat(lookup.normalize(string)).isEqualTo(normalizedString);
	}

}
