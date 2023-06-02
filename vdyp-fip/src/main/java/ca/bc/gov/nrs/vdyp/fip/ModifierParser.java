package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.io.parse.ResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;

public class ModifierParser implements ResourceControlMapModifier {

	public static final String CONTROL_KEY = "MODIFIERS";

	public static final int MAX_MODS = 60;

	@Override
	public void modify(Map<String, Object> control, InputStream data) throws ResourceParseException, IOException {
		// TODO

	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

}
