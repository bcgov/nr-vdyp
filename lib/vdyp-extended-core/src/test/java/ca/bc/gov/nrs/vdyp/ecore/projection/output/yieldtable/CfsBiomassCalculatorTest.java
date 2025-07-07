package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class CfsBiomassCalculatorTest {

	private static final String TEST_PROJECTION_ID = "TestProjectionId";
	private static final long TEST_POLYGON_NUMBER = 12345678;

	static Stream<Arguments> biomassAmounts() {
		return Stream.of(
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						127.9, 84.56, 9.26, 11.35, 7.21, 0.05
				), // computed biomass proportions
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						0.01, 0.054, 0.0079, 0.011, 0.010, 0.001
				), // low bound biomass proportions
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						2259.3, 850.09, 65.57, 48.13, 24.98, 0.05
				), // high bound biomass proportions
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						0.0, 0.0, 0.0, 0.0, 0.0, 0.00001
				), // zero biomass proportions,
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						-9.0, -9.0, -9.0, -9.0, -9.0, 0.00001
				), // not supplied biomass proportions
				Arguments.of(
						"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,13,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,",
						127.9, 84.56, 9.80, 12.55, 8.05, 0.05
				) // Supplied cfs eco zone
		);
	}

	@ParameterizedTest
	@MethodSource("biomassAmounts")
	void testPolygonBiomassCalculation(
			String polygonData, double closeUtilizationVolume, double stemwood, double bark, double branches,
			double foliage, double variance
	) throws AbstractProjectionRequestException, IOException {

		var parameters = new Parameters();
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE, polygonData

		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"6535156131,6484558358,000A000,56540605,1,P,,1,,PL,16.30,50,,,PL,30.0,AC,30.0,H,30.0,S,10.0,,,,,7,,,,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		var entityVolumeDetails = new EntityVolumeDetails(
				closeUtilizationVolume, closeUtilizationVolume, closeUtilizationVolume, closeUtilizationVolume,
				closeUtilizationVolume
		);

		var cfsBiomass = CfsBiomassCalculator.calculateBiomassPolygonVolumeDetails(entityVolumeDetails, polygon);
		var totalProp = cfsBiomass.propStemwood() + cfsBiomass.propBark() + cfsBiomass.propBranches()
				+ cfsBiomass.propFoliage();
		if (closeUtilizationVolume != 0.0) {
			assertThat(totalProp, closeTo(1.0, 0.001));
		}
		assertThat(cfsBiomass.bioStemwood(), closeTo(stemwood, variance));
		assertThat(cfsBiomass.bioBark(), closeTo(bark, variance));
		assertThat(cfsBiomass.bioBranches(), closeTo(branches, variance));
		assertThat(cfsBiomass.bioFoliage(), closeTo(foliage, variance));

	}

	@Test
	void testLayerCalculation() throws AbstractProjectionRequestException {
		var parameters = new Parameters();
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"6535156131,000A000,56540605,,UNK,UNK,F,UNK,,,,,,,,IDF,,55,1,,V,T,U,TM,OP,,2025,,,,,,,,,,,,,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"6535156131,6484558358,000A000,56540605,1,P,,1,,FD,16.30,50,,,FD,30.0,AC,30.0,H,30.0,S,10.0,,,,,7,,,,,,,,,,,",
				"6535156131,6484558358,000A000,56540605,2,V,,1,,PL,16.30,50,,,PL,30.0,AC,30.0,H,30.0,S,10.0,,,,,7,,,,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();
		double closeUtilizationVolume = 127.9;
		double stemwood = 84.56;
		double bark = 9.26;
		double branches = 11.35;
		double foliage = 7.21;
		double variance = 0.05;
		var entityVolumeDetails = new EntityVolumeDetails(
				closeUtilizationVolume, closeUtilizationVolume, closeUtilizationVolume, closeUtilizationVolume,
				closeUtilizationVolume
		);

		var cfsBiomass = CfsBiomassCalculator
				.calculateBiomassLayerVolumeDetails(entityVolumeDetails, polygon, polygon.getVeteranLayer());
		var totalProp = cfsBiomass.propStemwood() + cfsBiomass.propBark() + cfsBiomass.propBranches()
				+ cfsBiomass.propFoliage();
		assertThat(totalProp, closeTo(1.0, 0.001));
		assertThat(cfsBiomass.bioStemwood(), closeTo(stemwood, variance));
		assertThat(cfsBiomass.bioBark(), closeTo(bark, variance));
		assertThat(cfsBiomass.bioBranches(), closeTo(branches, variance));
		assertThat(cfsBiomass.bioFoliage(), closeTo(foliage, variance));
	}

	@Test
	void testDeadLayerNoCalculation() {
		var polygon = new Polygon.Builder().polygonNumber(TEST_POLYGON_NUMBER).build();
		var layer = new Layer.Builder().layerId("3").polygon(polygon).isDeadLayer(true).build();
		polygon.assignDeadLayer(layer, 2025, 50.0);
		var entityVolumeDetails = new EntityVolumeDetails(100.0, 98.0, 90.0, 80.0, 70.0);
		var cfsBiomass = CfsBiomassCalculator.calculateBiomassLayerVolumeDetails(entityVolumeDetails, polygon, layer);
		assertThat(cfsBiomass.cfsBiomassMerch(), is(-9.0));
	}

	@Test
	void testNullPolygon() {
		var entityVolumeDetails = new EntityVolumeDetails(100.0, 98.0, 90.0, 80.0, 70.0);
		var cfsBiomass = CfsBiomassCalculator.calculateBiomassPolygonVolumeDetails(entityVolumeDetails, null);
		assertThat(cfsBiomass.cfsBiomassMerch(), is(-9.0));
	}

}
