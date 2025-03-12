package ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions;

public class ProjectionRequestValidationException extends Exception {

	private static final long serialVersionUID = 5172661648677695483L;

	public ProjectionRequestValidationException(Exception cause) {
		super(cause);
	}

	public ProjectionRequestValidationException(String reason) {
		super(reason);
	}

	public ProjectionRequestValidationException(Exception cause, String reason) {
		super(reason, cause);
	}
}
