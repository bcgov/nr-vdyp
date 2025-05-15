package ca.bc.gov.nrs.vdyp.si32;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.si32.bec.BecZone;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZoneMethods;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP64Name;

class BecZoneMethodsTest {

	@Test
	void test_SiteTool_MoFBiomassCoefficient() {
		assertThat(
				BecZoneMethods.mofBiomassCoefficient(BecZone.AT.getText(), SP64Name.A.getText()),
				equalTo(BecZoneMethods.mofBiomassCoeffs[SP64Name.A.getOffset()][BecZone.AT.getOffset()])
		);
		assertThat(
				BecZoneMethods.mofBiomassCoefficient("at", "a"),
				equalTo(BecZoneMethods.mofBiomassCoeffs[SP64Name.A.getOffset()][BecZone.AT.getOffset()])
		);
		assertThat(BecZoneMethods.mofBiomassCoefficient("??", "??"), equalTo(-1.0f));
		assertThat(BecZoneMethods.mofBiomassCoefficient(SP64Name.A.getText(), "??"), equalTo(-1.0f));
	}

	@Test
	void test_SiteTool_IndexToBecZone() {
		assertThat(BecZoneMethods.becZoneToIndex(BecZone.AT.getText()), equalTo(BecZone.AT));
		assertThat(BecZoneMethods.becZoneToIndex(null), equalTo(BecZone.UNKNOWN));
	}

	@Test
	void test_SiteTool_BECZoneToCode() {
		assertThat(BecZoneMethods.becZoneToCode(BecZone.AT), equalTo(BecZone.AT.getText()));
		assertThat(BecZoneMethods.becZoneToCode(null), equalTo(BecZoneMethods.UNKNOWN_BEC_ZONE_TEXT));
	}

	@Test
	void test_VDYP_MofBiomassCoefficient() {
		assertThat(BecZoneMethods.mofBiomassCoefficient(BecZone.AT.getText(), SP64Name.A.getText()), equalTo(0.75226f));
		assertThat(BecZoneMethods.mofBiomassCoefficient(null, SP64Name.A.getText()), equalTo(-1.0f));
		assertThat(BecZoneMethods.mofBiomassCoefficient(BecZone.AT.getText(), null), equalTo(-1.0f));
	}

	@Test
	void testUtilityMethods() {
		assertThat(BecZone.MS.getIndex(), equalTo(9));
		assertThat(BecZone.IDF.getSpeciesRegion(), equalTo(SpeciesRegion.INTERIOR));
		assertThat(BecZone.CDF.getOffset(), equalTo(BecZone.CDF.getIndex()));
		assertThrows(UnsupportedOperationException.class, () -> BecZone.UNKNOWN.getOffset());
		assertThat(BecZone.UNKNOWN.getText(), equalTo("UNK"));
		assertThat(BecZone.CDF.getText(), equalTo("CDF"));
		assertThat(BecZone.forIndex(9), equalTo(BecZone.MS));
		assertThat(BecZone.forIndex(9), not(equalTo(BecZone.CDF)));

		BecZone.Iterator i = new BecZone.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(BecZone.AT));

	}
}