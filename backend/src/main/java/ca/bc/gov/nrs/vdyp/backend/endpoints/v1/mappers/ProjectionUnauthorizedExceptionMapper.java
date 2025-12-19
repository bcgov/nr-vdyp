package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProjectionUnauthorizedExceptionMapper extends AbstractApiExceptionMapper<ProjectionUnauthorizedException> {

	@Override
	protected Response buildResponse(ProjectionUnauthorizedException e) {
		return response(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
	}
}
