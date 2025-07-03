package ca.bc.gov.nrs.vdyp.si32;

import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType.SI_AT_BREAST;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType.SI_AT_TOTAL;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation.SI_ACT_THROWER;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation.SI_AT_CHEN;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation.SI_AT_GOUDIE;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation.SI_FDI_THROWER;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation.SI_SW_HU_GARCIA;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType.SI_EST_DIRECT;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType.SI_EST_ITERATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.common_calculators.SiteIndexNames;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.LessThan13Exception;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDetails;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedEcoZone;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedGenera;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsDeadConversionParams;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsLiveConversionParams;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeSpecies;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;
import ca.bc.gov.nrs.vdyp.si32.site.NameFormat;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP64Name;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SpeciesTable;

class SiteToolTest {

	private static SpeciesTable speciesTable;

	@BeforeAll
	static void atStart() {
		speciesTable = new SpeciesTable();
	}

	@Test
	void test_lcl_MoFSP64ToCFSSpecies() {

		SP64Name.Iterator i = new SP64Name.Iterator();

		int namesSeen = 0;
		while (i.hasNext()) {
			namesSeen += 1;
			SP64Name sp64Name = i.next();

			CfsBiomassConversionSupportedSpecies result = SiteTool.lcl_MoFSP64ToCFSSpecies(sp64Name.getText());
			assertThat(result, notNullValue(CfsBiomassConversionSupportedSpecies.class));

		}

		assertThat(namesSeen, is(SP64Name.size()));
	}

	@Test
	void test_lcl_InternalSpeciesIndexToString() {
		assertThat(SiteTool.lcl_InternalSpeciesIndexToString(null), is("??"));
		assertThat(SiteTool.lcl_InternalSpeciesIndexToString(CfsBiomassConversionSupportedSpecies.UNKNOWN), is("??"));
		assertThat(
				SiteTool.lcl_InternalSpeciesIndexToString(CfsBiomassConversionSupportedSpecies.AC),
				is(CfsBiomassConversionSupportedSpecies.AC.getText())
		);
	}

	@Test
	void test_lcl_InternalGenusIndexToString() {
		assertThat(SiteTool.lcl_InternalGenusIndexToString(null), is("genusInt_INVALID"));
		assertThat(
				SiteTool.lcl_InternalGenusIndexToString(CfsBiomassConversionSupportedGenera.INVALID),
				is("genusInt_INVALID")
		);
		assertThat(
				SiteTool.lcl_InternalGenusIndexToString(CfsBiomassConversionSupportedGenera.AC),
				is(CfsBiomassConversionSupportedSpecies.AC.getText())
		);
	}

	@Test
	void test_lcl_LiveConversionParamToString() {
		assertThat(SiteTool.lcl_LiveConversionParamToString(null, null), is("cfsLiveParam_UNKNOWN"));
		assertThat(
				SiteTool.lcl_LiveConversionParamToString(CfsLiveConversionParams.A_NONMERCH, null), is("A_NONMERCH")
		);
		assertThat(SiteTool.lcl_LiveConversionParamToString(null, NameFormat.ENUM_STR), is("cfsLiveParam_UNKNOWN"));
		assertThat(SiteTool.lcl_LiveConversionParamToString(null, NameFormat.CAT_NAME), is("??"));
		assertThat(
				SiteTool.lcl_LiveConversionParamToString(CfsLiveConversionParams.A_NONMERCH, NameFormat.NAME_ONLY),
				is("A")
		);
		assertThat(
				SiteTool.lcl_LiveConversionParamToString(CfsLiveConversionParams.A_NONMERCH, NameFormat.CAT_ONLY),
				is("Non-Merch")
		);
		assertThat(
				SiteTool.lcl_LiveConversionParamToString(CfsLiveConversionParams.A_NONMERCH, NameFormat.CAT_NAME),
				is("Non-Merch A")
		);
	}

	@Test
	void test_lcl_DeadConversionParamToString() {
		assertThat(SiteTool.lcl_DeadConversionParamToString(null, null), is("cfsDeadParam_UNKNOWN"));
		assertThat(SiteTool.lcl_DeadConversionParamToString(CfsDeadConversionParams.PROP1, null), is("PROP1"));
		assertThat(SiteTool.lcl_DeadConversionParamToString(null, NameFormat.ENUM_STR), is("cfsDeadParam_UNKNOWN"));
		assertThat(SiteTool.lcl_DeadConversionParamToString(null, NameFormat.CAT_NAME), is("??"));
		assertThat(
				SiteTool.lcl_DeadConversionParamToString(CfsDeadConversionParams.PROP1, NameFormat.NAME_ONLY), is("P1")
		);
		assertThat(
				SiteTool.lcl_DeadConversionParamToString(CfsDeadConversionParams.PROP1, NameFormat.CAT_ONLY), is("Dead")
		);
		assertThat(
				SiteTool.lcl_DeadConversionParamToString(CfsDeadConversionParams.PROP1, NameFormat.CAT_NAME),
				is("Dead P1")
		);
	}

	@Test
	void test_cfsSpcsToCfsSpcsNum() {
		assertThat(SiteTool.cfsSpcsToCfsSpcsNum(null), is(CfsTreeSpecies.UNKNOWN.getNumber()));
		assertThat(SiteTool.cfsSpcsToCfsSpcsNum(CfsTreeSpecies.ALDER), is(CfsTreeSpecies.ALDER.getNumber()));
	}

	@Test
	void test_getIsDeciduous() {
		assertThat(SiteTool.getIsDeciduous(SP64Name.A.getIndex()), is(true));
		assertThat(SiteTool.getIsDeciduous(SP64Name.BN.getIndex()), is(false));
		assertThat(SiteTool.getIsDeciduous(Integer.MAX_VALUE), is(false));
	}

	@Test
	void test_getIsSoftwood() {
		assertThat(SiteTool.getIsSoftwood("A"), is(false));
		assertThat(SiteTool.getIsSoftwood("BN"), is(true));
		assertThat(SiteTool.getIsSoftwood(null), is(false));
	}

	@Test
	void test_getIsPine() {
		assertThat(SiteTool.getIsPine("A"), is(false));
		assertThat(SiteTool.getIsPine("PL"), is(true));
		assertThat(SiteTool.getIsPine(null), is(false));
	}

	@Test
	void test_getSpeciesCFSSpcs() {
		assertThat(SiteTool.getSpeciesCFSSpcs(SP64Name.A.getText()), is(CfsTreeSpecies.UNKNOWN));
		assertThat(SiteTool.getSpeciesCFSSpcs(SP64Name.B.getText()), is(CfsTreeSpecies.FIR));
		assertThat(SiteTool.getSpeciesCFSSpcs(null), is(CfsTreeSpecies.UNKNOWN));
	}

	@Test
	void test_getSpeciesCFSSpcsNum() {
		assertThat(SiteTool.getSpeciesCFSSpcsNum(SP64Name.A.getText()), is(CfsTreeSpecies.UNKNOWN.getNumber()));
		assertThat(SiteTool.getSpeciesCFSSpcsNum(SP64Name.B.getText()), is(CfsTreeSpecies.FIR.getNumber()));
		assertThat(SiteTool.getSpeciesCFSSpcsNum(null), is(-1));
	}

	@Test
	void test_htAgeToSI() throws CommonCalculatorException {
		assertThrows(
				LessThan13Exception.class,
				() -> SiteTool.heightAndAgeToSiteIndex(null, 0, SI_AT_BREAST, 1.0, SI_EST_ITERATE)
		);
		assertThrows(
				NoAnswerException.class,
				() -> SiteTool.heightAndAgeToSiteIndex(null, 0, SI_AT_TOTAL, 0.0, SI_EST_ITERATE)
		);
		assertThrows(
				NoAnswerException.class,
				() -> SiteTool.heightAndAgeToSiteIndex(null, 0, SI_AT_TOTAL, 23.0, SI_EST_ITERATE)
		);
		assertThat(SiteTool.heightAndAgeToSiteIndex(SI_AT_GOUDIE, 10.0, SI_AT_BREAST, 23.0, SI_EST_DIRECT), is(34.30));
		assertThat(SiteTool.heightAndAgeToSiteIndex(SI_AT_GOUDIE, 10.0, SI_AT_BREAST, 23.0, SI_EST_ITERATE), is(69.45));
		assertThat(
				SiteTool.heightAndAgeToSiteIndex(SI_FDI_THROWER, 10.0, SI_AT_BREAST, 23.0, SI_EST_DIRECT), is(84.31)
		);
		assertThat(
				SiteTool.heightAndAgeToSiteIndex(SI_FDI_THROWER, 10.0, SI_AT_BREAST, 23.0, SI_EST_ITERATE), is(87.60)
		);
	}

	@Test
	void test_htSIToAge() throws CommonCalculatorException {
		assertThrows(
				LessThan13Exception.class, () -> SiteTool.heightAndSiteIndexToAge(null, 1.0, SI_AT_BREAST, 1.0, 0)
		);
		assertThrows(
				LessThan13Exception.class, () -> SiteTool.heightAndSiteIndexToAge(null, 10.0, SI_AT_BREAST, 1.1, 0.0)
		);
		assertThat(SiteTool.heightAndSiteIndexToAge(null, 0.0, SI_AT_TOTAL, 1.0, 0), is(0.0));
		assertThat(round(SiteTool.heightAndSiteIndexToAge(SI_FDI_THROWER, 10.0, SI_AT_BREAST, 47.0, 5.0), 2), is(8.54));
	}

	@Test
	void test_ageSIToHt() throws CommonCalculatorException {
		assertThat(round(SiteTool.ageAndSiteIndexToHeight(SI_FDI_THROWER, 10.0, SI_AT_TOTAL, 30.0, 5.0), 2), is(4.10));
	}

	@Test
	void test_yearsToBreastHeight() throws CommonCalculatorException {
		assertThat(SiteTool.yearsToBreastHeight(SI_FDI_THROWER, 30.0), is(7.3));
	}

	@Test
	void test_getSICurveName() {
		assertThat(SiteTool.getSICurveName(null), is(SiteTool.UNKNOWN_CURVE_RESULT));
		assertThat(SiteTool.getSICurveName(SI_ACT_THROWER), is(SiteIndexNames.siCurveName[SI_ACT_THROWER.n()]));
	}

	@Test
	void test_getNumSpecies() {
		assertThat(SiteTool.getNumSpecies(), is(speciesTable.getNSpecies()));
	}

	@Test
	void test_getSpeciesShortName() {
		assertThat(SiteTool.getSpeciesShortName(Integer.MAX_VALUE), is(SP64Name.UNKNOWN.getText()));
		assertThat(SiteTool.getSpeciesShortName(SP64Name.A.getIndex()), is("A"));
	}

	@Test
	void test_getSpeciesIndex() {
		assertThat(SiteTool.getSpeciesIndex("ZZZZ"), is(SpeciesTable.UNKNOWN_ENTRY_INDEX));
		assertThat(SiteTool.getSpeciesIndex("A"), is(SP64Name.A.getIndex()));
	}

	@Test
	void test_getSpeciesFullName() {
		assertThat(SiteTool.getSpeciesFullName("ZZZZ"), is(SpeciesTable.DefaultEntry.fullName()));
		assertThat(SiteTool.getSpeciesFullName("A"), is(speciesTable.getByCode("A").details().fullName()));
	}

	@Test
	void test_getSpeciesLatinName() {
		assertThat(SiteTool.getSpeciesLatinName("ZZZZ"), is(SpeciesTable.DefaultEntry.latinName()));
		assertThat(SiteTool.getSpeciesLatinName("A"), is(speciesTable.getByCode("A").details().latinName()));
	}

	@Test
	void test_getSpeciesGenusCode() {
		assertThat(SiteTool.getSpeciesGenusCode("ZZZZ"), is(SpeciesTable.DefaultEntry.genusName()));
		assertThat(SiteTool.getSpeciesGenusCode("A"), is(speciesTable.getByCode("A").details().genusName()));
	}

	@Test
	void test_getSpeciesSINDEXCode() {
		assertThat(SiteTool.getSpeciesSINDEXCode("ZZZZ", true), is(""));
		assertThat(SiteTool.getSpeciesSINDEXCode("A", false), is("At"));
	}

	@Test
	void test_getSpeciesVDYP7Code() {
		assertThat(SiteTool.getSpeciesVDYP7Code("ZZZZ"), is(""));
		assertThat(SiteTool.getSpeciesVDYP7Code("A"), is("AC"));
	}

	@Test
	void test_setSICurve() {
		SiteIndexEquation oldCurve = SiteTool.getSICurve("ABAL", true);
		SiteIndexEquation newCurve = oldCurve == SiteIndexEquation.SI_AT_CHEN ? SiteIndexEquation.SI_AT_NIGH
				: SiteIndexEquation.SI_AT_CHEN;
		assertThat(SiteTool.setSICurve("ABAL", true, newCurve), is(oldCurve));
		assertThat(SiteTool.getSICurve("ABAL", true), is(newCurve));
	}

	@Test
	void test_getSICurve() {
		assertThat(SiteTool.getSICurve("ABAL", true), is(SI_AT_CHEN));
	}

	@Test
	void test_getSiteCurveSINDEXSpecies() {
		assertThat(SiteTool.getSiteCurveSINDEXSpecies(null), is(""));
		assertThat(SiteTool.getSiteCurveSINDEXSpecies(SI_SW_HU_GARCIA), is("Sw"));
	}

	@Test
	void test_getSpeciesDefaultCrownClosure() {
		assertThat(SiteTool.getSpeciesDefaultCrownClosure("ZZZZ", true), is(-1.0f));
		assertThat(
				SiteTool.getSpeciesDefaultCrownClosure("ABAL", true),
				is(speciesTable.getByCode("ABAL").details().defaultCrownClosure()[SpeciesRegion.COAST.ordinal()])
		);
	}

	@Test
	void test_fillInAgeTriplet() {
		Reference<Double> rTotalAge = new Reference<>();
		Reference<Double> rBreastHeightAge = new Reference<>();
		Reference<Double> rYTBH = new Reference<>();

		rTotalAge.set(10.0);
		rBreastHeightAge.set(5.0);
		rYTBH.set(-9.0);
		SiteTool.fillInAgeTriplet(rTotalAge, rBreastHeightAge, rYTBH);
		assertThat(rYTBH.get(), is(5.5));

		rTotalAge.set(-9.0);
		rBreastHeightAge.set(5.0);
		rYTBH.set(6.0);
		SiteTool.fillInAgeTriplet(rTotalAge, rBreastHeightAge, rYTBH);
		assertThat(rTotalAge.get(), is(10.5));

		rTotalAge.set(10.0);
		rBreastHeightAge.set(-9.0);
		rYTBH.set(7.0);
		SiteTool.fillInAgeTriplet(rTotalAge, rBreastHeightAge, rYTBH);
		assertThat(rBreastHeightAge.get(), is(3.5));
	}

	private double round(double d, int precision) {
		assert precision >= 0;
		double factor = Math.pow(10.0, precision);
		return Math.round(d * factor) / factor;
	}

	@Test
	void testBadValuesLiveCoefficients() {
		CfsBiomassConversionCoefficientsDetails answer = SiteTool.lookupLiveCfsConversionParams(null, null);
		assertThat(answer.containsData(), is(false));
		answer = SiteTool.lookupLiveCfsConversionParams(CfsBiomassConversionSupportedEcoZone.UNKNOWN, null);
		assertThat(answer.containsData(), is(false));
		answer = SiteTool.lookupLiveCfsConversionParams(CfsBiomassConversionSupportedEcoZone.UNKNOWN, "BAD");
		assertThat(answer.containsData(), is(false));
	}

	static Stream<Arguments> cfsLiveCoefficientByEcoZoneAndSpecies() {
		return Stream.of(
				Arguments.of(
						// Known Species for eco zone
						CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS, "FDC",
						new float[] { 0.61495547f, 0.94445681f, 15.60925025f, -0.98799751f, 0.99656179f, 3.65329157f,
								0.54870672f, -0.83582989f, 0.99627406f, 1.02299762f, -1.40325200f, 0.00004760f,
								-0.06294830f, -0.38615170f, 0.00011320f, -0.19501550f, -0.11680800f, 0.00008170f,
								-0.33527570f, 0.59098570f, 1855.68858950f, 0.45328151f, 0.69287488f, 0.09997755f,
								0.11581675f, 0.22025293f, 0.13382703f, 0.22648801f, 0.05748134f }
				),
				Arguments.of(
						// known genus for eco zone
						CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS, "BA",
						new float[] { 0.62123600f, 0.92745332f, 19.30602940f, -0.94332226f, 0.92391419f, 5.50265387f,
								0.14752324f, -0.49851762f, 0.98816410f, 1.06044551f, -0.40594000f, 0.00205040f,
								-0.35415510f, 0.40718550f, 0.00015900f, -0.39484070f, 0.48957120f, -0.00012160f,
								-0.48394590f, 39.64644616f, 266.25940420f, 0.56054960f, 0.69704505f, 0.10552136f,
								0.11022134f, 0.18914164f, 0.11958752f, 0.14478740f, 0.07314609f }
				),
				Arguments.of(
						// unknown species and genus for eco zone softwood
						CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS, "JR",
						new float[] { 1.10569830f, 0.81758788f, 99.99999999f, -1.38307410f, 1.06044355f, 9.09948714f,
								0.95217531f, -0.01430719f, 0.11594155f, 1.02036282f, -1.53866200f, -0.00020320f,
								-0.08322460f, -2.58313000f, -0.00221930f, 0.10208420f, -1.60939000f, -0.00005150f,
								-0.14558330f, 0.41433015f, 753.09896287f, 0.69804346f, 0.82829281f, 0.13018607f,
								0.08786734f, 0.06259323f, 0.02314516f, 0.10917725f, 0.06069470f }
				),
				Arguments.of(
						// unknown species and genus for eco zone hardwood
						CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS, "DM",
						new float[] { 1.52128657f, 0.78020277f, 60.22191801f, -1.08440396f, 0.82555881f, 6.83453138f,
								0.95217531f, -0.01430719f, 0.11594155f, 1.02036282f, -1.58397100f, -0.00021960f,
								0.00547460f, -1.78569000f, -0.00055610f, -0.07375640f, -2.02195000f, -0.00103160f,
								-0.23426020f, 0.24238936f, 997.95058130f, 0.69195659f, 0.80753904f, 0.14324682f,
								0.13820026f, 0.10266800f, 0.04669479f, 0.06212858f, 0.00756591f }
				)
		);
	}

	@ParameterizedTest
	@MethodSource("cfsLiveCoefficientByEcoZoneAndSpecies")
	void testLiveCoefficients(CfsBiomassConversionSupportedEcoZone zone, String mofSp64, float[] expected) {
		CfsBiomassConversionCoefficientsDetails answer = SiteTool.lookupLiveCfsConversionParams(zone, mofSp64);
		assertThat(answer.containsData(), is(true));
		assertArrayEquals(expected, answer.parms());
	}

	@Test
	void testBadValuesDeadCoefficients() {
		CfsBiomassConversionCoefficientsDetails answer = SiteTool.lookupDeadCfsConversionParams(null, null);
		assertThat(answer.containsData(), is(false));
		answer = SiteTool.lookupDeadCfsConversionParams(CfsBiomassConversionSupportedEcoZone.UNKNOWN, null);
		assertThat(answer.containsData(), is(false));
		answer = SiteTool.lookupDeadCfsConversionParams(CfsBiomassConversionSupportedEcoZone.UNKNOWN, "BAD");
		assertThat(answer.containsData(), is(false));
	}

	static Stream<Arguments> cfsDeadCoefficientByEcoZoneAndSpecies() {
		return Stream.of(
				Arguments.of(
						// Known Genus
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "M",
						new float[] { 0.18500000f, 0.07400000f, 0.11500000f, 0.18600000f, 0.09600000f, 348.00000000f,
								520.50000000f, 699.00000000f, 1022.50000000f }
				)
		);
	}

	@ParameterizedTest
	@MethodSource("cfsDeadCoefficientByEcoZoneAndSpecies")
	void testDeadCoefficients(CfsBiomassConversionSupportedEcoZone zone, String mofSp64, float[] expected) {
		CfsBiomassConversionCoefficientsDetails answer = SiteTool.lookupDeadCfsConversionParams(zone, mofSp64);
		assertThat(answer.containsData(), is(true));
		assertArrayEquals(expected, answer.parms());
	}

}
