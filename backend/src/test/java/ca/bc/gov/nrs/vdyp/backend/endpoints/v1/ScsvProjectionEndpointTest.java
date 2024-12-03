package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
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
				.multiPart(ParameterNames.POLYGON_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.LAYERS_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.HISTORY_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.NON_VEGETATION_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.OTHER_VEGETATION_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.POLYGON_ID_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.SPECIES_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.multiPart(ParameterNames.VRI_ADJUST_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.post("/projection/scsv").then().statusCode(501);
	}
}
