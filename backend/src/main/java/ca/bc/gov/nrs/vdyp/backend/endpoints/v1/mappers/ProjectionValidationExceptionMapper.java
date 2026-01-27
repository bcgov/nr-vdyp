package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProjectionValidationExceptionMapper extends AbstractApiExceptionMapper<ProjectionStateException> {

	@Override
	protected Response buildResponse(ProjectionStateException e) {
		return response(Response.Status.CONFLICT, "BAD_REQUEST", e.getMessage());
	}
}
