package ca.bc.gov.nrs.vdyp.common_calculators;

import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexSpecies.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common_calculators.SiteIndexNames.SpeciesConversionParamsDetails;

class SiteIndexNamesTest {

	@Test
	void testTypicalUsage() {
		SpeciesConversionParamsDetails details = SiteIndexNames.getSpeciesConversionParams(SI_SPEC_HWC, SI_SPEC_FDC);

		assertThat(details.param1(), is(0.48053393));
		assertThat(details.param2(), is(1.11234705));
	}

	@Test
	void testNoEntry() {
		SpeciesConversionParamsDetails details = SiteIndexNames.getSpeciesConversionParams(SI_SPEC_HWC, SI_SPEC_HWC);

		assertThat(details, nullValue());
	}
}
