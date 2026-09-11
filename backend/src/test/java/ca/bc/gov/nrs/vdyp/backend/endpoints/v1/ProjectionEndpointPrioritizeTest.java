package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class ProjectionEndpointPrioritizeTest {

	@Test
	@TestSecurity(user = "non-admin-test-user", roles = { "USER" })
	void prioritizeProjection_nonAdminUser_returnsForbidden() {
		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/{projectionGUID}/prioritize", UUID.randomUUID())
				.then().statusCode(403);
	}

	@Test
	void prioritizeProjection_unauthenticatedUser_isRejected() {
		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/{projectionGUID}/prioritize", UUID.randomUUID())
				.then().statusCode(anyOf(is(401), is(403)));
	}
}
