package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.OVER225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U125TO175;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U175TO225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U75TO125;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.BASAL_AREA;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.LOREY_HEIGHT;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.QUAD_MEAN_DIAMETER;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.WHOLE_STEM_VOLUME;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.WHOLE_STEM_VOL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.SpeciesErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.CommonData;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

class PreliminaryForwardProcessingEngineStepsTest extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PreliminaryForwardProcessingEngineStepsTest.class);

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		assertThat(fpe.fps.fcm.getBecLookup(), notNullValue());
		assertThat(fpe.fps.fcm.getGenusDefinitionMap(), notNullValue());
		assertThat(fpe.fps.fcm.getSiteCurveMap(), notNullValue());

		int nPolygonsProcessed = 0;
		while (true) {
			var polygon = forwardDataStreamReader.readNextPolygon();

			if (polygon.isPresent()) {
				fpe.processPolygon(polygon.get());
				nPolygonsProcessed += 1;
			} else {
				break;
			}
		}

		assertEquals(10, nPolygonsProcessed);
	}

	@Test
	/** CALCULATE_COVERAGES */
	void testFindPrimarySpecies() throws IOException, ResourceParseException, ProcessingException {

		 // This tests ExecutionStep.CALCULATE_COVERAGES, but that code is stand-alone and can be
		 // tested independently.

		var polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		{
			ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
			fpe.fps.setPolygonLayer(polygon, LayerType.PRIMARY);

			LayerProcessingState lps = fpe.fps.getLayerProcessingState();

			ForwardProcessingEngine.calculateCoverages(lps);
			fpe.determinePolygonRankings(CommonData.PRIMARY_SPECIES_TO_COMBINE);

			assertThat(lps.getPrimarySpeciesIndex(), is(3));
			assertThat(lps.getSecondarySpeciesIndex(), is(4));
			assertThat(lps.getInventoryTypeGroup(), is(37));
			assertThat(lps.getPrimarySpeciesGroupNumber(), is(1));
			assertThat(lps.getPrimarySpeciesStratumNumber(), is(1));
		}
		{
			ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
			fpe.fps.setPolygonLayer(polygon, LayerType.PRIMARY);
			LayerProcessingState lps = fpe.fps.getLayerProcessingState();

			var speciesToCombine = Arrays
					.asList(Arrays.asList(lps.getBank().speciesNames[3], lps.getBank().speciesNames[4]));

			ForwardProcessingEngine.calculateCoverages(lps);
			fpe.determinePolygonRankings(speciesToCombine);

			// The test-specific speciesToCombine will combine 3 & 4 into 3 (leaving 4 at 0.0), promoting 2 to
			// secondary.
			assertThat(lps.getPrimarySpeciesIndex(), is(3));
			assertThat(lps.getSecondarySpeciesIndex(), is(2));
			assertThat(lps.getInventoryTypeGroup(), is(37));
			assertThat(lps.getPrimarySpeciesGroupNumber(), is(1));
			assertThat(lps.getPrimarySpeciesStratumNumber(), is(1));
		}
	}

	@Test
	/** DETERMINE_POLYGON_RANKINGS */
	void testGroupAndStratumNumberSpecialCases() throws IOException, ResourceParseException, ProcessingException {

		// We want the
		// "equationModifierGroup.isPresent()"
		// and the
		// " Region.INTERIOR.equals(lps.wallet.getBecZone().getRegion()) && exceptedSpeciesIndicies.contains(primarySpeciesIndex)"
		// cases in determinePolygonRankings.

		buildPolygonParserForStream(
				"testPolygon.dat", //
				"01002 S000001 00     1970 IDF  A    99 37  1  1", //
				""
		);

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.DETERMINE_POLYGON_RANKINGS);

		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesIndex(), is(1));
		assertThrows(IllegalStateException.class, () -> fpe.fps.getLayerProcessingState().getSecondarySpeciesIndex());
		assertThat(fpe.fps.getLayerProcessingState().getInventoryTypeGroup(), is(10));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesGroupNumber(), is(35));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesStratumNumber(), is(24));
	}

	@Test
	/** CALCULATE_MISSING_SITE_CURVES */
	void testCalculateMissingSiteCurves() throws IOException, ResourceParseException, ProcessingException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  62.9   9.1 0 -9", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_MISSING_SITE_CURVES);

		// Cannot check 0 since determinePolygonRankings has not been executed.
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(1), is(12));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(2), is(2));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(3), is(42));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(4), is(10));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(5), is(10));
	}

	@Test
	/** CALCULATE_MISSING_SITE_CURVES alternate */
	void testCalculateMissingSiteCurvesNoSiteCurveData()
			throws IOException, ResourceParseException, ProcessingException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C       0.0     0.0     0.0     0.0 13.40 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  62.9   9.1 0 -9", //
				"01002 S000001 00     1970 P  8 H       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970 P 15 S       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_MISSING_SITE_CURVES);

		// Cannot check 0 since determinePolygonRankings has not been executed.
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(1), is(118));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(2), is(122));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(3), is(13));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(4), is(99));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(5), is(59));
	}

	@Test
	/** ESTIMATE_MISSING_SITE_INDICES, step 1 */
	void testEstimateMissingSiteIndicesStep1() throws ProcessingException, IOException, ResourceParseException,
			CurveErrorException, SpeciesErrorException, NoAnswerException {

		buildPolygonParserForStream("testPolygon.dat", "01002 S000001 00     1970 CWH  A    99 37  1  1");

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 13.40 -9.00  -9.0  -9.0  -9.0 0 11", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 99", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 28.90 265.0 253.9  11.1 1 99", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_SITE_INDICES);

		// Despite 13.40 being in the data stream, the change (2024/8/29) to ignore site information
		// for all species of the layer except the primary means that method (1) will never be
		// successful, since none of the non-primary species will have a site index value.

//		var sourceSiteCurve = SiteIndexEquation.SI_CWC_KURUCZ;
//		var sourceSiteIndex = 13.4;
//		var targetSiteCurve = SiteIndexEquation.SI_HWC_WILEYAC;
//		double expectedValue = SiteTool
//				.convertSiteIndexBetweenCurves(sourceSiteCurve, sourceSiteIndex, targetSiteCurve);

		assertThat(fpe.fps.getLayerProcessingState().getBank().siteIndices[4], is(VdypEntity.MISSING_FLOAT_VALUE));
	}

	@Test
	/** ESTIMATE_MISSING_SITE_INDICES, step 2*/
	void testEstimateMissingSiteIndicesStep2() throws ProcessingException, IOException, ResourceParseException,
			CurveErrorException, SpeciesErrorException, NoAnswerException {

		buildPolygonParserForStream("testPolygon.dat", "01002 S000001 00     1970 CWH  A    99 37  1  1");

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 13.40 28.90 265.0 253.9  11.1 1 12", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_SITE_INDICES);

		var sourceSiteCurve = SiteIndexEquation.SI_CWC_BARKER;
		var sourceSiteIndex = 13.4;
		var targetSiteCurve = SiteIndexEquation.SI_CWC_NIGH;
		double expectedValue = SiteTool
				.convertSiteIndexBetweenCurves(sourceSiteCurve, sourceSiteIndex, targetSiteCurve);

		assertThat(fpe.fps.getLayerProcessingState().getBank().siteIndices[2], is((float) expectedValue));
	}

	@Test
	/** ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES */
	void testEstimateMissingYearsToBreastHeightValues()
			throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 35.00 35.30  55.0  54.0   1.0 1 13", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(
				polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getBank().yearsToBreastHeight,
				is(new float[] { 0.0f, 4.7f, 4.6f, 1.0f, 5.0f, 5.0f })
		);
	}

	@Test
	/** CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX */
	void testCalculateDominantHeightAgeSiteIndex() throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				// Polygon Year L? x G S1 % S2 % S3 % S4 % Sidx domH age agebh y2bh P? SC
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  15.0  11.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 34.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 1 13", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		// Since the change to ignore site information for all but non-primary species, there is
		// no way to successfully estimate age for a primary species from the non-primary species.
		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		assertThrows(
				ProcessingException.class,
				() -> fpe.processPolygon(
						polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX
				)
		);
	}

	@Test
	/** CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX */
	void testCalculateDominantHeightAgeSiteIndexNoSecondary()
			throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 34.00 -9.00  22.0  -9.0  -9.0 1 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStream(
				"testUtilizations.dat", //
				"01002 S000001 00     1970 P  0    -1  0.01513     5.24   7.0166   0.0630   0.0000   0.0000   0.0000   0.0000   6.1", //
				"01002 S000001 00     1970 P  0     0 44.93259   595.32  30.9724 620.9775 592.2023 580.1681 577.6229 549.0159  31.0", //
				"01002 S000001 00     1970 P  0     1  0.53100    64.82  -9.0000   2.5979   0.3834   0.3794   0.3788   0.3623  10.2", //
				"01002 S000001 00     1970 P  0     2  1.27855    71.93  -9.0000   9.1057   6.9245   6.8469   6.8324   6.5384  15.0", //
				"01002 S000001 00     1970 P  0     3  2.33020    73.60  -9.0000  22.4019  20.1244  19.8884  19.8375  18.9555  20.1", //
				"01002 S000001 00     1970 P  0     4 40.79285   384.98  -9.0000 586.8720 564.7699 553.0534 550.5741 523.1597  36.7", //
				"01002 S000001 00     1970 P  4 C  -1  0.01243     4.40   6.4602   0.0507   0.0000   0.0000   0.0000   0.0000   6.0", //
				"01002 S000001 00     1970 P  4 C   0  5.04597    83.46  22.9584  43.4686  39.4400  36.2634  35.2930  32.9144  27.7", //
				"01002 S000001 00     1970 P  4 C   1  0.12822    16.12  -9.0000   0.6027   0.1116   0.1094   0.1090   0.1035  10.1", //
				"01002 S000001 00     1970 P  4 C   2  0.31003    17.87  -9.0000   1.9237   1.4103   1.3710   1.3628   1.2915  14.9", //
				"01002 S000001 00     1970 P  4 C   3  0.51339    16.55  -9.0000   3.8230   3.3162   3.2127   3.1859   3.0076  19.9", //
				"01002 S000001 00     1970 P  4 C   4  4.09434    32.92  -9.0000  37.1192  34.6019  31.5703  30.6352  28.5119  39.8", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX);

		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesDominantHeight(), is(22.950302f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesSiteIndex(), is(34.0f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesTotalAge(), is(22.0f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesAgeAtBreastHeight(), is(Float.NaN));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesAgeToBreastHeight(), is(4.7f));
	}
	
	@Test
	/** SET_COMPATIBILITY_VARIABLES */
	void testSetCompatibilityVariables() throws ResourceParseException, IOException, ProcessingException {

		var reader = new ForwardDataStreamReader(controlMap);
		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.SET_COMPATIBILITY_VARIABLES);

		// These values have been verified against the FORTRAN implementation, allowing for minor
		// platform-specific differences.

		assertThat(
				fpe.fps.getLayerProcessingState().getVolumeEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 12, 20, 25, 37, 66 })
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getDecayEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 7, 14, 19, 31, 54 })
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getBreakageEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 5, 6, 12, 17, 28 })
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0063979626f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.00016450882f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.00024962425f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.00011026859f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.000006198883f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.000024557114f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0062299743f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0010375977f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0001244545f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0000038146973f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.00013566017f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.00033128262f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.00021290779f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.0000059604645f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.000882864f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.0002478361f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(0.0008614063f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
				is(-0.0000052452087f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-0.004629612f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(1.3446808E-4f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.01768279f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.0010006428f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(2.2292137E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-1.9311905E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.010708809f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(7.638931E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-2.4795532E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.011518478f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-0.0010123253f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-9.3603134E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-5.4836273E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(0.010197163f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-0.0014338493f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
				is(-2.9087067E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.035768032f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0016698837f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.16244507f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0045113564f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0030164719f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(3.528595E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(4.1484833E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.13775301f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.005630493f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0028266907f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(3.7765503E-4f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState()
						.getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(5.378723E-4f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(3.684759E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(0.0027964115f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(1, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-9.393692E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-3.0636787E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(4.1246414E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-2.527237E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(2, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(9.536743E-7f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(0.001360178f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(4.8589706E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(4.4107437E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(3, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-1.1920929E-6f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-5.042553E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(5.4240227E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(6.2704086E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(4, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-2.169609E-5f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(1.3077259E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-1.21593475E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(2.3770332E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVVolume(5, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY),
				is(-1.2636185E-5f)
		);

		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(1, OVER225, LayerType.PRIMARY), is(-2.065301E-5f));
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(1, U125TO175, LayerType.PRIMARY), is(1.0924414E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(1, U175TO225, LayerType.PRIMARY), is(1.0116026E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(1, U75TO125, LayerType.PRIMARY), is(-3.9674342E-7f)
		);

		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(2, OVER225, LayerType.PRIMARY), is(-1.1444092E-5f));
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(2, U125TO175, LayerType.PRIMARY), is(5.00679E-6f));
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(2, U175TO225, LayerType.PRIMARY), is(9.596348E-6f));
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(2, U75TO125, LayerType.PRIMARY), is(6.660819E-6f));

		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(3, OVER225, LayerType.PRIMARY), is(7.6293945E-6f));
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(3, U125TO175, LayerType.PRIMARY), is(-3.7066638E-6f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(3, U175TO225, LayerType.PRIMARY), is(-1.0579824E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(3, U75TO125, LayerType.PRIMARY), is(-2.4344772E-6f)
		);

		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(4, OVER225, LayerType.PRIMARY), is(3.194809E-5f));
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(4, U125TO175, LayerType.PRIMARY), is(-1.4662743E-5f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(4, U175TO225, LayerType.PRIMARY), is(-9.059906E-6f)
		);
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(4, U75TO125, LayerType.PRIMARY), is(-8.106232E-6f));

		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(5, OVER225, LayerType.PRIMARY), is(-4.673004E-5f));
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(5, U125TO175, LayerType.PRIMARY), is(8.821487E-6f));
		assertThat(
				fpe.fps.getLayerProcessingState().getCVBasalArea(5, U175TO225, LayerType.PRIMARY), is(2.8401613E-5f)
		);
		assertThat(fpe.fps.getLayerProcessingState().getCVBasalArea(5, U75TO125, LayerType.PRIMARY), is(-4.954636E-7f));

		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(1, OVER225, LayerType.PRIMARY),
				is(-0.0054130554f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(1, U125TO175, LayerType.PRIMARY),
				is(-0.011018753f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(1, U175TO225, LayerType.PRIMARY),
				is(-0.0404377f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(1, U75TO125, LayerType.PRIMARY),
				is(-0.018251419f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(2, OVER225, LayerType.PRIMARY),
				is(7.209778E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(2, U125TO175, LayerType.PRIMARY),
				is(-2.0122528E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(2, U175TO225, LayerType.PRIMARY),
				is(6.0272217E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(2, U75TO125, LayerType.PRIMARY),
				is(-1.2207031E-4f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(3, OVER225, LayerType.PRIMARY),
				is(-1.7929077E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(3, U125TO175, LayerType.PRIMARY),
				is(-0.008193016f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(3, U175TO225, LayerType.PRIMARY),
				is(-0.0019302368f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(3, U75TO125, LayerType.PRIMARY),
				is(-0.008533478f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(4, OVER225, LayerType.PRIMARY),
				is(-0.0016155243f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(4, U125TO175, LayerType.PRIMARY),
				is(-7.4863434E-4f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(4, U175TO225, LayerType.PRIMARY),
				is(-0.0012569427f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(4, U75TO125, LayerType.PRIMARY),
				is(1.9168854E-4f)
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(5, OVER225, LayerType.PRIMARY),
				is(-0.002658844f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(5, U125TO175, LayerType.PRIMARY),
				is(0.0011692047f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(5, U175TO225, LayerType.PRIMARY),
				is(-0.005224228f)
		);
		assertThat(
				fpe.fps.getLayerProcessingState().getCVQuadraticMeanDiameter(5, U75TO125, LayerType.PRIMARY),
				is(-6.6947937E-4f)
		);

		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(1, BASAL_AREA), is(-2.1831444E-7f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(1, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(1, LOREY_HEIGHT), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(1, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(2, BASAL_AREA), is(-4.4927E-5f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(2, QUAD_MEAN_DIAMETER), is(0.0023670197f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(2, LOREY_HEIGHT), is(3.576278E-7f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(2, WHOLE_STEM_VOLUME), is(0.0010275329f));

		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(3, BASAL_AREA), is(4.946138E-6f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(3, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(3, LOREY_HEIGHT), is(-4.827988E-6f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(3, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(4, BASAL_AREA), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(4, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(4, LOREY_HEIGHT), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(4, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(5, BASAL_AREA), is(3.4186523E-6f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(5, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(5, LOREY_HEIGHT), is(1.800044E-5f));
		assertThat(fpe.fps.getLayerProcessingState().getCVSmall(5, WHOLE_STEM_VOLUME), is(0.0f));
	}
}
