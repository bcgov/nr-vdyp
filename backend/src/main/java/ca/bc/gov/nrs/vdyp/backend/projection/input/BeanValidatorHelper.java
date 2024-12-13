package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;

public class BeanValidatorHelper {

	private List<ValidationMessage> validationMessages = new ArrayList<ValidationMessage>();
	private String entityIdentifier;
	
	BeanValidatorHelper(String entityIdentifier) {
		this.entityIdentifier = entityIdentifier;
	}
	
	protected List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}
	
	protected <T extends Comparable<T>> void validateRange(String numberText, Consumer<String> validator, Function<String, T> getValue, T low, T high, String fieldName) {
		if (numberText != null) {
			validateNumber(numberText, validator, fieldName);
			T value = getValue.apply(numberText);
			if (value.compareTo(low) < 0 || value.compareTo(high) > 0) {
				addValidationMessage(ValidationMessageKind.NUMBER_OUT_OF_RANGE, fieldName, numberText, low, high);
			}
		}
	}

	protected <T> void validateNumber(String numberText, Consumer<String> validator, String fieldName) {
		if (numberText != null) {
			try {
				validator.accept(numberText);
			} catch (NumberFormatException e) {
				addValidationMessage(ValidationMessageKind.NOT_A_NUMBER, fieldName, numberText);
			}
		}
	}

	protected <T> void validateEnumeration(String code, Consumer<String> validator, String fieldName) {
		if (code != null) {
			try {
				validator.accept(code);
			} catch (IllegalArgumentException e) {
				addValidationMessage(ValidationMessageKind.INVALID_CODE, fieldName, code);
			}
		}
	}

	protected void addValidationMessage(ValidationMessageKind kind, Object... args) {
		if (args.length == 0) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier));
		} else if (args.length == 1) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0]));
		} else if (args.length == 2) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1]));
		} else if (args.length == 3) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2]));
		} else if (args.length == 4) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2], args[3]));
		} else if (args.length == 5) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2], args[3], args[4]));
		} else if (args.length == 6) {
			validationMessages.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2], args[3], args[4], args[5]));
		}
	}
}
