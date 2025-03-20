package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.List;

import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;

/**
 * Exceptions thrown during the validation of a Layer read from an input CSV. These exceptions are used only to wrap the
 * list of validation errors and get passed into a {@link CsvConstraintViolationException}
 */
public class LayerValidationException extends ProjectionRequestException {

	private static final long serialVersionUID = 2651505762328626871L;

	public LayerValidationException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
	}
}
