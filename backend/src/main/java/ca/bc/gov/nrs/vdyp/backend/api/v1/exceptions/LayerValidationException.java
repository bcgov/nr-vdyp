package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

public class LayerValidationException extends ProjectionRequestException {

	private static final long serialVersionUID = 2651505762328626871L;

	public LayerValidationException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
	}
}
