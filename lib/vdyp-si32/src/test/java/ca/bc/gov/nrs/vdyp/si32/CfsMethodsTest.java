package ca.bc.gov.nrs.vdyp.si32;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDead;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedEcoZone;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedGenera;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsDeadConversionParams;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsDensity;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsMethods;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsSP0Densities;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsSpeciesMethods;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeClass;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeSpecies;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

class CfsMethodsTest {

	@Test
	void test_CFS_CFSTreeClassToString() {
		assertThat(
				CfsMethods.cfsTreeClassToString(CfsTreeClass.LIVE_NO_PATH.getIndex()),
				equalTo(CfsTreeClass.LIVE_NO_PATH.getDescription())
		);
		assertThat(CfsMethods.cfsTreeClassToString(100), equalTo(CfsTreeClass.UNKNOWN.getDescription()));
	}

	@Test
	void test_CFS_CFSGenusToString() {
		assertThat(CfsMethods.cfsGenusToString(CfsTreeGenus.BIRCH), equalTo(CfsTreeGenus.BIRCH.getGenusName()));
		assertThat(CfsMethods.cfsGenusToString(null), equalTo(CfsTreeGenus.UNKNOWN.getGenusName()));
	}

	@Test
	void test_CFS_CFSSP0DensityFunctions() {
		assertThat(CfsMethods.cfsSP0DensityMax(null), equalTo(CfsSP0Densities.DEFAULT_VALUE));
		assertThat(CfsMethods.cfsSP0DensityMax(SP0Name.AC), equalTo(564.00F));
		assertThat(CfsMethods.cfsSP0DensityMean(SP0Name.AC), equalTo(295.00F));
		assertThat(CfsMethods.cfsSP0DensityMin(SP0Name.AC), equalTo(229.00F));
	}

	@Test
	void test_CFS_StringToCfsSpeciesTest() {
		assertThat(CfsMethods.stringToCfsSpecies("Black Spruce"), equalTo(CfsTreeSpecies.SPRUCE_BLACK));
		assertThat(CfsMethods.stringToCfsSpecies("Black spruce"), equalTo(CfsTreeSpecies.SPRUCE_BLACK));
		assertThat(CfsMethods.stringToCfsSpecies("something"), equalTo(CfsTreeSpecies.UNKNOWN));
		assertThat(CfsMethods.stringToCfsSpecies(null), equalTo(CfsTreeSpecies.UNKNOWN));
	}

	@Test
	void test_CFS_CFSSpcsNumToCFSGenus() {
		assertThat(CfsMethods.cfsSpcsNumToCFSGenus(CfsTreeSpecies.SPRUCE_BLACK), equalTo(CfsTreeGenus.SPRUCE));
		assertThat(CfsMethods.cfsSpcsNumToCFSGenus(null), equalTo(CfsTreeGenus.UNKNOWN));
	}

	@Test
	void test_CFS_CFSBiomassConversionCoefficientArrays() {
		assertThat(CfsBiomassConversionCoefficientsDead.get(1, 1).parms()[1], equalTo(0.29900000f));
		assertThat(CfsBiomassConversionCoefficientsForSpecies.get(1, 1).parms()[1], equalTo(0.84904177f));
		assertThat(CfsBiomassConversionCoefficientsForGenus.get(1, 1).parms()[1], equalTo(0.95456459f));
	}

	@Test
	public void testGetSpeciesBySpeciesName() {
		String name = CfsTreeSpecies.ALDER_RED.getName();
		CfsTreeSpecies ts;

		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name);
		assertThat(ts, equalTo(CfsTreeSpecies.ALDER_RED));

		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name.toLowerCase());
		assertThat(ts.getName(), equalTo("Red alder"));

		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name.toUpperCase());
		assertThat(ts.getNumber(), equalTo(1802));

		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name.toUpperCase());
		assertThat(ts.getIndex(), equalTo(114));

		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(null);
		assertThat(ts, equalTo(CfsTreeSpecies.UNKNOWN));
	}

	@Test
	public void testGetGenusBySpecies() {
		CfsTreeGenus g = CfsSpeciesMethods.getGenusBySpecies(CfsTreeSpecies.ALDER_RED);
		assertThat(g, equalTo(CfsTreeGenus.OTHER_BROAD_LEAVES));
		assertThat(CfsSpeciesMethods.getGenusBySpecies(null), equalTo(CfsTreeGenus.UNKNOWN));
	}

	@Test
	public void testGetSpeciesIndexBySpecies() {
		int r = CfsSpeciesMethods.getSpeciesIndexBySpecies(CfsTreeSpecies.ALDER_RED);
		assertThat(r, equalTo(1802));
		assertThat(CfsSpeciesMethods.getSpeciesIndexBySpecies(null), equalTo(-1));
	}

	@Test
	public void testCfsSP0Densities() {
		assertThat(CfsSP0Densities.getValue(SP0Name.B, CfsDensity.MEAN_DENSITY_INDEX), equalTo(379.25F));
		assertThat(
				CfsSP0Densities.getValue(null, CfsDensity.MEAN_DENSITY_INDEX), equalTo(CfsSP0Densities.DEFAULT_VALUE)
		);
		assertThat(CfsSP0Densities.getValue(SP0Name.B, null), equalTo(CfsSP0Densities.DEFAULT_VALUE));
	}

	@Test
	void testCfsBiomassConversionSupportedEcoZoneUtilityMethods() {
		assertThat(CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS.getIndex(), equalTo(1));
		assertThat(
				CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA.getOffset(),
				equalTo(CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA.getIndex())
		);
		assertThrows(
				UnsupportedOperationException.class, () -> CfsBiomassConversionSupportedEcoZone.UNKNOWN.getOffset()
		);
		assertThat(CfsBiomassConversionSupportedEcoZone.UNKNOWN.getText(), equalTo("UNK"));
		assertThat(CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA.getText(), equalTo("BOREAL_CORDILLERA"));

		CfsBiomassConversionSupportedEcoZone.Iterator i = new CfsBiomassConversionSupportedEcoZone.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS));
	}

	@Test
	void testCfsBiomassConversionSupportedGeneraUtilityMethods() {
		assertThat(CfsBiomassConversionSupportedGenera.D.getIndex(), equalTo(3));
		assertThat(
				CfsBiomassConversionSupportedGenera.F.getOffset(),
				equalTo(CfsBiomassConversionSupportedGenera.F.getIndex())
		);
		assertThrows(
				UnsupportedOperationException.class, () -> CfsBiomassConversionSupportedGenera.INVALID.getOffset()
		);
		assertThat(CfsBiomassConversionSupportedGenera.INVALID.getText(), equalTo("INV"));
		assertThat(CfsBiomassConversionSupportedGenera.D.getText(), equalTo("D"));

		CfsBiomassConversionSupportedGenera.Iterator i = new CfsBiomassConversionSupportedGenera.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(CfsBiomassConversionSupportedGenera.AC));
	}

	@Test
	void testCfsBiomassConversionSupportedSpeciesUtilityMethods() {
		assertThat(CfsBiomassConversionSupportedSpecies.BL.getIndex(), equalTo(6));
		assertThat(
				CfsBiomassConversionSupportedSpecies.PY.getOffset(),
				equalTo(CfsBiomassConversionSupportedSpecies.PY.getIndex())
		);
		assertThrows(
				UnsupportedOperationException.class, () -> CfsBiomassConversionSupportedSpecies.UNKNOWN.getOffset()
		);
		assertThrows(UnsupportedOperationException.class, () -> CfsBiomassConversionSupportedSpecies.UNKNOWN.getText());
		assertThat(CfsBiomassConversionSupportedSpecies.PY.getText(), equalTo("PY"));

		CfsBiomassConversionSupportedSpecies.Iterator i = new CfsBiomassConversionSupportedSpecies.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(CfsBiomassConversionSupportedSpecies.AC));
	}

	@Test
	void testCfsDeadConversionParamsUtilityMethods() {
		assertThat(CfsDeadConversionParams.PROP3.getIndex(), equalTo(2));
		assertThat(CfsDeadConversionParams.PROP4.getOffset(), equalTo(CfsDeadConversionParams.PROP4.getIndex()));
		assertThrows(UnsupportedOperationException.class, () -> CfsDeadConversionParams.UNKNOWN.getOffset());
		assertThat(CfsDeadConversionParams.UNKNOWN.getText(), equalTo("UNK"));
		assertThat(CfsDeadConversionParams.PROP1.getText(), equalTo("PROP1"));
		assertThat(CfsDeadConversionParams.PROP1.getCategory(), equalTo("Dead"));
		assertThat(CfsDeadConversionParams.PROP1.getShortName(), equalTo("P1"));
		assertThat(CfsDeadConversionParams.size(), equalTo(9));

		CfsDeadConversionParams.Iterator i = new CfsDeadConversionParams.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(CfsDeadConversionParams.PROP1));
	}

	@Test
	void testCfsTreeClassUtilityMethods() {
		assertThat(CfsTreeClass.DEAD_POTENTIAL.getIndex(), equalTo(3));
		assertThat(CfsTreeClass.DEAD_USELESS.getOffset(), equalTo(CfsTreeClass.DEAD_USELESS.getIndex()));
		assertThrows(UnsupportedOperationException.class, () -> CfsTreeClass.UNKNOWN.getOffset());
		assertThat(CfsTreeClass.UNKNOWN.getText(), equalTo("UNK"));
		assertThat(CfsTreeClass.MISSING.getText(), equalTo("MISSING"));
		assertThat(CfsTreeClass.MISSING.getDescription(), equalTo("Missing"));
		assertThat(CfsTreeClass.size(), equalTo(7));

		CfsTreeClass.Iterator i = new CfsTreeClass.Iterator();
		assertTrue(i.hasNext());
		assertThat(i.next(), equalTo(CfsTreeClass.MISSING));

		CfsBiomassConversionSupportedSpecies species = SiteTool.lcl_MoFSP64ToCFSSpecies("BL");
		assertThat(species.getIndex(), is(6));
		assertThat(species.getText(), is("BL"));
	}
}
