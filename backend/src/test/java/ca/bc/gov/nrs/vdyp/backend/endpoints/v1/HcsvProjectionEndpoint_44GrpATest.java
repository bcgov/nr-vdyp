package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class HcsvProjectionEndpoint_44GrpATest extends HttpProjectionRequestTest {

	private static final Logger logger = LoggerFactory.getLogger(HcsvProjectionEndpoint_44GrpATest.class);

	private static Path resourceFolderPath;
	private static Parameters parameters;

	@BeforeAll
	static void setup() {
		resourceFolderPath = Path.of(FileHelper.TEST_DATA_FILES, FileHelper.HCSV, "44grpa");

		parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES
		);

		parameters.yearStart(2000).yearEnd(2050);

		// Included to generate JSON text of parameters as needed
//		ObjectMapper mapper = new ObjectMapper();
//		String serializedParametersText = mapper.writeValueAsString(parameters);
	}

	@BeforeEach
	void perTestSetup() {
	}

	@Test
	void run44GroupA_20929412_Test() throws IOException {
		logger.info("Starting run44GroupA_20929412_Test");

		var zipEntries = runTest("20929412");

		var errorEntryContent = zipEntries.get("ErrorLog.txt");
		Assert.assertTrue(errorEntryContent.length() == 0);
		var csvEntryContent = zipEntries.get("YieldTable.csv");
		Assert.assertTrue(csvEntryContent.length() > 0);
	}

	@Test
	void run44GroupATest() throws IOException {

		logger.info("Starting run44GroupATest");

		var resultsList = runTestSet(fId -> true /* do all */);
		for (var resultEntry: resultsList.entrySet()) {
			var errorEntryContent = resultEntry.getValue().get("ErrorLog.txt");
			if (errorEntryContent.length() > 0) {
				logger.error("{}: {}", resultEntry.getKey(), errorEntryContent);
			}
		}
	}
	
	Map<String, String> runTest(String featureId) throws IOException {
		var zipEntriesMap = runTestSet(fId -> fId.equals(featureId));
		
		Assert.assertTrue(zipEntriesMap.size() == 1 && zipEntriesMap.containsKey(featureId));
		return zipEntriesMap.get(featureId);
	}

	private Map<String, Map<String, String>> runTestSet(Predicate<String> featureSelector) throws IOException {

		var overallCsvFilePath = testHelper.getResourceFile(resourceFolderPath, "44GrpA_VDYP7_INPUT_POLY.csv");
		List<String> lines = Files.readAllLines(overallCsvFilePath);

		String headerLine = lines.get(0);

		Map<String, String> features = new HashMap<>();

		List<String> selectedLines = lines.stream().filter(l -> featureSelector.test(getFeatureId(l))).toList();
		for (var line: selectedLines) {
			features.put(getFeatureId(line), headerLine + '\n' + line);
		}

		Map<String /* feature id */, Map<String /* file name */, String /* content */>> resultsByFeatureId = new HashMap<>();

		for (var feature : features.entrySet()) {
			Path csvFilePath = Files.createTempFile(feature.getKey(), ".csv");

			var csvFile = Files.write(csvFilePath, feature.getValue().getBytes());

			resultsByFeatureId.put(feature.getKey(), runTestFile(csvFile.toFile()));
		}

		return resultsByFeatureId;
	}
	
	private String getFeatureId(String line) {
		String s = line.substring(0, line.indexOf(','));
		return s;
	}

	private Map<String, String> runTestFile(File polyFile) throws IOException {

		logger.info("Running projection of " + polyFile);
		
		InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, polyFile) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						testHelper.getResourceFile(resourceFolderPath, "44GrpA_VDYP7_INPUT_LAYER.csv").toFile()
				) //
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();

		return parseZipResults(zipInputStream);
	}
}
