package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Modifies the control map based on a resource
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public interface ResourceControlMapModifier extends ControlMapModifier {

	void modify(Map<String, Object> control, InputStream data) throws ResourceParseException, IOException;

}
