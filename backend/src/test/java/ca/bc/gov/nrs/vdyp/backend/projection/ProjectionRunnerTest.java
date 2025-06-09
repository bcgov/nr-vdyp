package ca.bc.gov.nrs.vdyp.backend.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ProjectionRunnerTest {
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

	ProjectionRunner unit;
	Parameters params;

	@Test
	void testMapSheetProgressFrequency() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(100).progressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		// TODO once supported this needs to make a non HCSV Stream because HCSV streams do not support Districts so
		// this type of progress throws an exception if there is more than 1 polygon
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,V,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

		InputStream progressStream = unit.getProgressStream();
		String progressLog = new String(progressStream.readAllBytes());
		assertThat(progressLog.contains("Processing Map Sheet: \"null\", \"093C090\""), is(true));
	}

	@Test
	void testPolygonProgressFrequency() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(100).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,",
				"13919429,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,V,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,",
				"13919429,14321066,093C090,94833422,1,V,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

		InputStream progressStream = unit.getProgressStream();
		String progressLog = new String(progressStream.readAllBytes());
		assertThat(progressLog.contains("Processing Polygon 13919428:"), is(true));
		assertThat(progressLog.contains("Processing Polygon 13919429:"), is(true));
	}

	@Test
	void testFIPStartResultExceptions() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,F,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,V,,1,,,,20,0.1,,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

	}

}
