package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.OrphanFileCleanupException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrphanFileCleanupExceptionMapper extends AbstractApiExceptionMapper<OrphanFileCleanupException> {

	@Override
	protected Response buildResponse(OrphanFileCleanupException e) {
		return response(Response.Status.CONFLICT, e.getCode(), e.getMessage());
	}

	@Override
	protected void log(OrphanFileCleanupException e) {
		logger.info("COMS reconciliation blocked: {}: {}", e.getCode(), e.getMessage());
	}
}
