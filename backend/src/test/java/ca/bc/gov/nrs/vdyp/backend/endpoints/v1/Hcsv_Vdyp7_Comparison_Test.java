package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecificEntry;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.io.read.ParamsReader;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.integration_tests.MainTest;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
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
		var socketTimeout = RestAssured.config().getHttpClientConfig().params().get("http.socket.timeout");
		var connectionTimeout = RestAssured.config().getHttpClientConfig().params().get("http.connection.timeout");
		RestAssured.config = RestAssuredConfig.config().httpClient(
				HttpClientConfig.httpClientConfig().setParam("http.socket.timeout", 24 * 60 * 60 * 1000)
						.setParam("http.connection.timeout", 24 * 60 * 60 * 1000)
		);
	}

	@AfterEach
	void teardown() {
		RestAssured.reset();
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
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
		);
		parameters.ageStart(0).ageEnd(250).ageIncrement(25);

		// Included to generate JSON text of parameters as needed
		// ObjectMapper mapper = new ObjectMapper();
		// String serializedParametersText = mapper.writeValueAsString(parameters);

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
		assertThat(vdyp8YieldTableContent, not(emptyString()));

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
	// PRJ_SCND_HT was included in IGNORE_COLUMNS_EXCEPT_LH due to VDYP-804 and is
	// unrelated to VDYP-604.
	static final Pattern IGNORE_COLUMNS = Pattern.compile("PRJ_SCND_HT|PRJ_(SP\\d_)?VOL_(?:D|DW|DWB|CU|WS)");

	@Test
	void test755() throws IOException, ResourceParseException {

		logger.info("Starting test604");

		Map<String, List<String>> paramMap = new HashMap<>();
		var parameters = new Parameters();
		try (
				InputStream paramStream = MainTest.class.getResourceAsStream("v-record-test-08-noback/input/parms.txt");
				InputStreamReader reader = new InputStreamReader(paramStream);
				BufferedReader bufReader = new BufferedReader(reader); var lines = bufReader.lines();
		) {
			ParamsReader.parseParameters(paramMap, lines);
			ParamsReader.parseParameters(parameters, paramMap);
		}

		// Included to generate JSON text of parameters as needed
		// ObjectMapper mapper = new ObjectMapper();
		// String serializedParametersText = mapper.writeValueAsString(parameters);

		try (
				InputStream polyStream = MainTest.class
						.getResourceAsStream("v-record-test-08-noback/input/VDYP7_INPUT_POLY.csv");
				InputStream layerStream = MainTest.class
						.getResourceAsStream("v-record-test-08-noback/input/VDYP7_INPUT_LAYER.csv");
		) {

			InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
					.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
					.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, "VDYP7_INPUT_POLY.csv", polyStream) //
					.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, "VDYP7_INPUT_LAYER.csv", layerStream) //
					.post("/projection/hcsv?trialRun=false") //
					.then().statusCode(201) //
					.and().contentType("application/octet-stream") //
					.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
					.extract().body().asInputStream();

			ZipInputStream zipFile = new ZipInputStream(zipInputStream);
			ZipEntry entry1 = zipFile.getNextEntry();
			assertEquals("YieldTable.csv", entry1.getName());
			String vdyp8YieldTableContent = new String(testHelper.readZipEntry(zipFile, entry1));
			assertThat(vdyp8YieldTableContent, not(emptyString()));

			var vdyp8YieldTable = new ResultYieldTable(vdyp8YieldTableContent);
			vdyp8YieldTable.get("2423088").get("1").get("2113").get("PRJ_LOREY_HT"); // 31.7994 but should be 32.4588
		}
	}

	@Test
	void test812() throws IOException, ResourceParseException {

		logger.info("Starting test604");

		Map<String, List<String>> paramMap = new HashMap<>();
		var parameters = new Parameters();
		try (
				InputStream paramStream = MainTest.class.getResourceAsStream("f-record-test-01-noback/input/parms.txt");
				InputStreamReader reader = new InputStreamReader(paramStream);
				BufferedReader bufReader = new BufferedReader(reader); var lines = bufReader.lines();
		) {
			ParamsReader.parseParameters(paramMap, lines);
			ParamsReader.parseParameters(parameters, paramMap);
		}

		// Included to generate JSON text of parameters as needed
		// ObjectMapper mapper = new ObjectMapper();
		// String serializedParametersText = mapper.writeValueAsString(parameters);

		try (
				InputStream polyStream = MainTest.class
						.getResourceAsStream("f-record-test-01-noback/input/VDYP7_INPUT_POLY.csv");
				InputStream layerStream = MainTest.class
						.getResourceAsStream("f-record-test-01-noback/input/VDYP7_INPUT_LAYER.csv");
		) {

			InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
					.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
					.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, "VDYP7_INPUT_POLY.csv", polyStream) //
					.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, "VDYP7_INPUT_LAYER.csv", layerStream) //
					.post("/projection/hcsv?trialRun=false") //
					.then().statusCode(201) //
					.and().contentType("application/octet-stream") //
					.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
					.extract().body().asInputStream();

			ZipInputStream zipFile = new ZipInputStream(zipInputStream);
			ZipEntry entry1 = zipFile.getNextEntry();
			assertEquals("YieldTable.csv", entry1.getName());
			String vdyp8YieldTableContent = new String(testHelper.readZipEntry(zipFile, entry1));
			assertThat(vdyp8YieldTableContent, not(emptyString()));

			var vdyp8YieldTable = new ResultYieldTable(vdyp8YieldTableContent);

			assertThat(
					vdyp8YieldTable,
					(Matcher) hasSpecificEntry(
							"1816115",
							hasSpecificEntry(
									"1",
									hasSpecificEntry(
											"2103",
											Matchers.allOf(
													hasSpecificEntry(
															"PRJ_DIAMETER",
															VdypMatchers
																	.parseAs(closeTo(33.32f, 0.02f), ValueParser.FLOAT)
													),
													hasSpecificEntry(
															"PRJ_TPH",
															VdypMatchers
																	.parseAs(closeTo(397.13f, 0.02f), ValueParser.FLOAT)
													)
											)
									)
							)
					)
			);
		}
	}

	@Test
	void test604() throws IOException, ResourceParseException {

		logger.info("Starting test604");

		Map<String, List<String>> paramMap = new HashMap<>();
		var parameters = new Parameters();
		try (
				InputStream paramStream = MainTest.class.getResourceAsStream("vdyp-604/input/parms.txt");
				InputStreamReader reader = new InputStreamReader(paramStream);
				BufferedReader bufReader = new BufferedReader(reader); var lines = bufReader.lines();
		) {
			ParamsReader.parseParameters(paramMap, lines);
			ParamsReader.parseParameters(parameters, paramMap);
		}

		// Included to generate JSON text of parameters as needed
		// ObjectMapper mapper = new ObjectMapper();
		// String serializedParametersText = mapper.writeValueAsString(parameters);

		try (
				InputStream polyStream = MainTest.class.getResourceAsStream("vdyp-604/input/VDYP7_INPUT_POLY.csv");
				InputStream layerStream = MainTest.class.getResourceAsStream("vdyp-604/input/VDYP7_INPUT_LAYER.csv");
		) {

			InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
					.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
					.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, "VDYP7_INPUT_POLY.csv", polyStream) //
					.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, "VDYP7_INPUT_LAYER.csv", layerStream) //
					.post("/projection/hcsv?trialRun=false") //
					.then().statusCode(201) //
					.and().contentType("application/octet-stream") //
					.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
					.extract().body().asInputStream();

			ZipInputStream zipFile = new ZipInputStream(zipInputStream);
			ZipEntry entry1 = zipFile.getNextEntry();
			assertEquals("YieldTable.csv", entry1.getName());
			String vdyp8YieldTableContent = new String(testHelper.readZipEntry(zipFile, entry1));
			assertThat(vdyp8YieldTableContent, not(emptyString()));

			var vdyp8YieldTable = new ResultYieldTable(vdyp8YieldTableContent);

			assertThat(
					vdyp8YieldTable,
					(Matcher) hasSpecificEntry(
							"17585871",
							hasSpecificEntry(
									"1",
									hasSpecificEntry(
											"2103",
											Matchers.allOf(
													hasSpecificEntry(
															"PRJ_BA",
															VdypMatchers.parseAs(
																	closeTo(28.392712f, 0.02f), ValueParser.FLOAT
															)
													)
											)
									)
							)
					)
			);
		}
	}
}
