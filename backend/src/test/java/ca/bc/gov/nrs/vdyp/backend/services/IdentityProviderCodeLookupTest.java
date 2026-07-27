package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.IdentityProviderCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.IdentityProviderCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.IdentityProviderCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.IdentityProviderCodeRepository;

class IdentityProviderCodeLookupTest {
	IdentityProviderCodeRepository repository;

	IdentityProviderCodeResourceAssembler assembler;

	IdentityProviderCodeLookup lookup;

	IdentityProviderCodeEntity idir = new IdentityProviderCodeEntity();
	IdentityProviderCodeEntity bceid = new IdentityProviderCodeEntity();

	@BeforeEach
	void setup() {
		repository = Mockito.mock(IdentityProviderCodeRepository.class);
		assembler = new IdentityProviderCodeResourceAssembler();

		idir.setCode(IdentityProviderCodeModel.IDIR);
		idir.setDisplayOrder(BigDecimal.ONE);
		bceid.setCode(IdentityProviderCodeModel.BCEID);
		bceid.setDisplayOrder(BigDecimal.TEN);

		when(repository.listAll()).thenReturn(List.of(idir, bceid));
		lookup = new IdentityProviderCodeLookup(repository, assembler);
		lookup.init();
	}

	static List<Arguments> claimAndExpectedCode() {
		return List.of(
				Arguments.of("azureidir", "IDIR"), //
				Arguments.of("AzureIDIR", "IDIR"), //
				Arguments.of("bceidbusiness", "BCEID"), //
				Arguments.of("BCeIDBusiness", "BCEID"), //
				Arguments.of("unknown-idp", null), //
				Arguments.of((String) null, null)
		);
	}

	@ParameterizedTest
	@MethodSource("claimAndExpectedCode")
	void test_getIdentityProviderCodeFromClaim(String claim, String expectedCode) {
		var result = lookup.getIdentityProviderCodeFromClaim(claim);
		if (expectedCode == null) {
			Assertions.assertTrue(result.isEmpty());
		} else {
			assertThat(result.get().getCode()).isEqualTo(expectedCode);
		}
	}
}
