package ca.bc.gov.nrs.vdyp.backend.v1.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ParameterDetailsMessage;

public class ParameterDetailsMessageBuilder {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ParameterDetailsMessageBuilder.class);

	public static ParameterDetailsMessage build(
			String field, String shortDescription, String parameterValue, String longDescription, String defaultValue
	) {
		ParameterDetailsMessage m = new ParameterDetailsMessage();

		m.setDefaultValue(defaultValue);
		m.setField(field);
		m.setLongDescription(longDescription);
		m.setParameterValue(parameterValue);
		m.setShortDescription(shortDescription);

		// logger.info("built ParameterDetailsMessage {}", shortDescription);

		return m;
	}
}
