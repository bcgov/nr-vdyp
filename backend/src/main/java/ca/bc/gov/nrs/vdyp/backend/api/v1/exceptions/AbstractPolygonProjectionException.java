package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

public abstract class AbstractPolygonProjectionException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = 1014280499592713124L;

	public AbstractPolygonProjectionException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
	}

	public AbstractPolygonProjectionException(PolygonValidationException pve) {
		super(pve);
	}

	public AbstractPolygonProjectionException(String cause, Exception e) {
		super(cause, e);
	}
}
