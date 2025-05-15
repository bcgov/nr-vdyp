package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.io.IOException;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

public class OutputFileLocationResolver implements ControlMapValueReplacer<String, String> {

	private final ControlKey controlKey;

	public OutputFileLocationResolver(ControlKey controlKey) {
		this.controlKey = controlKey;
	}

	@Override
	public ControlKey getControlKey() {
		return controlKey;
	}

	@Override
	public ValueParser<Object> getValueParser() {
		return String::strip;
	}

	@Override
	public String map(String rawValue, FileResolver fileResolver, Map<String, Object> control)
			throws ResourceParseException, IOException {

		return fileResolver.toPath(rawValue).toString();
	}

}
