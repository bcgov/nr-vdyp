package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

/*
 * An exception preventing a particular stand from being processed, but which should not affect other stands
 */
public abstract class StandProcessingException extends ProcessingException {

	private static final long serialVersionUID = -3844954593240011442L;

	protected StandProcessingException() {
		super();
	}

	protected StandProcessingException(
			String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace
	) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	protected StandProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	protected StandProcessingException(String message) {
		super(message);
	}

	protected StandProcessingException(Throwable cause) {
		super(cause);
	}

	/**
	 * Returns the IPASS error code that would be returned in VDYP7. These codes are application specific.
	 */
	public abstract Optional<Integer> getIpassCode(VdypApplicationIdentifier app);

	protected static <T extends StandProcessingException> T
			unwrap(RuntimeStandProcessingException cause, Class<T> klazz) {
		var unwrapped = cause.getCause();
		if (!klazz.isInstance(unwrapped)) {
			final IllegalArgumentException ex = new IllegalArgumentException(
					"Could not unwrap RuntimeStandProcessingException to " + klazz.getCanonicalName(), cause
			);
			ex.addSuppressed(cause);
			throw ex;
		}
		return klazz.cast(unwrapped);
	}
}
