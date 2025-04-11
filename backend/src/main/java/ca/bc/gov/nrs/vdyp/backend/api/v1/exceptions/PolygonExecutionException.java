package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

public class PolygonExecutionException extends ProjectionRequestException {

	private static final long serialVersionUID = 663894496709845053L;

	public PolygonExecutionException(PolygonValidationException pve) {
		super(pve);
	}

	public PolygonExecutionException(String cause, Exception e) {
		super(cause, e);
	}
}
