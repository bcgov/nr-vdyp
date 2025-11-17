package ca.bc.gov.nrs.vdyp.batch.exception;

public class BatchConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BatchConfigurationException(String message) {
		super(message);
	}

	public BatchConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
