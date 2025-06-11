package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.api.helpers.TestProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

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
}
