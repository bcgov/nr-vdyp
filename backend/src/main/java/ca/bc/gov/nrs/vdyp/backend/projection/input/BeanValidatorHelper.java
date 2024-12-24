package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>numberText</code>:
	 * <ul>
	 * <li>fails <code>validator()</code>,
	 * <li>cannot be converted to a number by <code>getValue()</code>, or
	 * <li>is not between <code>low</code> and <code>high</code>, inclusive.
	 * </ul>
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>numberText</code> is in the given range or is null
	 */
	protected <T extends Comparable<T>> boolean validateRange(
			String numberText, Consumer<String> validator, Function<String, T> getValue, T low, T high, String fieldName
	) {
		if (numberText != null) {
			validateNumber(numberText, validator, fieldName);
			T value = getValue.apply(numberText);
			if (value.compareTo(low) < 0 || value.compareTo(high) > 0) {
				addValidationMessage(ValidationMessageKind.NUMBER_OUT_OF_RANGE, fieldName, numberText, low, high);
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>d</code> is not between <code>low</code>
	 * and <code>high</code>, inclusive. 
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>d</code> is in the given range or is null
	 */
	protected boolean validateRange(Double d, double low, double high, String fieldName) {
		if (d != null) {
			if (d < low || d > high) {
				addValidationMessage(ValidationMessageKind.NUMBER_OUT_OF_RANGE, fieldName, d, low, high);
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>d</code>: is not between <code>low</code>
	 * and <code>high</code>, inclusive.
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>d</code> is in the given range or is null
	 */
	protected boolean validateRange(Short d, short low, short high, String fieldName) {
		if (d != null) {
			if (d < low || d > high) {
				addValidationMessage(ValidationMessageKind.NUMBER_OUT_OF_RANGE, fieldName, d, low, high);
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>d</code>: is not between <code>low</code>
	 * and <code>high</code>, inclusive.
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>d</code> is in the given range or is null
	 */
	protected boolean validateRange(Integer d, int low, int high, String fieldName) {
		if (d != null) {
			if (d < low || d > high) {
				addValidationMessage(ValidationMessageKind.NUMBER_OUT_OF_RANGE, fieldName, d, low, high);
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>numberText</code>:
	 * <ul>
	 * <li>fails <code>validator()</code>,
	 * <li>cannot be converted to a number by <code>getValue()</code>, or
	 * <li>is not between <code>low</code> and <code>high</code>, inclusive.
	 * </ul>
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>numberText</code> passes the given validation test or is null
	 */
	protected <T> boolean validateNumber(String numberText, Consumer<String> validator, String fieldName) {
		if (numberText != null) {
			try {
				validator.accept(numberText);
			} catch (NumberFormatException e) {
				addValidationMessage(ValidationMessageKind.NOT_A_NUMBER, fieldName, numberText);
				return false;
			}
		}
		return true;
	}

	/**
	 * Add a validation-failed message against <code>fieldName</code> if <code>code</code>:
	 * <ul>
	 * <li>fails <code>validator()</code>,
	 * <li>cannot be converted to a number by <code>getValue()</code>, or
	 * <li>is not between <code>low</code> and <code>high</code>, inclusive.
	 * </ul>
	 * <p>
	 * <code>null</code> values are silently <b>accepted</b>.
	 * @return true iff <code>code</code> passes the validation test or is null
	 */
	protected <T> boolean validateEnumeration(String code, Consumer<String> validator, String fieldName) {
		if (code != null) {
			try {
				validator.accept(code);
			} catch (IllegalArgumentException e) {
				addValidationMessage(ValidationMessageKind.INVALID_CODE, fieldName, code);
			}
			return false;
		}
		return true;
	}

	/**
	 * Add a validation message of kind <code>kind</code> with args <code>args</code> to the 
	 * validation messages being recorded by this helper instance.
	 * @param kind
	 * @param args
	 */
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
			validationMessages
					.add(new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2], args[3], args[4]));
		} else if (args.length == 6) {
			validationMessages.add(
					new ValidationMessage(kind, entityIdentifier, args[0], args[1], args[2], args[3], args[4], args[5])
			);
		}
	}

	/**
	 * If <code>s</code> is not <code>null</code> and is longer than <code>maxLenLayerLevelCode</code>, return
	 * the result of truncating <code>s</code> to the given length. Otherwise, return <code>s</code>.
	 * @param s
	 * @param maxStringLength
	 * @return as described
	 */
	public String truncateString(String s, int maxStringLength) {
		if (s != null && s.length() > maxStringLength) {
			return s.substring(0, maxStringLength);
		} else {
			return s;
		}
	}

	/**
	 * Round the given number, expressed as a text string, to the given precision. Trailing 0s
	 * in the fractional part of the resulting number are removed.
	 * <p>
	 * It is not an error if the original text cannot be parsed as a number. In this case, it 
	 * is the original text is returned.
	 * @param doubleText the text of the number to be manipulated.
	 * @param maximumPrecision the maximum precision the result is to have.
	 * @return a String representation of the resulting number
	 */
	public static String round(String doubleText, int maximumPrecision) {
		
		if (doubleText != null) {
			try {
				BigDecimal bd = new BigDecimal(doubleText);
				bd = bd.setScale(maximumPrecision, RoundingMode.HALF_UP);
				doubleText = bd.toString();
			} catch (NumberFormatException e) {
				// do nothing; return the original text.
			}
		} 
		
		return doubleText;
	}

	/**
	 * Round the given Double to the given precision.
	 * @param d the number to be manipulated.
	 * @param maximumPrecision the maximum precision the result is to have.
	 * @return the resulting number
	 */
	public static Double round(Double d, int maximumPrecision) {
		
		if (d != null) {
			BigDecimal bd = new BigDecimal(d.toString());
			bd = bd.setScale(maximumPrecision, RoundingMode.HALF_UP);
			d = bd.doubleValue();
		} 
		
		return d;
	}
}
