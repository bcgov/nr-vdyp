package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

	// @Test - uncomment and change runTest param to the polygon id from 44grpa that you want
	// to run.
	void run44GroupA_SpecificPolygon_Test() throws IOException {
		logger.info("Starting run44GroupA_SpecificPolygon_Test");

		var zipEntries = runTest("19976958");

		var errorEntryContent = zipEntries.get("ErrorLog.txt");
		Assert.assertTrue(errorEntryContent.length() == 0);
		var csvEntryContent = zipEntries.get("YieldTable.csv");
		Assert.assertTrue(csvEntryContent.length() > 0);
	}

	// @Test - uncomment if you want to run the entire 44grpa polygon set
	void run44GroupATest() throws IOException {

		logger.info("Starting run44GroupATest");

		Path testFolderPath = testHelper.getResourceFile(resourceFolderPath, "44GrpA_VDYP7_INPUT_POLY.csv").getParent();
		Path statusFilePath = Path.of(testFolderPath.toString(), "RunStatus.txt");

		Files.deleteIfExists(statusFilePath);
		Path statusFile = Files.createFile(statusFilePath);

		var statusStream = new FileOutputStream(statusFile.toFile());

		var resultsList = runTestSet(fId -> true /* do all */, Optional.of(statusStream));

		logger.info("run44GroupATest complete: {} results seen", resultsList);
	}

	// @Test
	// Uncomment if you want to run a subset of the 44grpa polygon set given in the file "polyids.txt"
	// (placed alongside the polygon and layer files) that contains one polygon id per line.
	void runFiltered44GroupATest() throws IOException {

		logger.info("Starting runFiltered44GroupATest");

		Path testFolderPath = testHelper.getResourceFile(resourceFolderPath, "44GrpA_VDYP7_INPUT_POLY.csv").getParent();
		Path statusFilePath = Path.of(testFolderPath.toString(), "RunStatus.txt");

		Path polyIdFilePath = testHelper.getResourceFile(resourceFolderPath, "polyids.txt");

		Predicate<String> predicate;
		if (Files.exists(polyIdFilePath)) {
			var polyIdSet = new HashSet<String>();
			for (var line : Files.readAllLines(polyIdFilePath)) {
				polyIdSet.add(line.strip());
			}
			predicate = fId -> polyIdSet.contains(fId);
		} else {
			predicate = fId -> true /* do all */;
		}

		Files.deleteIfExists(statusFilePath);
		Path statusFile = Files.createFile(statusFilePath);

		var statusStream = new FileOutputStream(statusFile.toFile());

		var resultsList = runTestSet(predicate, Optional.of(statusStream));

		logger.info("run44GroupATest complete: {} results seen", resultsList);
	}

	Map<String, String> runTest(String featureId) throws IOException {
		var zipEntriesMap = runTestSet(fId -> fId.equals(featureId), Optional.empty());

		Assert.assertTrue(zipEntriesMap.size() == 1 && zipEntriesMap.containsKey(featureId));
		return zipEntriesMap.get(featureId);
	}

	private Map<String, Map<String, String>>
			runTestSet(Predicate<String> featureSelector, Optional<FileOutputStream> statusStream) throws IOException {

		Map<String /* feature id */, Map<String /* file name */, String /* content */>> resultsByFeatureId = new HashMap<>();

		var overallCsvFilePath = testHelper.getResourceFile(resourceFolderPath, "44GrpA_VDYP7_INPUT_POLY.csv");
		List<String> lines = Files.readAllLines(overallCsvFilePath);

		String headerLine = lines.get(0);
		for (var featureLine : lines.subList(1, lines.size())) {

			var featureId = getFeatureId(featureLine);

			if (featureSelector.test(featureId)) {

				var fileContents = headerLine + "\n" + featureLine;

				Path inputPolygonFilePath = Files.createTempFile(featureId, ".csv");
				inputPolygonFilePath = Files.write(inputPolygonFilePath, fileContents.getBytes());

				var resultMap = runTestFile(inputPolygonFilePath.toFile());

				statusStream.ifPresent(os -> {
					var errorEntryContent = resultMap.get("ErrorLog.txt");
					if (errorEntryContent.length() > 0) {
						try {
							String message = String.format("%10s: %s", featureId, errorEntryContent);
							if (!message.endsWith("\n")) {
								message += "\n";
							}
							os.write(message.getBytes());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}

					var csvEntryContent = resultMap.get("YieldTable.csv");
					if (csvEntryContent.length() > 0) {
						try {
							String message = String.format("%10s: success\n", featureId);
							os.write(message.getBytes());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}

				});

				resultsByFeatureId.put(featureId, resultMap);
			}
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
