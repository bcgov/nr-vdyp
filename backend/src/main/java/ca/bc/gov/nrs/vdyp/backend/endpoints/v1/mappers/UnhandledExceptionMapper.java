package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnhandledExceptionMapper extends AbstractApiExceptionMapper<Throwable> {

	@Override
	protected Response buildResponse(Throwable e) {
		return response(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
	}
}
