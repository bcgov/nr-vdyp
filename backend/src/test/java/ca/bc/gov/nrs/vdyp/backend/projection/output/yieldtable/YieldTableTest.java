package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.api.helpers.TestProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class YieldTableTest {

	private static final String POLYGON_CSV_HEADER_LINE = //
			"FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT,TSA_NAME,TFL_NAME,INVENTORY_STANDARD_CODE,TSA_NUMBER"
					+ ",SHRUB_HEIGHT,SHRUB_CROWN_CLOSURE,SHRUB_COVER_PATTERN,HERB_COVER_TYPE_CODE,HERB_COVER_PCT"
					+ ",HERB_COVER_PATTERN_CODE,BRYOID_COVER_PCT,BEC_ZONE_CODE,CFS_ECOZONE,PRE_DISTURBANCE_STOCKABILITY"
					+ ",YIELD_FACTOR,NON_PRODUCTIVE_DESCRIPTOR_CD,BCLCS_LEVEL1_CODE,BCLCS_LEVEL2_CODE,BCLCS_LEVEL3_CODE"
					+ ",BCLCS_LEVEL4_CODE,BCLCS_LEVEL5_CODE,PHOTO_ESTIMATION_BASE_YEAR,REFERENCE_YEAR,PCT_DEAD"
					+ ",NON_VEG_COVER_TYPE_1,NON_VEG_COVER_PCT_1,NON_VEG_COVER_PATTERN_1,NON_VEG_COVER_TYPE_2"
					+ ",NON_VEG_COVER_PCT_2,NON_VEG_COVER_PATTERN_2,NON_VEG_COVER_TYPE_3,NON_VEG_COVER_PCT_3"
					+ ",NON_VEG_COVER_PATTERN_3,LAND_COVER_CLASS_CD_1,LAND_COVER_PCT_1,LAND_COVER_CLASS_CD_2"
					+ ",LAND_COVER_PCT_2,LAND_COVER_CLASS_CD_3,LAND_COVER_PCT_3";

	private static final String LAYER_CSV_HEADER_LINE = //
			"FEATURE_ID,TREE_COVER_LAYER_ESTIMATED_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE"
					+ ",VDYP7_LAYER_CD,LAYER_STOCKABILITY,FOREST_COVER_RANK_CODE,NON_FOREST_DESCRIPTOR_CODE"
					+ ",EST_SITE_INDEX_SPECIES_CD,ESTIMATED_SITE_INDEX,CROWN_CLOSURE,BASAL_AREA_75,STEMS_PER_HA_75"
					+ ",SPECIES_CD_1,SPECIES_PCT_1,SPECIES_CD_2,SPECIES_PCT_2,SPECIES_CD_3,SPECIES_PCT_3,SPECIES_CD_4"
					+ ",SPECIES_PCT_4,SPECIES_CD_5,SPECIES_PCT_5,SPECIES_CD_6,SPECIES_PCT_6,EST_AGE_SPP1,"
					+ "EST_HEIGHT_SPP1,EST_AGE_SPP2,EST_HEIGHT_SPP2,ADJ_IND,LOREY_HEIGHT_75,BASAL_AREA_125,"
					+ "WS_VOL_PER_HA_75,WS_VOL_PER_HA_125,CU_VOL_PER_HA_125,D_VOL_PER_HA_125,DW_VOL_PER_HA_125";

	private static String TEST_PROJECTION_ID = "TestProjectionId";
	private static long TEST_POLYGON_NUMBER = 12345678;

	private static TestHelper testHelper;
	private static Path resourceFolderPath = Path.of(FileHelper.TEST_DATA_FILES, FileHelper.YIELD_TABLE_TEST_DATA, "1");

	@BeforeAll
	public static void startUp() {
		testHelper = new TestHelper();
	}

	@Test
	void testGenerateYieldTableFramework() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //

				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //

				Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES, //
				Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
		);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var yieldTable = YieldTable.of(context);

		yieldTable.startGeneration();
		yieldTable.endGeneration();

		var content = new String(yieldTable.getAsStream().readAllBytes());
		Assert.assertTrue(content.length() == 0);
	}

	@Test
	void testMinimalYieldTableGeneration() throws AbstractProjectionRequestException, IOException {

		var parameters = new Parameters();

		// Either an age range or a year range must be supplied.
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);
		var polygon = new Polygon.Builder().polygonNumber(TEST_POLYGON_NUMBER).build();

		var yieldTable = YieldTable.of(context);

		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();

			ProjectionResultsReader forwardReader = new NullProjectionResultsReader();
			ProjectionResultsReader backReader = new NullProjectionResultsReader();
			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(polygon, projectionResults, state, false);

		} finally {
			yieldTable.endGeneration();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());
		Assert.assertTrue(content.length() == 0);
	}

	@Test
	void testSinglePolygonYieldTableGeneration() throws AbstractProjectionRequestException, IOException {

		var parameters = new Parameters();
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(resourceFolderPath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(resourceFolderPath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(resourceFolderPath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(polygon, projectionResults, state, false);
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var resultYieldTable = new ResultYieldTable(new String(yieldTable.getAsStream().readAllBytes()));
		Assert.assertTrue(resultYieldTable.containsKey("13919428"));
		Assert.assertTrue(resultYieldTable.get("13919428").containsKey(""));
	}
}
