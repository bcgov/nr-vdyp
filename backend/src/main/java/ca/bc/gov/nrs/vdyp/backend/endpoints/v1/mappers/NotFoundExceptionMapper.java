package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper extends AbstractApiExceptionMapper<NotFoundException> {

	@Override
	protected void log(NotFoundException exception) {
		logger.debug("Resource not found: {}", exception.getMessage());
	}

	@Override
	protected Response buildResponse(NotFoundException e) {
		return response(Response.Status.NOT_FOUND, "NOT_FOUND", "Resource not found");
	}
}
