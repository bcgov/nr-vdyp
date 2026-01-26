package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.UserTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.UserTypeCodeRepository;

class UserTypeCodeLookupTest {
	UserTypeCodeRepository repository;

	UserTypeCodeResourceAssembler assembler;

	UserTypeCodeLookup lookup;

	UserTypeCodeEntity admin = new UserTypeCodeEntity();
	UserTypeCodeEntity system = new UserTypeCodeEntity();
	UserTypeCodeEntity user = new UserTypeCodeEntity();

	@BeforeEach
	void setup() {
		repository = Mockito.mock(UserTypeCodeRepository.class);
		assembler = new UserTypeCodeResourceAssembler();

		admin.setCode("ADMIN");
		admin.setDisplayOrder(BigDecimal.ONE);
		system.setCode("SYSTEM");
		system.setDisplayOrder(BigDecimal.ZERO);
		user.setCode("USER");
		user.setDisplayOrder(BigDecimal.TEN);

		when(repository.listAll()).thenReturn(List.of(admin, system, user));
		lookup = new UserTypeCodeLookup(repository, assembler);
		lookup.init();

	}

	// This test is written to match the current business rule but if we are going to require a provisioned user we
	// should change the logic
	static List<Arguments> roleAndExpectedCode() {
		return List.of(
				Arguments.of(Set.of("USER"), "USER"), //
				Arguments.of(Set.of("User"), "USER"), //
				Arguments.of(Set.of("SYSTEM"), "SYSTEM"), //
				Arguments.of(Set.of("System"), "SYSTEM"), //
				Arguments.of(Set.of("Admin"), "ADMIN"), //
				Arguments.of(Set.of("ADMIN"), "ADMIN"), //
				Arguments.of(Set.of("Unknown"), null), //
				Arguments.of(Set.of("User", "Admin"), "ADMIN"), //
				Arguments.of(Set.of("SYSTEM", "User", "Admin"), "SYSTEM") //
		);

	}

	@ParameterizedTest
	@MethodSource("roleAndExpectedCode")
	void test_getUserTypeCodeFromExternalRoles_NoRoles(Set<String> userRoles, String expectedTypeCode) {
		UserTypeCodeModel type = lookup.getUserTypeCodeFromExternalRoles(userRoles);
		if (expectedTypeCode == null) {
			Assertions.assertNull(type);
		} else {
			assertThat(type.getCode()).isEqualTo(expectedTypeCode);
		}
	}

	@Test
	void test_requireModel_NonExistant_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> lookup.requireModel(""));
	}

	@Test
	void test_findModelNull_returnsEmpty() {
		assertThat(lookup.findModel(null)).isSameAs(Optional.empty());
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

	@Test
	void test_requireEntity_NonExistant_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> lookup.requireEntity(""));
	}

	@Test
	void test_findEntityNull_returnsEmpty() {
		assertThat(lookup.findEntity(null)).isSameAs(Optional.empty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "user", "SuperUSER", "ADMIN" })
	void test_findModel_Exists_returnsExpected(String code) {
		assertEquals(lookup.normalize(code), lookup.findModel(code).get().getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = { "user", "SuperUSER", "ADMIN" })
	void test_requireModel_Exists_returnsExpected(String code) {
		assertEquals(lookup.normalize(code), lookup.requireModel(code).getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = { "user", "SuperUSER", "ADMIN" })
	void test_findEntity_Exists_returnsExpected(String code) {
		assertEquals(lookup.normalize(code), lookup.findEntity(code).get().getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = { "user", "SuperUSER", "ADMIN" })
	void test_requireEntity_Exists_returnsExpected(String code) {
		assertEquals(lookup.normalize(code), lookup.requireEntity(code).getCode());
	}

}
