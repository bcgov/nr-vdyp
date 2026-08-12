package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProjectionFileUploadExceptionMapper extends AbstractApiExceptionMapper<ProjectionFileUploadException> {

	@Override
	protected Response buildResponse(ProjectionFileUploadException e) {
		return response(e.getStatus(), e.getCode(), e.getMessage());
	}

	@Override
	protected void log(ProjectionFileUploadException e) {
		logger.info(e.getMessage());
	}
}
