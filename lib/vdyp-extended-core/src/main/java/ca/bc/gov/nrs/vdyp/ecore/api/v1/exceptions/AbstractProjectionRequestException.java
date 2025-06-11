package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

public abstract class AbstractProjectionRequestException extends Exception {

	private static final long serialVersionUID = -3349545755843821804L;
	private final List<ValidationMessage> validationMessages;

	public AbstractProjectionRequestException(List<ValidationMessage> validationMessages) {
		super(buildMessage(validationMessages));
		this.validationMessages = validationMessages;
	}

	public AbstractProjectionRequestException(String message, List<ValidationMessage> validationMessages) {
		super( (message != null ? (message + ": ") : "") + buildMessage(validationMessages));
		this.validationMessages = validationMessages;
	}

	public AbstractProjectionRequestException(Throwable cause) {
		super(cause);
		validationMessages = new ArrayList<ValidationMessage>();
		if (cause.getMessage() != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, cause.getMessage()));
		}
	}

	public AbstractProjectionRequestException(String message, Throwable e) {
		super(message, e);
		validationMessages = new ArrayList<ValidationMessage>();
		if (message != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, message));
		}
	}

	public AbstractProjectionRequestException(String message) {
		super(message);
		validationMessages = new ArrayList<ValidationMessage>();
		if (message != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, message));
		}
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}

	private static String buildMessage(List<ValidationMessage> validationMessages) {
		if (validationMessages.size() > 0) {
			StringBuffer sb = new StringBuffer(validationMessages.get(0).toString());
			for (int i = 1; i < validationMessages.size(); i++) {
				sb.append("; ").append(validationMessages.get(i).getMessage());
			}
			return sb.toString();
		} else {
			return "";
		}
	}
}
