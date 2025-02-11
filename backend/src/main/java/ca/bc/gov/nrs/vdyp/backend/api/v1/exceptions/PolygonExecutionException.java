package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.io.IOException;

public class PolygonExecutionException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = 663894496709845053L;

	public PolygonExecutionException(PolygonValidationException pve) {
		super(pve);
	}

	public PolygonExecutionException(IOException e) {
		super(e);
	}
}
