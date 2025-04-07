package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

/**
 * Exceptions thrown during the calculation of stand yields during the creation of yield tables. These exceptions are
 * meant to indicate that while the yield table information for a given polygon may be incomplete, generation should
 * continue.
 */
public class StandYieldCalculationException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = -4342933248690426666L;

	private final int errorCode;

	public StandYieldCalculationException(int errorCode, Exception cause) {
		super("StandYieldCalculation exception " + errorCode, cause);
		this.errorCode = errorCode;
	}

	public StandYieldCalculationException(int errorCode) {
		super("StandYieldCalculation exception " + errorCode);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
