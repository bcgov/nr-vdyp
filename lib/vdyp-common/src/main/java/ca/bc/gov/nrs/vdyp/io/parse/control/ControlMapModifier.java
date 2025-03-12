package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.io.IOException;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

public interface ControlMapModifier {

	/**
	 *
	 * @param control
	 * @param fileResolverContext The file resolver to use to resolve file paths per key.
	 * @throws ResourceParseException
	 * @throws IOException
	 */
	void modify(Map<String, Object> control, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException;

	/**
	 * @return the name of the key for this resource's entry in the control map
	 */
	default String getControlKeyName() {
		return getControlKey().name();
	}

	/**
	 * @return the key for this resource's entry in the control map
	 */
	ControlKey getControlKey();

	/**
	 * @return value parser for the control value before modification
	 */
	ValueParser<Object> getValueParser();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ValueParser<Object> FILENAME = (ValueParser) ValueParser.FILENAME;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ValueParser<Object> OPTIONAL_FILENAME = (ValueParser) ValueParser.optional(FILENAME);
}
