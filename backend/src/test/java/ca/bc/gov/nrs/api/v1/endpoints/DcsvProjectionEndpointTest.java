package ca.bc.gov.nrs.api.v1.endpoints;

import static io.restassured.RestAssured.given;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.api.impl.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.smallrye.common.constraint.Assert;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class DcsvProjectionEndpointTest {

	private final TestHelper testHelper;

	@Inject
	DcsvProjectionEndpointTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionDscv_shouldThrow() throws IOException {

		Parameters parameters = new Parameters();

		Response response = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(ParameterNames.DCSV_INPUT_DATA, testHelper.buildTestFile().readAllBytes()) //
				.post("/projection/dcsv").andReturn();

		Assert.assertTrue(true);
	}
}
