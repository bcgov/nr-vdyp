package ca.bc.gov.nrs.vdyp.backend.exceptions;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/** An actionable maintenance precondition, not an unexpected backend failure. */
public class OrphanFileCleanupException extends ClientErrorException {

	private final String code;

	public OrphanFileCleanupException(String code, String message) {
		super(message, Response.Status.CONFLICT);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
