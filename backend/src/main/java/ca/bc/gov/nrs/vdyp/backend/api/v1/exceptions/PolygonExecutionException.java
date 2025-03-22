package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

/**
 * Exceptions thrown during the execution of the projection of a single polygon. These exceptions are meant to be caught
 * and handled at a point that allows projection (of the next polygon) to continue.
 */
public class PolygonExecutionException extends AbstractPolygonProjectionException {

	private static final long serialVersionUID = 663894496709845053L;

	public PolygonExecutionException(PolygonValidationException pve) {
		super(pve);
	}

	public PolygonExecutionException(String cause, Exception e) {
		super(cause, e);
	}
}
