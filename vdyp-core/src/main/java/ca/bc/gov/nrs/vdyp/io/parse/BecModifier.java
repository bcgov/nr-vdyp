package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.io.FileResolver;

public class BecModifier implements ControlMapModifier {

	@Override
	public void modify(Map<String, Object> control, FileResolver fileResolver)
			throws ResourceParseException, IOException {
		var becMap = BecDefinitionParser.getBecs(control);

	}

}
