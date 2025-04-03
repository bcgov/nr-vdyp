package ca.bc.gov.nrs.vdyp.backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ParameterDetailsMessage;

public class ParameterDetailsMessageBuilder {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ParameterDetailsMessageBuilder.class);

	public static ParameterDetailsMessage build(
			String field, String shortDescription, String parameterValue, String longDescription, String defaultValue
	) {
		var mb = new ParameterDetailsMessage.Builder();

		mb.defaultValue(defaultValue).field(field).longDescription(longDescription).parameterValue(parameterValue)
				.shortDescription(shortDescription);

		return mb.build();
	}

	public static ParameterDetailsMessage
			build(String field, String shortDescription, String longDescription, String defaultValue) {
		return build(field, shortDescription, null, longDescription, defaultValue);
	}
}
