package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

public class ProjectionRequestValidationException extends Exception {

	private static final long serialVersionUID = 5172661648677695483L;

	private final List<ValidationMessage> validationMessages;

	public ProjectionRequestValidationException(List<ValidationMessage> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}
}
