package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

/**
 * A problem occurred while VDYP was processing data
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class ProcessingException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Optional<Integer> errorNumber;

	public ProcessingException() {
		super();
		this.errorNumber = Optional.empty();
	}

	public ProcessingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.errorNumber = Optional.empty();
	}

	public ProcessingException(String message, int errorNumber) {
		super(MessageFormat.format("{0} ({1})", message, errorNumber));
		this.errorNumber = Optional.of(errorNumber);
	}

	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
		this.errorNumber = Optional.empty();
	}

	public ProcessingException(String message) {
		super(message);
		this.errorNumber = Optional.empty();
	}

	public ProcessingException(Throwable cause) {
		super(cause);
		this.errorNumber = Optional.empty();
	}

	public Optional<Integer> getErrorNumber() {
		return errorNumber;
	}
}
