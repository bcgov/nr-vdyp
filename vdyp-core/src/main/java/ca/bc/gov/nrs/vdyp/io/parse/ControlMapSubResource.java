package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import ca.bc.gov.nrs.vdyp.io.FileResolver;

/**
 * A resource that can be loaded as a sub-resource of a control file.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 * @param <T>
 */
public interface ControlMapSubResource<T> extends ResourceControlMapModifier, ResourceParser<T> {

	/**
	 * The key for this resource's entry in the control map
	 *
	 * @return
	 */
	String getControlKey();

	@Override
	default void modify(Map<String, Object> control, InputStream data) throws IOException, ResourceParseException {
		var result = this.parse(data, control);

		control.put(getControlKey(), result);
	}

	/**
	 * Replace the entry in the control map containing the filename for a resource
	 * with the parsed resource
	 *
	 * @param control
	 * @param fileResolver
	 * @throws IOException
	 * @throws ResourceParseException
	 */
	default void modify(Map<String, Object> control, FileResolver fileResolver)
			throws IOException, ResourceParseException {
		var filename = (String) control.get(getControlKey());
		try (InputStream data = fileResolver.resolve(filename)) {
			modify(control, data);
		}
	}
}
