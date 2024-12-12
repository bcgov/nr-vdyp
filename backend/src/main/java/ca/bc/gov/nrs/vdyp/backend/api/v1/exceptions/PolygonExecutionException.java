package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

public class PolygonExecutionException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = 663894496709845053L;

	public PolygonExecutionException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
	}
}
