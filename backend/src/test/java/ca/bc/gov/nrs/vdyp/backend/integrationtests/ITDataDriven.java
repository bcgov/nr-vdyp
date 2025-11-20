package ca.bc.gov.nrs.vdyp.backend.integrationtests;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
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

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected static Path outputDir;

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
	void testEndToEnd(String testName) throws IOException, ResourceParseException {

		Path testDir = testDataDir.resolve(testName);
		Path dataDir = testDir.resolve("input");
		Path expectedDir = testDir.resolve("output");

		Assumptions.assumeTrue(Files.exists(dataDir), "No input data");
		Assumptions.assumeTrue(Files.exists(expectedDir), "No expected output data");

		doSkip(testDir, "testEndToEnd");

		Map<String, List<String>> paramMap = new HashMap<>();
		var parameters = new Parameters();
		try (var reader = Files.newBufferedReader(dataDir.resolve(PARAMETERS_FILE)); var lines = reader.lines()) {
			ParamsReader.parseParameters(paramMap, lines);
			ParamsReader.parseParameters(parameters, paramMap);
		}

		var polyFile = lastItem(paramMap.get("ip")).replaceAll(VARIABLE_PATTERN, "");
		var layerFile = lastItem(paramMap.get("il")).replaceAll(VARIABLE_PATTERN, "");
		var yieldFile = lastItem(paramMap.get("o")).replaceAll(VARIABLE_PATTERN, "");

		logger.atInfo().setMessage("Using poly file {} and layer file {} in {}").addArgument(polyFile)
				.addArgument(layerFile).addArgument(dataDir).log();
		try (InputStream zipInputStream = runExpectedSuccessfulRequest(dataDir, polyFile, layerFile, parameters);) {
			ZipInputStream zipFile = new ZipInputStream(zipInputStream);

			logger.atInfo().setMessage("Extracting ZIP file to {}").addArgument(outputDir).log();
			// Extract the zip file and store all its contents in outputDir
			while (true) {
				var entry = zipFile.getNextEntry();
				if (entry == null) {
					break;
				}
				logger.atInfo().setMessage("Extracting {} from zip file").addArgument(entry.getName()).log();
				try (var out = Files.newOutputStream(outputDir.resolve(entry.getName()));) {
					while (zipFile.available() > 0) {
						byte[] buffer = new byte[1024];
						int bytesRead;
						while ( (bytesRead = zipFile.read(buffer, 0, 1024)) > 0) {
							out.write(buffer, 0, bytesRead);
						}
					}
				}
			}
			logger.atInfo().setMessage("ZIP file extracted").log();
		}

		ResultYieldTable actualYieldTable;
		ResultYieldTable expectedYieldTable;
		try (var reader = Files.newBufferedReader(outputDir.resolve("YieldTable.csv"))) {
			actualYieldTable = new ResultYieldTable(reader);
		}
		try (var reader = Files.newBufferedReader(expectedDir.resolve(yieldFile))) {
			expectedYieldTable = new ResultYieldTable(reader);
		}

		ResultYieldTable.compareWithTolerance(expectedYieldTable, actualYieldTable, 0.01, IGNORE_COLUMNS);
	}

	/**
	 * Rudimentary regexp combiner that creates a pattern that matches if either provided regexp matches. Doesn't
	 * account for regexp options.
	 *
	 * @param p1
	 * @param p2
	 * @return
	 */
	static Pattern eitherRegexp(Pattern... patterns) {
		if (patterns.length == 0)
			throw new IllegalArgumentException("Must have at least one pattern");
		if (patterns.length == 1)
			return patterns[0];
		return Pattern
				.compile(Arrays.stream(patterns).map(p -> "(?:" + p.toString() + ")").collect(Collectors.joining("|")));
	}

	// FIXME Workaround for VDYP-804
	static final Pattern BASE_804_AFFECTED = Pattern.compile("PRJ_SCND_HT");

	static final Predicate<String> IGNORE_COLUMNS = eitherRegexp(BASE_804_AFFECTED).asMatchPredicate();

}
