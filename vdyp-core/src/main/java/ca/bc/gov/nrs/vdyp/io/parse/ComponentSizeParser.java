package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parse a datafile with species component size limits
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class ComponentSizeParser extends SimpleCoefficientParser2<String, Region> {

	public static final String CONTROL_KEY = "SPECIES_COMPONENT_SIZE_LIMIT";

	public ComponentSizeParser(Map<String, Object> control) {
		super(1);
		this.speciesKey().space(1).regionKey().coefficients(4, 6);
	}

}
