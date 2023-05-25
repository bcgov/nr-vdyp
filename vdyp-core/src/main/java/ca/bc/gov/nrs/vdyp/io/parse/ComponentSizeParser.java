package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parse a datafile with species component size limits
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class ComponentSizeParser extends BaseCoefficientParser<Coefficients, MatrixMap2<String, Region, Coefficients>> {

	public static final String CONTROL_KEY = "SPECIES_COMPONENT_SIZE_LIMIT";

	public ComponentSizeParser(Map<String, Object> control) {
		super();
		this.speciesKey(control).space(1).regionKey().coefficients(4, 6);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected MatrixMap2<String, Region, Coefficients> createMap(List<Collection<?>> keyRanges) {
		return new MatrixMap2Impl<String, Region, Coefficients>(
				(Collection<String>) keyRanges.get(0), (Collection<Region>) keyRanges.get(1)
		);
	}

	@Override
	protected Coefficients getCoefficients(List<Float> coefficients) {
		return new Coefficients(coefficients, 1);
	}

}
