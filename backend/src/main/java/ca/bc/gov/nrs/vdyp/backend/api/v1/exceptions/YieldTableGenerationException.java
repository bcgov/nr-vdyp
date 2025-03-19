package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

public class YieldTableGenerationException extends ProjectionRequestException {

	private static final long serialVersionUID = -4342933248690426666L;

	public YieldTableGenerationException(Exception e) {
		super(e);
	}

	public YieldTableGenerationException(String cause, Exception e) {
		super(cause, e);
	}

	public YieldTableGenerationException(String format) {
		super(format);
	}

}
