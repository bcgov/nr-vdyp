package ca.bc.gov.nrs.vdyp.batch.exception;

// FIXME VDYP-839
public class BatchDataValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BatchDataValidationException(String message) {
		super(message);
	}

	public BatchDataValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
