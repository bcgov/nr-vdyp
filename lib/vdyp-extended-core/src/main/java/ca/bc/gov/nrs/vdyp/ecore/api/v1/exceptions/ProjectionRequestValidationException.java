package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;

public class ProjectionRequestValidationException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = -5559129890898878919L;

	public ProjectionRequestValidationException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
	}
}
