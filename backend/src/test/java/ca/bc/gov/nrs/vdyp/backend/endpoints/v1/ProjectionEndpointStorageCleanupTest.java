package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class ProjectionEndpointStorageCleanupTest {

	@InjectMock
	@RestClient
	VDYPBatchClient batchClient;

	@Test
	@TestSecurity(user = "non-admin-test-user", roles = { "USER" })
	void cleanupStorage_nonAdminUser_returnsForbidden() {
		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/storage-cleanup").then().statusCode(403);
	}

	@Test
	@TestSecurity(user = "admin-test-user", roles = { "ADMIN" })
	void cleanupStorage_adminUser_batchServiceFails_returnsBadGateway() {
		when(batchClient.cleanupStorage(any())).thenThrow(new RuntimeException("simulated batch service failure"));

		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/storage-cleanup").then().statusCode(502)
				.body("code", is("STORAGE_CLEANUP_FAILED"));
	}

	@Test
	@TestSecurity(user = "admin-test-user", roles = { "ADMIN" })
	void cleanupStorage_adminUser_previewSucceeds_returnsReportFromBatchService() {
		when(batchClient.cleanupStorage(any())).thenReturn(
				new StorageCleanupReportModel(
						true, 1, 100L,
						List.of(
								new CleanupSetResultModel(
										"11111111-1111-1111-1111-111111111111", "vdyp-batch-11111111-1111-1111-1111-111111111111",
										100L, CleanupOutcomeModel.DELETABLE, null
								)
						), List.of()
				)
		);

		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/storage-cleanup").then().statusCode(200)
				.body("dryRun", is(true)).body("scanned", is(1)).body("totalBytes", is(100));
	}

	@Test
	void cleanupStorage_unauthenticatedUser_isRejected() {
		given().basePath(TestHelper.ROOT_PATH).when().post("/projection/storage-cleanup").then()
				.statusCode(anyOf(is(401), is(403)));
	}
}
