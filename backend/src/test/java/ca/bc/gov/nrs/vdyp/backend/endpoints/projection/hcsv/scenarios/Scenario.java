package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import jakarta.ws.rs.core.MediaType;

public class Scenario {

	protected static final Logger logger = LoggerFactory.getLogger(Scenario.class);

	protected final TestHelper testHelper;
	protected final Path scenarioResourcePath;

	protected Scenario(TestHelper testHelper, String scenarioName) {
		this.testHelper = testHelper;
		this.scenarioResourcePath = Path.of(FileHelper.TEST_DATA_FILES, "hcsv", "scenarios", scenarioName);
	}

	protected ResultYieldTable assertYieldTableNext(ZipInputStream zipFile, Predicate<String> predicate)
			throws IOException {

		String content = assertLogEntry(zipFile, "YieldTable.csv", predicate);
		return new ResultYieldTable(content);
	}

	protected void assertProgressLogNext(ZipInputStream zipFile, Predicate<String> predicate) throws IOException {
		assertLogEntry(zipFile, "ProgressLog.txt", predicate);
	}

	protected void assertErrorLogNext(ZipInputStream zipFile, Predicate<String> predicate) throws IOException {
		assertLogEntry(zipFile, "ErrorLog.txt", predicate);
	}

	protected void assertDebugLogNext(ZipInputStream zipFile, Predicate<String> predicate) throws IOException {
		assertLogEntry(zipFile, "DebugLog.txt", predicate);
	}

	private String assertLogEntry(ZipInputStream zipFile, String entryName, Predicate<String> predicate)
			throws IOException {

		ZipEntry entry = zipFile.getNextEntry();
		assertEquals(entryName, entry.getName());

		String entryContent = new String(testHelper.readZipEntry(zipFile, entry));

		if (predicate != null) {
			assertTrue(predicate.test(entryContent));
		}

		return entryContent;
	}

	protected InputStream
			runExpectedSuccessfulRequest(String polygonFileName, String layerFileName, Parameters parameters) {

		return given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						testHelper.getResourceFile(scenarioResourcePath, polygonFileName).toFile()
				) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						testHelper.getResourceFile(scenarioResourcePath, layerFileName).toFile()
				) //
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();
	}

	/** @return an IntStream with range (i ... j + 1); i.e., including both i and j. */
	protected IntStream allOfRange(int i, int j) {
		return IntStream.range(i, j + 1);
	}

	protected void assertHasAgeRange(
			ResultYieldTable resultYieldTable, String polygonId, String layerId, Object... yearSpecs
	) {

		var assertFailureSeen = false;

		var polygonLayersTable = resultYieldTable.get(polygonId);
		if (polygonLayersTable == null) {
			polygonLayersTable = new HashMap<String, Map<String, Map<String, String>>>();
		}

		var yearsTable = polygonLayersTable.get(layerId);
		if (yearsTable == null) {
			yearsTable = new HashMap<String, Map<String, String>>();
		}

		var yearsInTable = new HashSet<String>(yearsTable.keySet());

		for (Object yearSpec : yearSpecs) {
			if (yearSpec instanceof Integer year) {
				var yearText = year.toString();
				if (!yearsInTable.contains(yearText)) {
					logger.error("Yield table does not contain entry for year " + year);
					assertFailureSeen = true;
				} else {
					yearsInTable.remove(yearText);
				}
			} else if (yearSpec instanceof IntStream years) {
				for (String yearText : years.boxed().map(y -> y.toString()).toList()) {
					var key = Integer.valueOf(yearText).toString();
					if (!yearsInTable.contains(key)) {
						logger.error("Yield table does not contain entry for year " + yearText);
						assertFailureSeen = true;
					} else {
						yearsInTable.remove(yearText);
					}
				}
			} else {
				throw new IllegalArgumentException("yearSpecs must be Integers or IntStreams");
			}
		}

		if (!yearsInTable.isEmpty()) {
			for (var year : yearsInTable.stream().sorted().toList()) {
				logger.error("Range does not contain entry for year " + year);
			}
			Assert.fail();
		} else if (assertFailureSeen) {
			Assert.fail();
		}
	}
}
