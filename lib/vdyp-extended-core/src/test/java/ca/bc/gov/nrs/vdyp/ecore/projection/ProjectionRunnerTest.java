package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ProjectionRunnerTest {

	static Stream<Arguments> nonProductiveAndStandData() {
		return Stream.of(
				Arguments.of("NPD", "PLI,100.00,,,,,,,,,,,180,18.00", true), // Non Productive and stand data
				Arguments.of("NPD", ",,,,,,,,,,,,,", false), // Non Productive No Stand Data
				Arguments.of("", ",,,,,,,,,,,,,", false) // No Stand Data
		);
	}

	@ParameterizedTest
	@MethodSource("nonProductiveAndStandData")
	void testNonProductiveAndStand(String nonProductiveCode, String standData, Boolean expectResults)
			throws AbstractProjectionRequestException {
		params = new Parameters().ageStart(0).ageEnd(200).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000," + nonProductiveCode
						+ ",V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,3,V,,,,,,5,10.000010,300," + standData + ",,,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

		assertThat(unit.getProjectionResults().hasNext(), is(expectResults));
	}

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
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
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
	void testAllowBack() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.BACK_GROW_ENABLED)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);
		String results = new String(unit.getYieldTable().readAllBytes());
		assertThat(results.length(), greaterThan(0));
	}

	@Test
	void testPolygonProgressFrequency() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,",
				"13919429,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
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
	void testTooYoungFIPStartFallthroughVRI() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,F,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,,300,PLI,100.00,,,,,,,,,,,7,1.52,,,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

		InputStream progressStream = unit.getProgressStream();
		InputStream errorStream = unit.getErrorStream();
		String progressLog = new String(progressStream.readAllBytes());
		String errorLog = new String(errorStream.readAllBytes());
		assertThat(progressLog, containsString("Processing Polygon 13919428:"));

	}

	@Test
	void testValidFIPStart() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				TestUtils.POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,F,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				TestUtils.LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,,300,PLI,100.00,,,,,,,,,,,7,5,,,,,,,,,,"
		);

		Map<String, InputStream> streams = Map.of(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonInputStream, ParameterNames.HCSV_LAYERS_INPUT_DATA,
				layersInputStream
		);
		unit.run(streams);

		InputStream progressStream = unit.getProgressStream();
		InputStream errorStream = unit.getErrorStream();
		String progressLog = new String(progressStream.readAllBytes());
		String errorLog = new String(errorStream.readAllBytes());
		assertThat(progressLog.contains("Processing Polygon 13919428:"), is(true));

		assertThat(errorLog.length(), is(0));

	}

}
