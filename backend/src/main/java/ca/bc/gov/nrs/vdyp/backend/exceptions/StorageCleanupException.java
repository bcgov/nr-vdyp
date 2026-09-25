package ca.bc.gov.nrs.vdyp.backend.exceptions;

import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;

/** The batch service's PVC storage cleanup could not be completed; nothing was deleted. */
public class StorageCleanupException extends ServerErrorException {

	public StorageCleanupException(String message, Throwable cause) {
		super(message, Response.Status.BAD_GATEWAY, cause);
	}
}
