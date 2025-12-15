package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProjectionNotFoundExceptionMapper extends AbstractApiExceptionMapper<ProjectionNotFoundException> {

	@Override
	protected Response buildResponse(ProjectionNotFoundException e) {
		return response(Response.Status.NOT_FOUND, "NOT_FOUND", e.getMessage());
	}
}
