package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;

public abstract class ProjectionRequestException extends Exception {

	private static final long serialVersionUID = -3349545755843821804L;
	private final List<ValidationMessage> validationMessages;

	public ProjectionRequestException(List<ValidationMessage> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public ProjectionRequestException(List<ValidationMessage> validationMessages, Exception cause) {
		super(cause);
		this.validationMessages = validationMessages;
	}

	public ProjectionRequestException(Exception cause) {
		super(cause);
		validationMessages = new ArrayList<ValidationMessage>();
		if (cause.getMessage() != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, cause.getMessage()));
		}
	}

	public ProjectionRequestException(String cause, Exception e) {
		super(cause, e);
		validationMessages = new ArrayList<ValidationMessage>();
		if (cause != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, cause));
		}
	}

	public ProjectionRequestException(String cause) {
		super(cause);
		validationMessages = new ArrayList<ValidationMessage>();
		if (cause != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, cause));
		}
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}
}
