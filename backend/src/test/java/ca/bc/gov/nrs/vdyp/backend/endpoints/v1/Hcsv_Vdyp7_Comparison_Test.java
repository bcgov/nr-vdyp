package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class Hcsv_Vdyp7_Comparison_Test {

	private static final Logger logger = LoggerFactory.getLogger(Hcsv_Vdyp7_Comparison_Test.class);

	private final TestHelper testHelper;

	@Inject
	Hcsv_Vdyp7_Comparison_Test(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testVdyp8VersusVdyp7YieldTables() throws IOException {

		logger.info("Starting testVdyp8VersusVdyp7YieldTables");

		Path resourceFolderPath = Path.of(FileHelper.TEST_DATA_FILES, FileHelper.HCSV, "vdyp7-comparison-test");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE
		);
		parameters.ageStart(0).ageEnd(250).ageIncrement(25);

		// Included to generate JSON text of parameters as needed
//		ObjectMapper mapper = new ObjectMapper();
//		String serializedParametersText = mapper.writeValueAsString(parameters);

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
		String vdyp8YieldTableContent = new String(testHelper.readZipEntry(zipFile, entry1));
		assertTrue(vdyp8YieldTableContent.length() > 0);

		var vdyp8YieldTable = new ResultYieldTable(vdyp8YieldTableContent);

		String vdyp7YieldTableContent = new String(
				Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "vdyp7-yield-table.csv"))
		);
		var vdyp7YieldTable = new ResultYieldTable(vdyp7YieldTableContent);

		// FIXME VDYP-604 stop ignoring columns once fixed
		ResultYieldTable
				.compareWithTolerance(vdyp7YieldTable, vdyp8YieldTable, 0.02, IGNORE_COLUMNS.asMatchPredicate());

		ZipEntry entry2 = zipFile.getNextEntry();
		assertEquals("ProgressLog.txt", entry2.getName());
		String entry2Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry2Content.contains("starting projection (type HCSV)"));

		ZipEntry entry3 = zipFile.getNextEntry();
		assertEquals("ErrorLog.txt", entry3.getName());
		String entry3Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry3Content.length() == 0);

		ZipEntry entry4 = zipFile.getNextEntry();
		assertEquals("DebugLog.txt", entry4.getName());
		String entry4Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry4Content.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		ZipEntry projectionResultsEntry;
		var outputSeen = false;
		while ( (projectionResultsEntry = zipFile.getNextEntry()) != null) {
			String entryContent = new String(testHelper.readZipEntry(zipFile, projectionResultsEntry));

			if (projectionResultsEntry.getName().endsWith("ForwardCompatibility")) {
				Assert.assertTrue(entryContent.length() == 0);
			} else {
				assert (entryContent.length() > 0);
				assert (entryContent.startsWith("092P037  72999905    2011"));
				logger.info(
						"Name: {} with content: {}", projectionResultsEntry.getName(),
						entryContent.substring(0, Math.min(entryContent.length(), 60))
				);
			}
			outputSeen = true;
		}
		Assert.assertTrue(outputSeen);
	}

	// FIXME VDYP-604 Remove these once VDYP-604 is fixed.
	static final Pattern IGNORE_COLUMNS = Pattern
			.compile("PRJ_TPH|PRJ_LOREY_HT|PRJ_DIAMETER|PRJ_BA|PRJ_(SP\\d_)?VOL_(?:D|DW|DWB|CU|WS)");

}
