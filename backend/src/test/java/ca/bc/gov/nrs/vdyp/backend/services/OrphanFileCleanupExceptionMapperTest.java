package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ApiError;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.OrphanFileCleanupExceptionMapper;
import ca.bc.gov.nrs.vdyp.backend.exceptions.OrphanFileCleanupException;

class OrphanFileCleanupExceptionMapperTest {

	@Test
	void preconditionRetainsConflictStatusAndActionableCode() {
		var exception = new OrphanFileCleanupException("PREVIEW_REQUIRED", "Preview this run first");
		try (var response = new OrphanFileCleanupExceptionMapper().toResponse(exception)) {
			assertEquals(409, response.getStatus());
			assertEquals(new ApiError("PREVIEW_REQUIRED", "Preview this run first"), response.getEntity());
		}
	}
}
