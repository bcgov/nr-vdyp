package ca.bc.gov.nrs.vdyp.io.parse.coe;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class DebugSettingsParser implements ValueParser<DebugSettings> {

	private static final Logger logger = LoggerFactory.getLogger(DebugSettingsParser.class);

	@Override
	public DebugSettings parse(String string) throws ValueParseException {

		if (string == null) {
			throw new ValueParseException(null, "VdypDebugSettingsParser: supplied string is null");
		}

		string = string.trim();

		// Ensure correct alignment to chop the string into 2 character sections.
		if (string.length() % 2 == 1) {
			string = " " + string;
		}

		List<Integer> debugSettingsValues = new ArrayList<>();

		while (string.length() > 0) {

			String settingText = string.substring(0, 2).trim();

			try {
				debugSettingsValues.add(Integer.parseInt(settingText));
			} catch (NumberFormatException e) {
				throw new ValueParseException("Debug setting value \"" + settingText + "\" is not an integer");
			}

			string = string.substring(2);
		}

		if (debugSettingsValues.size() < DebugSettings.MAX_DEBUG_SETTINGS) {
			logger.warn(
					"Only " + debugSettingsValues.size() + " debug values were supplied rather than the "
							+ DebugSettings.MAX_DEBUG_SETTINGS + " expected"
			);
		}

		return new DebugSettings(debugSettingsValues.stream().toArray(Integer[]::new));
	}
}
