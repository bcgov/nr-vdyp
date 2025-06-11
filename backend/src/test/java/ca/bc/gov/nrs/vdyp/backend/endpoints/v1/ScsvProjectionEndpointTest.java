package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ScsvProjectionEndpointTest {

	private final TestHelper testHelper;

	@Inject
	ScsvProjectionEndpointTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionSscv_shouldThrow() throws IOException {

		given().basePath(TestHelper.ROOT_PATH).when().multiPart(ParameterNames.PROJECTION_PARAMETERS, new Parameters()) //
				.multiPart(ParameterNames.SCSV_POLYGON_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_LAYERS_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_HISTORY_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_POLYGON_ID_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_SPECIES_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.post("/projection/scsv").then().statusCode(501);
	}
}
