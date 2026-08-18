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
class ProjectionEndpointStorageStatusTest {

	@Test
	@TestSecurity(user = "non-admin-test-user", roles = { "USER" })
	void getStorageStatus_nonAdminUser_returnsForbidden() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/storage-status").then().statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin-test-user", roles = { "ADMIN" })
	void getStorageStatus_adminUser_returnsOkWithStorageStatusFields() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/storage-status").then().statusCode(200)
				.body("percentFull", is(notNullValue())).body("usedBytes", is(notNullValue()))
				.body("totalBytes", is(notNullValue())).body("expectedBytes", is(notNullValue()))
				.body("outOfSpec", is(notNullValue())).body("thresholdPercent", is(notNullValue()));
	}

	@Test
	void getStorageStatus_unauthenticatedUser_isRejected() {
		given().basePath(TestHelper.ROOT_PATH).when().get("/projection/storage-status").then()
				.statusCode(anyOf(is(401), is(403)));
	}
}
