package ca.bc.gov.nrs.vdyp.batch.exception;

// MDJ: Why a RuntimeException? Is this ALWAYS unrecoverable? 
public class BatchDataValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BatchDataValidationException(String message) {
		super(message);
	}

	public BatchDataValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
