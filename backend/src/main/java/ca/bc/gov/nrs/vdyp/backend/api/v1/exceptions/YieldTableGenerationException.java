package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

/**
 * These exceptions are to be used when unrecoverable issues happen during yield table generation - that is, it is not
 * possible to resume the generation of the table.
 */
public class YieldTableGenerationException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = -4342933248690426666L;

	public YieldTableGenerationException(Exception cause) {
		super(cause);
	}

	public YieldTableGenerationException(String message, Exception cause) {
		super(message, cause);
	}

	public YieldTableGenerationException(String message) {
		super(message);
	}
}
