package ca.bc.gov.nrs.vdyp.backend.integrationtests;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import jakarta.ws.rs.core.MediaType;

/** Exercises logging through the packaged application rather than Quarkus's in-process test loader. */
@QuarkusIntegrationTest
class ITProjectionDebugLogging {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void packagedProjectionIncludesDebugLogOnlyWhenRequested(boolean debugEnabled) throws Exception {
		Parameters parameters = new Parameters().yearStart(2013).yearEnd(2014)
				.addSelectedExecutionOptionsItem(ExecutionOption.FORWARD_GROW_ENABLED)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER);
		if (debugEnabled) {
			parameters.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING);
		}
		Path polygons = Path.of(getClass().getResource("/test-data/hcsv/single-polygon/VDYP7_INPUT_POLY.csv").toURI());
		Path layers = Path.of(getClass().getResource("/test-data/hcsv/single-polygon/VDYP7_INPUT_LAYER.csv").toURI());
		try (
				var response = given().basePath(TestHelper.ROOT_PATH)
						.header("X-Consumer-Username", "integration-test-client")
						.header(TestHelper.GATEWAY_JWT_HEADER, TestHelper.GATEWAY_JWT)
						.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON)
						.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygons.toFile())
						.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, layers.toFile())
						.post("/projection/hcsv?trialRun=false").then().log().ifValidationFails().statusCode(201)
						.extract().body().asInputStream();
				ZipInputStream zip = new ZipInputStream(response)
		) {
			boolean foundDebug = false;
			boolean foundYieldTable = false;
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if ("DebugLog.txt".equals(entry.getName())) {
					foundDebug = true;
					String log = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
					assertTrue(log.contains("DEBUG"));
					assertTrue(log.contains("ca.bc.gov.nrs.vdyp.ecore"));
				}
				foundYieldTable |= "YieldTable.csv".equals(entry.getName());
			}
			assertTrue(foundYieldTable);
			assertEquals(debugEnabled, foundDebug);
		}
	}
}
