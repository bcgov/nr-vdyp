/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 * The version of the OpenAPI document: 1.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import java.text.MessageFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identifies how often or when progress will be reported from the application. In the case of a number being supplied,
 * the number indicates the number of polygons to be processed between indications of progress.
 */
@JsonPropertyOrder({ ProgressFrequency.JSON_PROPERTY_INT_VALUE_NAME, ProgressFrequency.JSON_PROPERTY_ENUM_VALUE_NAME })
public class ProgressFrequency {

	public static final ProgressFrequency MAPSHEET = new ProgressFrequency(FrequencyKind.MAPSHEET);
	public static final ProgressFrequency POLYGON = new ProgressFrequency(FrequencyKind.POLYGON);
	public static final ProgressFrequency NEVER = new ProgressFrequency(FrequencyKind.NEVER);

	public static final String JSON_PROPERTY_INT_VALUE_NAME = "intValue";
	@JsonProperty(JSON_PROPERTY_INT_VALUE_NAME)
	private Integer intValue;

	public static final String JSON_PROPERTY_ENUM_VALUE_NAME = "enumValue";
	@JsonProperty(JSON_PROPERTY_ENUM_VALUE_NAME)
	private FrequencyKind enumValue;

	/**
	 * Gets or Sets value
	 */
	public enum FrequencyKind {
		NEVER("never"), POLYGON("polygon"), MAPSHEET("mapsheet");

		private String value;

		FrequencyKind(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the text corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static FrequencyKind fromValue(String value) {
			for (FrequencyKind b : FrequencyKind.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public ProgressFrequency() {
		this.intValue = null;
		this.enumValue = null;
	}

	public ProgressFrequency(FrequencyKind enumValue) {
		setEnumValue(enumValue);
	}

	public ProgressFrequency(int intValue) {
		setIntValue(intValue);
	}

	public ProgressFrequency(String text) {
		text = text.trim();
		try {
			setEnumValue(FrequencyKind.fromValue(text));
		} catch (IllegalArgumentException iae) {
			try {
				setIntValue(Integer.parseInt(text));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						MessageFormat.format("\"{0}\" is not a valid ProgressFrequency value", text)
				);
			}
		}
	}

	public ProgressFrequency intValue(int intValue) {
		setIntValue(intValue);
		return this;
	}

	public void setIntValue(Integer intValue) {
		this.enumValue = null;
		this.intValue = intValue;
	}

	@JsonProperty(JSON_PROPERTY_INT_VALUE_NAME)
	public Integer getIntValue() {
		return intValue;
	}

	public ProgressFrequency enumValue(FrequencyKind enumValue) {
		setEnumValue(enumValue);
		return this;
	}

	public void setEnumValue(FrequencyKind enumValue) {
		this.intValue = null;
		this.enumValue = enumValue;
	}

	@JsonProperty(JSON_PROPERTY_ENUM_VALUE_NAME)
	public FrequencyKind getEnumValue() {
		return enumValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ProgressFrequency other = (ProgressFrequency) o;

		return other.intValue == this.intValue && other.enumValue == this.enumValue;
	}

	@Override
	public int hashCode() {
		return intValue != null ? intValue.hashCode() : enumValue != null ? enumValue.hashCode() : 17;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (intValue != null)
			sb.append("Every ").append(intValue).append(" polygons");
		else if (enumValue != null)
			sb.append(enumValue.getValue());
		return sb.toString();
	}

	public ProgressFrequency copy() {
		if (enumValue != null) {
			return new ProgressFrequency(enumValue);
		} else {
			return new ProgressFrequency(intValue);
		}
	}
}
