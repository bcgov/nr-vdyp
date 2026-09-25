package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StorageCleanupExceptionMapper extends AbstractApiExceptionMapper<StorageCleanupException> {

	@Override
	protected Response buildResponse(StorageCleanupException e) {
		return response(Response.Status.BAD_GATEWAY, "STORAGE_CLEANUP_FAILED", e.getMessage());
	}
}
