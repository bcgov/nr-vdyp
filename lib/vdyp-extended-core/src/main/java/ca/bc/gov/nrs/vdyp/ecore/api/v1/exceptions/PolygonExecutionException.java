package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

/**
 * Exceptions thrown during the execution of the projection of a single polygon. These exceptions are meant to be caught
 * and handled at a point that allows projection (of the next polygon) to continue.
 */
public class PolygonExecutionException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = 663894496709845053L;

	public PolygonExecutionException(PolygonValidationException pve) {
		super(pve.getValidationMessages());
	}

	public PolygonExecutionException(String message, PolygonValidationException pve) {
		super(message, pve.getValidationMessages());
	}

	public PolygonExecutionException(Throwable cause) {
		super(cause);
	}

	public PolygonExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PolygonExecutionException(String message) {
		super(message);
	}
}
