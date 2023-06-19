package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.io.parse.OptionalResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;

public class ModifierParser implements OptionalResourceControlMapModifier {

	public static final String CONTROL_KEY = "MODIFIERS";

	public static final int MAX_MODS = 60;

	@Override
	public void modify(Map<String, Object> control, InputStream data) throws ResourceParseException, IOException {
		// Modifiers, IPSJF155-Appendix XII

		// RD_E198

	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

	@Override
	public void defaultModify(Map<String, Object> control) {
		// Do nothing
	}

}
