package ca.bc.gov.nrs.vdyp.backend.integrationtests;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.io.read.ParamsReader;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.integration_tests.BaseDataBasedIntegrationTest;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import jakarta.ws.rs.core.MediaType;

@QuarkusIntegrationTest
class ITDataDriven extends BaseDataBasedIntegrationTest {

	protected static final Logger logger = LoggerFactory.getLogger(ITDataDriven.class);

	static final String PARAMETERS_FILE = "parms.txt";

	static final String VARIABLE_PATTERN = "\\$\\((\\w+)\\)[\\\\/]?"; // Matches / after the variable to make paths
																		// relative

	protected InputStream runExpectedSuccessfulRequest(
			Path dataDir, String polygonFileName, String layerFileName, Parameters parameters
	) {

		return given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, dataDir.resolve(polygonFileName).toFile()) //
				.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, dataDir.resolve(layerFileName).toFile()) //
																											// TODO
																											// select
																											// endpoint
																											// based on
																											// input
																											// format
																											// specified
																											// in
																											// parameters
																											// file
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();
	}

	static <T> T lastItem(List<T> list) {
		return list.get(list.size() - 1);
	}

	@ParameterizedTest
	@MethodSource("testNameProvider")
	void test(String testName) throws IOException, ResourceParseException {

		Path testDir = testDataDir.resolve(testName);
		Path dataDir = testDir.resolve("input");
		Path expectedDir = testDir.resolve("output");

		Assumptions.assumeTrue(Files.exists(dataDir), "No input data");
		Assumptions.assumeTrue(Files.exists(expectedDir), "No expected output data");

		doSkip(testDir, "testFipStart");

		Map<String, List<String>> paramMap = new HashMap<>();
		var parameters = new Parameters();
		try (var reader = Files.newBufferedReader(dataDir.resolve(PARAMETERS_FILE)); var lines = reader.lines()) {
			ParamsReader.parseParameters(paramMap, lines);
			ParamsReader.parseParameters(parameters, paramMap);
		}

		var polyFile = lastItem(paramMap.get("ip")).replaceAll(VARIABLE_PATTERN, "");
		var layerFile = lastItem(paramMap.get("il")).replaceAll(VARIABLE_PATTERN, "");

		logger.atInfo().setMessage("Using poly file {} and layer file {} in {}").addArgument(polyFile)
				.addArgument(layerFile).addArgument(dataDir).log();
		try (InputStream zipInputStream = runExpectedSuccessfulRequest(dataDir, polyFile, layerFile, parameters);) {

		}

	}
}
