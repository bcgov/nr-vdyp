package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.common.HoldFirst;
import ca.bc.gov.nrs.vdyp.model.Coefficients;

/**
 * Parses an mapping from a species to a set of coefficients.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class BySpeciesDqCoefficientParser implements ControlMapSubResourceParser<Map<String, Coefficients>> {

	public static final String CONTROL_KEY = "BY_SPECIES_DQ";

	public static final String COEFFICIENT_INDEX_KEY = "coefficientIndex";
	public static final String COEFFICIENTS_KEY = "coefficients";
	public static final String INDICATOR_KEY = "indicator";

	private int NUM_COEFFICIENTS = 3;
	private int NUM_SPECIES = 16;

	public BySpeciesDqCoefficientParser() {
		super();
		this.lineParser = new LineParser() {

			@Override
			public boolean isStopLine(String line) {
				return line.startsWith("   ");
			}

		}.space(1).value(1, COEFFICIENT_INDEX_KEY, ValueParser.INTEGER).value(2, INDICATOR_KEY, ValueParser.INTEGER)
				.multiValue(NUM_SPECIES, 9, COEFFICIENTS_KEY, ValueParser.FLOAT);
	}

	LineParser lineParser;

	@Override
	public Map<String, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		Map<String, Coefficients> result = new HashMap<>();

		var sp0Aliases = GenusDefinitionParser.getSpeciesAliases(control);

		lineParser.parse(is, result, (v, r) -> {
			var index = (int) v.get(COEFFICIENT_INDEX_KEY);
			@SuppressWarnings("unchecked")
			var specCoefficients = (List<Float>) v.get(COEFFICIENTS_KEY);

			var specIt = sp0Aliases.iterator();
			var coeIt = specCoefficients.iterator();
			HoldFirst<Float> firstSpecCoe = new HoldFirst<>();
			while (specIt.hasNext()) {
				var specAlias = specIt.next();
				float coe;
				if (! (index == 0 || index == 1)) {
					coe = firstSpecCoe.get(() -> coeIt.next());
				} else {
					try {
						coe = coeIt.next();
					} catch (NoSuchElementException ex) {
						throw new ValueParseException(null, "Expected " + NUM_SPECIES + " coefficients", ex);
					}
				}
				result.computeIfAbsent(specAlias, (x) -> Coefficients.empty(NUM_COEFFICIENTS, 0)).setCoe(index, coe);
			}

			return r;
		}, control);
		return result;

	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

}