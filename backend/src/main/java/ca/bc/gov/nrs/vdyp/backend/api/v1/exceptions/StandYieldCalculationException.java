package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

public class StandYieldCalculationException extends ProjectionRequestException {

	private static final long serialVersionUID = -4342933248690426666L;

	private final int errorCode;

	public StandYieldCalculationException(int errorCode, Exception e) {
		super("StandYieldCalculation exception " + errorCode, e);
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
