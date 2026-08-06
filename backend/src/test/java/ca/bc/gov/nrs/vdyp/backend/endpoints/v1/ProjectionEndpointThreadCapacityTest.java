package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class ProjectionEndpointThreadCapacityTest {

	@Test
	@TestSecurity(user = "non-admin-test-user", roles = { "USER" })
	void getThreadCapacity_nonAdminUser_returnsForbidden() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/thread-capacity").then().statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin-test-user", roles = { "ADMIN" })
	void getThreadCapacity_adminUser_returnsOkWithThreadCapacityField() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/thread-capacity").then().statusCode(200)
				.body("threadCapacity", is(notNullValue()));
	}

	@Test
	void getThreadCapacity_unauthenticatedUser_isRejected() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/thread-capacity").then()
				.statusCode(anyOf(is(401), is(403)));
	}
}
