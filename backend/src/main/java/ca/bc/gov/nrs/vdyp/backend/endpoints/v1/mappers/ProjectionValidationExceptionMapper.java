package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProjectionValidationExceptionMapper extends AbstractApiExceptionMapper<ProjectionValidationException> {

	@Override
	protected Response buildResponse(ProjectionValidationException e) {
		return response(Response.Status.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
	}

	@Override
	protected void log(ProjectionValidationException e) {
		logger.info(e.getMessage());
	}
}
