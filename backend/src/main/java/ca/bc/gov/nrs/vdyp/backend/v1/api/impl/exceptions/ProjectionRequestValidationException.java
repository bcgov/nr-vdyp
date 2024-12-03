package ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions;

import java.util.ArrayList;
import java.util.List;

public class ProjectionRequestValidationException extends Exception {

	private static final long serialVersionUID = 5172661648677695483L;

	private final List<String> validationErrorMessages;

	public ProjectionRequestValidationException(Exception cause) {
		super(cause);

		validationErrorMessages = new ArrayList<>();
		validationErrorMessages.add(Exceptions.getMessage(cause));
	}

	public ProjectionRequestValidationException(Exception cause, String reason) {
		super(reason, cause);

		validationErrorMessages = new ArrayList<>();
		validationErrorMessages.add(reason);
	}

	public ProjectionRequestValidationException(List<String> validationErrorMessages) {
		this.validationErrorMessages = validationErrorMessages;
	}
}
