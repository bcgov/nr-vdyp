package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.scenarios;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class Scenario1 extends Scenario {

	private static final Logger logger = LoggerFactory.getLogger(Scenario1.class);
	private static final String SCENARIO = "scenario1";

	protected Scenario1() {
		super(SCENARIO);
	}

	@Test
	void testVRIPerPolygonYieldTable() throws IOException {

		logger.info("Starting testVRIPerPolygonYieldTable");

		Path resourceFolderPath = getResourceFolderPath();

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);
		parameters.yearStart(2000).yearEnd(2050);

		recordScenarioParameters(parameters);

		InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY.csv").toFile()
				) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER.csv").toFile()
				) //
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.csv", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));
		record(new ByteArrayInputStream(entry1Content.getBytes()), CSV_OUTPUT_FILE_NAME);
		assertTrue(entry1Content.length() > 0);

		ZipEntry entry2 = zipFile.getNextEntry();
		assertEquals("ProgressLog.txt", entry2.getName());
		String entry2Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry2Content.contains("starting projection (type HCSV)"));

		ZipEntry entry3 = zipFile.getNextEntry();
		assertEquals("ErrorLog.txt", entry3.getName());

		ZipEntry entry4 = zipFile.getNextEntry();
		assertEquals("DebugLog.txt", entry4.getName());
		String entry4Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry4Content.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		ZipEntry projectionResultsEntry;
		var outputSeen = false;
		while ( (projectionResultsEntry = zipFile.getNextEntry()) != null) {
			logger.info("Name: {}", projectionResultsEntry.getName());
			String entryContent = new String(testHelper.readZipEntry(zipFile, projectionResultsEntry));
			if (entryContent.length() > 0) {
				logger.info("Content: {}", entryContent.substring(0, Math.min(entryContent.length(), 60)));
			} else {
				logger.info("Content: <empty>");
			}

			outputSeen = true;
		}
		Assert.assertTrue(outputSeen);
	}
}
