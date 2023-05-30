package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

@FunctionalInterface
public interface ControlledValueParser<T> {

	/**
	 * Parse a string to a value usinga control map for context. Should not attempt
	 * to modify the map.
	 *
	 * @param s
	 * @param control
	 * @return
	 * @throws ValueParseException
	 */
	T parse(String s, Map<String, Object> control) throws ValueParseException;

}
