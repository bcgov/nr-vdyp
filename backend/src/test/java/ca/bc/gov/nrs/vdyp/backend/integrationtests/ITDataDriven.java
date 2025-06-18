package ca.bc.gov.nrs.vdyp.backend.integrationtests;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.integration_tests.BaseDataBasedIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class ITDataDriven extends BaseDataBasedIntegrationTest {

	@Test
	void testGetHelp_shouldReturnStatusOK() {

		given().basePath(TestHelper.ROOT_PATH).when().get("/help").then().statusCode(200).and()
				.contentType("application/json").and()
				.body(Matchers.containsString("outputFormat"), Matchers.containsString("Output Data Format"));
	}

}
