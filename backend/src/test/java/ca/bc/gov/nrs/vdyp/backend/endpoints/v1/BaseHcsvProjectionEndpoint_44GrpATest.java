package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
import io.smallrye.common.constraint.Assert;
import jakarta.ws.rs.core.MediaType;

class BaseHcsvProjectionEndpoint_44GrpATest extends BaseHttpProjectionRequestTest {

	private static final Logger logger = LoggerFactory.getLogger(BaseHcsvProjectionEndpoint_44GrpATest.class);

	private static Path resourceFolderPath;
	protected static Parameters parameters;

	@BeforeAll
	static void setup() {
		resourceFolderPath = Path.of(FileHelper.TEST_DATA_FILES, FileHelper.HCSV, "44grpa");

		parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED
		);

		parameters.yearStart(2000).yearEnd(2050);

		// Included to generate JSON text of parameters as needed
//		ObjectMapper mapper = new ObjectMapper();
//		String serializedParametersText = mapper.writeValueAsString(parameters);
	}

	@BeforeEach
	void perTestSetup() {
	}

	protected Map<String, String> runTest(String featureId) throws IOException {
		var zipEntriesMap = runTestSet(fId -> fId.equals(featureId), Optional.empty());

		Assert.assertTrue(zipEntriesMap.size() == 1 && zipEntriesMap.containsKey(featureId));
		return zipEntriesMap.get(featureId);
	}

	protected Map<String, Map<String, String>>
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
