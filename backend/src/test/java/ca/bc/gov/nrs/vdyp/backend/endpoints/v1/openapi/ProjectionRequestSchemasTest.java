package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.DcsvProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.HcsvProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ProjectionConfigurationRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ProjectionFileUploadRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ScsvProjectionRequest;

class ProjectionRequestSchemasTest {

	@Test
	void testRequestSchemasCanBeCreated() {
		assertAll(
				() -> assertNotNull(new DcsvProjectionRequest()), () -> assertNotNull(new HcsvProjectionRequest()),
				() -> assertNotNull(new ScsvProjectionRequest()),
				() -> assertNotNull(new ProjectionConfigurationRequest()),
				() -> assertNotNull(new ProjectionFileUploadRequest())
		);
	}
}
