package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

public class ProjectionRequestValidationException extends Exception {

	private static final long serialVersionUID = 5172661648677695483L;

	private final List<String> validationMessages;

	public ProjectionRequestValidationException(List<String> validationMessages) {
		this.validationMessages = validationMessages;
	}
	
	public List<String> getValidationMessages() {
		return validationMessages;
	}
}
