package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;

/**
 * Parses a Coefficient data file.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class CoefficientParser implements ControlMapSubResourceParser<MatrixMap2<String, String, Coefficients>> {

	public static final String BA_CONTROL_KEY = "COE_BA"; // V7COE040/COE040
	public static final String DQ_CONTROL_KEY = "COE_DQ";

	public static final String BEC_KEY = "bec";
	public static final String COEFFICIENT_INDEX_KEY = "index";
	public static final String INDICATOR_KEY = "indicator";
	public static final String COEFFICIENT_KEY = "coefficient";

	public static final int NUM_COEFFICIENTS = 10;
	public static final int NUM_SPECIES = 16;

	final String controlKey;

	public CoefficientParser(String controlKey) {
		this.controlKey = controlKey;
	}

	LineParser lineParser = new LineParser() {

		@Override
		public boolean isStopLine(String line) {
			return line.startsWith("   ");
		}

	}.value(4, BEC_KEY, ValueParser.STRING).space(2).value(1, COEFFICIENT_INDEX_KEY, ValueParser.INTEGER)
			.value(2, INDICATOR_KEY, ValueParser.INTEGER)
			.multiValue(NUM_SPECIES, 8, COEFFICIENT_KEY, ValueParser.FLOAT);

	@Override
	public MatrixMap2<String, String, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {
		var becAliases = BecDefinitionParser.getBecAliases(control);
		var speciesIndecies = GenusDefinitionParser.getSpeciesAliases(control);
		MatrixMap2<String, String, Coefficients> result = new MatrixMap2Impl<>(
				becAliases, speciesIndecies, (k1, k2) -> Coefficients.empty(NUM_COEFFICIENTS, 0)
		);
		IntFunction<String> speciesLookup = index -> GenusDefinitionParser.getSpeciesByIndex(index, control).getAlias();

		lineParser.parse(is, result, (v, r) -> {
			var bec = (String) v.get(BEC_KEY);
			var indicator = (int) v.get(INDICATOR_KEY);
			var index = (int) v.get(COEFFICIENT_INDEX_KEY);
			@SuppressWarnings("unchecked")
			var coefficients = (List<Float>) v.get(COEFFICIENT_KEY);

			if (!becAliases.contains(bec)) {
				throw new ValueParseException(bec, bec + " is not a valid BEC alias");
			}
			if (index < 0 || index >= NUM_COEFFICIENTS) {
				throw new ValueParseException(Integer.toString(index), index + " is not a valid coefficient index");
			}

			for (int species = 0; species < speciesIndecies.size(); species++) {
				float c;
				switch (indicator) {
				case 0:
				default:
					c = coefficients.get(0);
					break;
				case 1:
					c = coefficients.get(species);
					break;
				case 2:
					if (species == 0)
						c = coefficients.get(0);
					else
						c = coefficients.get(0) + coefficients.get(species);
					break;
				}
				r.get(bec, speciesLookup.apply(species + 1)).setCoe(index, c);
			}
			return r;
		}, control);
		return result;
	}

	@Override
	public String getControlKey() {
		return controlKey;
	}

}
