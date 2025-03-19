package ca.bc.gov.nrs.vdyp.exceptions;

public class FatalProcessingException extends ProcessingException {

	private static final long serialVersionUID = -4051659652585072913L;

	public FatalProcessingException(String message, int errorNumber) {
		super(message, errorNumber);
	}

	public FatalProcessingException(
			String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace
	) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FatalProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public FatalProcessingException(String message) {
		super(message);
	}

	public FatalProcessingException(Throwable cause) {
		super(cause);
	}

}
