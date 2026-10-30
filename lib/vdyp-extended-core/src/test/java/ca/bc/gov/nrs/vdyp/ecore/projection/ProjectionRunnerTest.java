package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ProjectionRunnerTest {

	static Stream<Arguments> layerErrorInputs() {
		// V = valid, I = invalid, - = absent. Each position identifies the same feature in both files.
		return Stream.of(
				Arguments.of("VV", "IV", 1, 1), // Layer failure with a valid parent
				Arguments.of("IV", "IV", 1, 1), // Layer failure with a rejected parent
				Arguments.of("III", "III", 3, 0), // Every parent and layer rejected
				Arguments.of("-V", "IV", 0, 1), // Orphan before first polygon
				Arguments.of("V--", "VVI", 0, 1), // Trailing invalid layer beyond iterator read-ahead
				Arguments.of("---", "VVI", 0, 0), // No polygon rows
				Arguments.of("VVV", "VII", 2, 1) // Future errors must not invalidate an earlier polygon
		);
	}

	@ParameterizedTest
	@MethodSource("layerErrorInputs")
	void testLayerErrorsAreConsumedOnce(String polygons, String layers, int skipped, int processed) throws Exception {
		var polygonRows = new ArrayList<String>();
		var layerRows = new ArrayList<String>();
		polygonRows.add(TestUtils.POLYGON_CSV_HEADER_LINE);
		layerRows.add(TestUtils.LAYER_CSV_HEADER_LINE);
		for (int i = 0; i < polygons.length(); i++) {
			if (polygons.charAt(i) != '-') {
				polygonRows.add(
						(13919428L + i) + ",093C090,94833422,DQU,UNK,UNK," + (polygons.charAt(i) == 'I' ? "Z" : "V")
								+ ",UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
				);
			}
		}
		for (int i = 0; i < layers.length(); i++) {
			layerRows.add(
					(13919428L + i) + ",14321066,093C090,94833422,1,P,,1,,,,20,10,300,"
							+ (layers.charAt(i) == 'I' ? "" : "PLI") + ",100.00,,,,,,,,,,,180,18.00,,,,,,,,,,"
			);
		}
		try (
				var runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "TEST", validationLoggingParameters(), false
				)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA,
							TestUtils.makeInputStream(polygonRows.toArray(String[]::new)),
							ParameterNames.HCSV_LAYERS_INPUT_DATA,
							TestUtils.makeInputStream(layerRows.toArray(String[]::new))
					), new StubComponentRunner()
			);
			String errors = new String(runner.getErrorStream().readAllBytes());
			assertEquals(skipped, runner.getLastRunPolygonsSkipped(), errors);
			assertEquals(processed, runner.getLastRunPolygonsProcessed(), errors);
			for (int i = 0; i < layers.length(); i++) {
				String prefix = "Polygon " + (13919428L + i) + " Layer 1";
				assertEquals(
						layers.charAt(i) == 'I' ? 1 : 0, errors.lines().filter(line -> line.contains(prefix)).count(),
						errors
				);
			}
			for (int i = 0; i < polygons.length(); i++) {
				String prefix = "Polygon " + (13919428L + i);
				assertEquals(
						polygons.charAt(i) == 'I' ? 1 : 0,
						errors.lines().filter(line -> line.contains(prefix) && line.contains("Inventory Standard Code"))
								.count(),
						errors
				);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "IV", "VIV", "VI", "III", "IVIVI" })
	void testRejectedPolygonRowsAreLoggedOnce(String rows) throws Exception {
		var polygonRows = new ArrayList<String>();
		var layerRows = new ArrayList<String>();
		polygonRows.add(TestUtils.POLYGON_CSV_HEADER_LINE);
		layerRows.add(TestUtils.LAYER_CSV_HEADER_LINE);
		int invalidCount = 0;
		for (int i = 0; i < rows.length(); i++) {
			long featureId = 13919428L + i;
			boolean invalid = rows.charAt(i) == 'I';
			invalidCount += invalid ? 1 : 0;
			polygonRows.add(
					featureId + ",093C090,94833422,DQU,UNK,UNK," + (invalid ? "Z" : "V")
							+ ",UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
			);
			layerRows.add(
					featureId
							+ ",14321066,093C090,94833422,1,V,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
			);
		}
		try (
				var runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "TEST", validationLoggingParameters(), false
				)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA,
							TestUtils.makeInputStream(polygonRows.toArray(String[]::new)),
							ParameterNames.HCSV_LAYERS_INPUT_DATA,
							TestUtils.makeInputStream(layerRows.toArray(String[]::new))
					), new StubComponentRunner()
			);
			String errors = new String(runner.getErrorStream().readAllBytes());
			assertEquals(
					invalidCount, errors.lines().filter(line -> line.contains("Inventory Standard Code")).count(),
					errors
			);
			assertEquals(rows.length(), runner.getLastRunPolygonsSeen());
			assertEquals(invalidCount, runner.getLastRunPolygonsSkipped());
			assertEquals(rows.length() - invalidCount, runner.getLastRunPolygonsProcessed());
			for (int i = 0; i < rows.length(); i++) {
				String prefix = "Polygon " + (13919428L + i);
				assertEquals(
						rows.charAt(i) == 'I' ? 1 : 0,
						errors.lines().filter(line -> line.contains(prefix) && line.contains("Inventory Standard Code"))
								.count(),
						errors
				);
			}
		}
	}

	@Test
	void testBuildFailureAlsoLogsUnreadLayerValidationErrors() throws Exception {
		try (
				var runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "TEST", validationLoggingParameters(), false
				)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA,
							TestUtils.makeInputStream(
									TestUtils.POLYGON_CSV_HEADER_LINE,
									"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
							), ParameterNames.HCSV_LAYERS_INPUT_DATA,
							TestUtils.makeInputStream(
									TestUtils.LAYER_CSV_HEADER_LINE,
									"13919428,14321066,093C090,94833422,1,V,,1,,,,20,10,300,PLI,100.00,,,,,,,,,,,180,18.00,,,,,,,,,,",
									"13919428,14321067,093C090,94833422,1,V,,,,,,20,10,300,PLI,100.00,,,,,,,,,,,180,18.00,,,,,,,,,,",
									"13919428,14321068,093C090,94833422,2,V,,,,,,20,10,300,PLI,100.00,,,,,,,,,,,180,18.00,,,,,,,,,,",
									"13919428,14321069,093C090,94833422,3,V,,,,,,20,10,300,,100.00,,,,,,,,,,,180,18.00,,,,,,,,,,"
							)
					), new StubComponentRunner()
			);
			String errors = new String(runner.getErrorStream().readAllBytes());
			assertThat(errors, containsString("Polygon 13919428 Layer 3"));
			assertThat(errors, containsString("has already been supplied"));
			assertEquals(4, runner.getErrorLogCount(), errors);
			assertEquals(1, runner.getLastRunPolygonsSkipped());
		}
	}

	@Test
	void testInvalidFeatureIdIsLoggedWithOriginalIdentity() throws Exception {
		try (
				var runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "TEST", validationLoggingParameters(), false
				)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA,
							TestUtils.makeInputStream(
									TestUtils.POLYGON_CSV_HEADER_LINE,
									"bad-id,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
							), ParameterNames.HCSV_LAYERS_INPUT_DATA,
							TestUtils.makeInputStream(TestUtils.LAYER_CSV_HEADER_LINE)
					), new StubComponentRunner()
			);
			String errors = new String(runner.getErrorStream().readAllBytes());
			assertThat(errors, containsString("Polygon bad-id"));
			assertThat(errors, containsString("Feature ID"));
			assertEquals(3, runner.getErrorLogCount());
			assertEquals(1, runner.getLastRunPolygonsSkipped());
		}
	}

	private static Parameters validationLoggingParameters() {
		return new Parameters().ageStart(0).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING);
	}

	static Stream<Arguments> unassignableLayerErrors() {
		return Stream.of(
				Arguments.of(
						"bad-id,14321066,093C090,94833422,1,P,,1,,,,20,10,300,PLI,100.00,,,,,,,,,,,180,18.00,,,,,,,,,,",
						"Polygon bad-id"
				), Arguments.of("13919428", "Polygon 13919428")
		);
	}

	@ParameterizedTest
	@MethodSource("unassignableLayerErrors")
	void testUnassignableLayerErrorsAreLogged(String layer, String expectedContext) throws Exception {
		try (
				var runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "TEST", validationLoggingParameters(), false
				)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA,
							TestUtils.makeInputStream(TestUtils.POLYGON_CSV_HEADER_LINE),
							ParameterNames.HCSV_LAYERS_INPUT_DATA,
							TestUtils.makeInputStream(TestUtils.LAYER_CSV_HEADER_LINE, layer)
					), new StubComponentRunner()
			);
			String errors = new String(runner.getErrorStream().readAllBytes());
			assertThat(errors, containsString(expectedContext));
			assertEquals(3, runner.getErrorLogCount(), errors);
			assertEquals(0, runner.getLastRunPolygonsSeen());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void debugLogIsCreatedOnlyWhenSelected(boolean enabled) throws Exception {
		Parameters parameters = new Parameters().ageStart(0).ageEnd(10);
		if (enabled) {
			parameters.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING);
		}
		try (
				ProjectionRunner runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, "DEBUG-TEST", parameters, true
				); InputStream polygons = TestUtils.makeInputStream(TestUtils.POLYGON_CSV_HEADER_LINE);
				InputStream layers = TestUtils.makeInputStream(TestUtils.LAYER_CSV_HEADER_LINE)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA, polygons, ParameterNames.HCSV_LAYERS_INPUT_DATA,
							layers
					)
			);
			assertEquals(enabled, Files.exists(runner.getContext().getExecutionFolder().resolve("DebugLog.txt")));
			if (enabled) {
				try (InputStream debug = runner.getDebugStream()) {
					String debugContents = new String(debug.readAllBytes());
					assertThat(debugContents, containsString("doEnableDebugLogging"));
				}
			}
		}
	}

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

		assertThat("Next projection results present", unit.getProjectionResults().hasNext(), is(expectResults));
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
		assertThat("progressLog", progressLog, containsString("Processing Map Sheet: \"null\", \"093C090\""));
	}

	@Test
	void testGenerateCFSBiomassByPolygon() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().yearStart(2013).yearEnd(2113)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				// excluded....
				.addExcludedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
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

		YieldTable yieldTable = unit.getContext().getYieldTables().get(0);
		String results = new String(yieldTable.getAsStream().readAllBytes());
		assertThat("results", results, containsString("CFS_BIO"));
	}

	@Test
	void testGenerateMOFBiomassByPolygon() throws AbstractProjectionRequestException, IOException {
		params = new Parameters().yearStart(2013).yearEnd(2113)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);
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

		YieldTable yieldTable = unit.getContext().getYieldTables().get(0);
		String results = new String(yieldTable.getAsStream().readAllBytes());
		assertThat("results", results, containsString("MoF_BIO"));
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
		YieldTable yieldTable = unit.getContext().getYieldTables().get(0);
		String results = new String(yieldTable.getAsStream().readAllBytes());
		assertThat("results", results, not(emptyString()));
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
		assertThat(progressLog.contains("Processing Polygon 13919428"), is(true));
		assertThat(progressLog.contains("Processing Polygon 13919429"), is(true));
	}

	@Test
	void getErrorLogCount_returnsErrorLogCount_FromContext() throws AbstractProjectionRequestException {
		params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		unit = new ProjectionRunner(ProjectionRequestKind.HCSV, "TEST", params, false);
		assertEquals(0, unit.getErrorLogCount());
		assertEquals(0, unit.getLastRunPolygonsProcessed());
		assertEquals(0, unit.getLastRunPolygonsSeen());
		assertEquals(0, unit.getLastRunPolygonsSkipped());
	}

}
