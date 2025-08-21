package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.api.helpers.TestProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationClassSet;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class YieldTableTest {

	public static final Logger logger = LoggerFactory.getLogger(YieldTableTest.class);

	private static String TEST_PROJECTION_ID = "TestProjectionId";
	private static long TEST_POLYGON_NUMBER = 12345678;

	private static TestHelper testHelper;
	private static Path relativeResourcePath = Path
			.of(FileHelper.TEST_DATA_FILES, FileHelper.YIELD_TABLE_TEST_DATA, "1");

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
		assertTrue(content.length() == 0);
	}

	static Stream<Arguments> yieldTableWriterTypes() {
		return Stream.of(
				Arguments.of(Parameters.OutputFormat.DCSV, ProjectionRequestKind.DCSV), //
				Arguments.of(Parameters.OutputFormat.PLOTSY, ProjectionRequestKind.HCSV), //
				Arguments.of(Parameters.OutputFormat.YIELD_TABLE, ProjectionRequestKind.HCSV)
		);
	}

	@ParameterizedTest
	@MethodSource("yieldTableWriterTypes")
	void testGenerateYieldTableFramework(Parameters.OutputFormat format, ProjectionRequestKind kind)
			throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters().outputFormat(format), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //

				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //

				Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
		);

		if (format != Parameters.OutputFormat.DCSV) {
			parameters.ageStart(0).ageEnd(100);
		}

		var context = new ProjectionContext(kind, TEST_PROJECTION_ID, parameters, false);

		var yieldTable = YieldTable.of(context);

		yieldTable.startGeneration();
		yieldTable.endGeneration();

		var content = new String(yieldTable.getAsStream().readAllBytes());
		if (format == Parameters.OutputFormat.YIELD_TABLE) {
			assertFalse(content.isEmpty()); // this should have only the text headers
		} else {
			assertTrue(content.isEmpty());
		}
	}

	@Test
	void testNoLayerReportingInfo() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(new Parameters().ageStart(0).ageEnd(100));

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var yieldTable = YieldTable.of(context);

		assertThrows(
				IllegalArgumentException.class,
				() -> yieldTable
						.generateYieldTableForPolygonLayer(new Polygon.Builder().build(), null, null, null, true)
		);

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
		assertTrue(content.length() == 0);
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

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(
					polygon, projectionResults, state, false /* don't generate detailed table header */
			);
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var resultYieldTable = new ResultYieldTable(new String(yieldTable.getAsStream().readAllBytes()));
		assertTrue(resultYieldTable.containsKey("13919428"));
		assertTrue(resultYieldTable.get("13919428").containsKey(""));
	}

	@Test
	void testIncludeSmall() throws AbstractProjectionRequestException, IOException {

		var parameters = new Parameters();
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		// Include small utilization class
		parameters
				.addUtilsItem(new UtilizationParameter().speciesName("PL").utilizationClass(UtilizationClassSet._4_0));

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

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(
					polygon, projectionResults, state, false /* don't generate detailed table header */
			);
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var resultYieldTable = new ResultYieldTable(new String(yieldTable.getAsStream().readAllBytes()));
		assertThat(
				resultYieldTable,
				VdypMatchers.recursiveHasEntry(is("13919428"), is(""), is("2029"), is("PRJ_LOREY_HT"))
						.withValue(VdypMatchers.parseAs(closeTo(21.3734f), ValueParser.FLOAT))
		);

	}

	@Test
	void testDoIncludeSecondarySpeciesDominantHeight() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters().yearStart(2025).yearEnd(2030), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //

				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //

				Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES, //
				Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES, //
				Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
		);

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

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateYieldTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, false
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var resultYieldTable = new ResultYieldTable(new String(yieldTable.getAsStream().readAllBytes()));
		assertTrue(resultYieldTable.containsKey("13919428"));
		assertTrue(resultYieldTable.get("13919428").containsKey("1"));
	}

	@Test
	void testMOFVolumes() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters().yearStart(2025).yearEnd(2030),
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES,
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS,
				Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION
		);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,100,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);
			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateYieldTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, false
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var resultYieldTable = new ResultYieldTable(new String(yieldTable.getAsStream().readAllBytes()));
		assertTrue(resultYieldTable.containsKey("13919428"));
		assertTrue(resultYieldTable.get("13919428").containsKey("1"));
	}

	@Test
	void testGetDoSuppressPerHAYields() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		// "NSR" in NON_PRODUCTIVE_DESCRIPTOR_CD field turns on SuppressPerHAYields
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,NSR,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			resourceFolderPath = vdypPolygonStreamFile.getParent().toAbsolutePath();

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(
					polygon, projectionResults, state, false /* don't generate detailed table header */
			);
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var csvContent = yieldTable.getAsStream().readAllBytes();
		var csvFilePath = Path.of(resourceFolderPath.toString(), "outputYieldTable.csv");
		Files.write(csvFilePath, csvContent);
		logger.info("Resulting CSV file written to " + csvFilePath);

		var vdyp8ResultYieldTable = new ResultYieldTable(new String(csvContent));
		var vdyp7ResultYieldTable = new ResultYieldTable(
				Files.readString(Path.of(resourceFolderPath.toString(), "vdyp7_Output_YldTbl-do-suppress.csv"))
		);

		ResultYieldTable.compareWithTolerance(vdyp7ResultYieldTable, vdyp8ResultYieldTable, 0.01);
	}

	@Test
	void testTextYieldTableWriter() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);
		parameters.setOutputFormat(Parameters.OutputFormat.YIELD_TABLE);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		// "NSR" in NON_PRODUCTIVE_DESCRIPTOR_CD field turns on SuppressPerHAYields
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			resourceFolderPath = vdypPolygonStreamFile.getParent().toAbsolutePath();

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);

			yieldTable.generateYieldTableForPolygon(polygon, projectionResults, state, true);
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());

		assertThat(content.length(), greaterThan(0));
	}

	@Test
	void testTextYieldTableWriterForLayer() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);
		parameters.setYearStart(2025);
		parameters.setYearEnd(2030);
		parameters.setOutputFormat(Parameters.OutputFormat.YIELD_TABLE);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		// "NSR" in NON_PRODUCTIVE_DESCRIPTOR_CD field turns on SuppressPerHAYields
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);
			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateYieldTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, true
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());

		assertThat(content.length(), greaterThan(0));
	}

	@Test
	void testCSVCFSBiomassTable() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);
		testHelper.addExcludedOptions(parameters, Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		parameters.setYearStart(2013);
		parameters.setYearEnd(2050);
		parameters.setOutputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		// "NSR" in NON_PRODUCTIVE_DESCRIPTOR_CD field turns on SuppressPerHAYields
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);
			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateCfsBiomassTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, true
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());
		ResultYieldTable csvTable = new ResultYieldTable(content);
		var yieldTableRow = csvTable.get("13919428").get("1").get("2013");
		/* DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE - default on */
		assertTrue(yieldTableRow.containsKey("PRJ_MODE"));
		assertTrue(yieldTableRow.containsKey("POLYGON_ID"));
		assertTrue(yieldTableRow.containsKey("PRJ_CFS_BIO_STEM"));
		assertTrue(yieldTableRow.containsKey("PRJ_CFS_BIO_BARK"));
		assertTrue(yieldTableRow.containsKey("PRJ_CFS_BIO_BRANCH"));
		assertTrue(yieldTableRow.containsKey("PRJ_CFS_BIO_FOLIAGE"));
		assertFalse(yieldTableRow.containsKey("PRJ_VOL_WS"));
	}

	@Test
	void testTextCFSBiomassTable() throws AbstractProjectionRequestException, IOException {

		var parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES
		);
		parameters.setYearStart(2013);
		parameters.setYearEnd(2050);
		parameters.setOutputFormat(Parameters.OutputFormat.YIELD_TABLE);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		// "NSR" in NON_PRODUCTIVE_DESCRIPTOR_CD field turns on SuppressPerHAYields
		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);
			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateYieldTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, true
					);
					yieldTable.generateCfsBiomassTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, true
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());

		assertThat(content.length(), greaterThan(0));
		assertThat(content, containsString("Vws"));
		assertThat(content, containsString("Vcu"));
		assertThat(content, containsString("Vdw"));
		assertThat(content, containsString("Vd"));
		assertThat(content, containsString("Vd"));
		assertThat(content, containsString("Bstem"));
		assertThat(content, containsString("Bbark"));
		assertThat(content, containsString("Bbranch"));
		assertThat(content, containsString("Bfol"));
		assertThat(content, containsString("Mode"));
		assertThat(content, containsString("Table Number: 2"));
	}

	static Stream<Arguments> yieldTableExecutionOptions() {
		return Stream.of(
				Arguments.of(
						List.of(
								Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
								Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
								Parameters.ExecutionOption.REPORT_INCLUDE_WHOLE_STEM_VOLUME, //
								Parameters.ExecutionOption.REPORT_INCLUDE_CLOSE_UTILIZATION_VOLUME, //
								Parameters.ExecutionOption.REPORT_INCLUDE_NET_DECAY_VOLUME, //
								Parameters.ExecutionOption.REPORT_INCLUDE_ND_WASTE_VOLUME, //
								Parameters.ExecutionOption.REPORT_INCLUDE_ND_WAST_BRKG_VOLUME, //
								Parameters.ExecutionOption.REPORT_INCLUDE_CULMINATION_VALUES, //
								Parameters.ExecutionOption.REPORT_INCLUDE_VOLUME_MAI,
								Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						),
						"My Testing VDYP Yield Table Report that is longer than 80 characters so that it will be wrapped in the output"
				),
				Arguments.of(
						List.of(
								Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS, //
								Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER
						), //
						"My Testing VDYP Yield Table Report"
				)
		);
	}

	@ParameterizedTest
	@MethodSource("yieldTableExecutionOptions")
	void testMOFFullReportYieldTable(List<Parameters.ExecutionOption> options, String reportTitle)
			throws AbstractProjectionRequestException, IOException {

		var parameters = new Parameters();
		for (var option : options) {
			parameters.addSelectedExecutionOptionsItem(option);
		}

		parameters.setAgeStart(180);
		parameters.setAgeEnd(217);
		parameters.setReportTitle(
				"My Testing VDYP Yield Table Report that is longer than 80 characters so that it will be wrapped in the output"
		);
		parameters.setOutputFormat(Parameters.OutputFormat.TEXT_REPORT);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, TEST_PROJECTION_ID, parameters, false);

		var polygonInputStream = TestUtils.makeInputStream(
				//
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				//
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);

		var polygonStream = new HcsvPolygonStream(context, polygonInputStream, layersInputStream);

		var polygon = polygonStream.getNextPolygon();

		Path resourceFolderPath;
		var yieldTable = YieldTable.of(context);
		try {
			yieldTable.startGeneration();

			var state = new PolygonProjectionState();
			state.setProcessingResults(ProjectionStageCode.Initial, ProjectionTypeCode.PRIMARY, Optional.empty());
			state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

			var vdypPolygonStreamFile = testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat");
			var vdypPolygonStream = Files.newInputStream(vdypPolygonStreamFile);
			var vdypSpeciesStreamFile = testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat");
			var vdypSpeciesStream = Files.newInputStream(vdypSpeciesStreamFile);
			var vdypUtilizationsStreamFile = testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat");
			var vdypUtilizationsStream = Files.newInputStream(vdypUtilizationsStreamFile);

			ProjectionResultsReader forwardReader = new TestProjectionResultsReader(
					testHelper, vdypPolygonStream, vdypSpeciesStream, vdypUtilizationsStream
			);
			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, ProjectionTypeCode.PRIMARY, forwardReader, backReader);
			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {
				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {
					yieldTable.generateCfsBiomassTableForPolygonLayer(
							polygon, projectionResults, state, layerReportingInfo, true
					);
				}
			}
		} finally {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(yieldTable.getAsStream().readAllBytes());

		assertThat(content.length(), greaterThan(0));
		assertThat(content, containsString("VDYP Yield Table Report"));
		assertThat(content, containsString("My Testing VDYP Yield Table Report"));
		assertThat(content, containsString("TABLE PROPERTIES..."));
		assertThat(content, containsString("Species Parameters..."));
		assertThat(content, containsString("Site Index Curves Used..."));
		assertThat(content, containsString("Additional Stand Attributes:"));
	}

}
