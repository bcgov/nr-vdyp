package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

public class AbstractProjectionRequestException extends Exception {

	private static final long serialVersionUID = -3349545755843821804L;
	private final List<ValidationMessage> validationMessages;

	public AbstractProjectionRequestException(List<ValidationMessage> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}
}
