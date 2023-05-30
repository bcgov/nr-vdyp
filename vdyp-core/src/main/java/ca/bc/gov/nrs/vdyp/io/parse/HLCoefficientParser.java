package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parses an HL Coefficient data file.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class HLCoefficientParser implements ResourceParser<MatrixMap3<Integer, String, Region, Float>> {

	public static final String CONTROL_KEY_P1 = "HL_PRIMARY_SP_EQN_P1";
	public static final String CONTROL_KEY_P2 = "HL_PRIMARY_SP_EQN_P2";
	public static final String CONTROL_KEY_P3 = "HL_PRIMARY_SP_EQN_P3";

	public static final int NUM_COEFFICIENTS_P1 = 3;
	public static final int NUM_COEFFICIENTS_P2 = 2;
	public static final int NUM_COEFFICIENTS_P3 = 4;

	public static final String SP0_KEY = "sp0";
	public static final String REGION_KEY = "region";
	public static final String COEFFICIENT_KEY = "coefficient";

	int numCoefficients;

	public HLCoefficientParser(int numCoefficients) {
		super();
		this.numCoefficients = numCoefficients;
		this.lineParser = new LineParser() {

			@Override
			public boolean isStopLine(String line) {
				return line.startsWith("   ");
			}

		}.value(2, SP0_KEY, String::strip).space(1).value(1, REGION_KEY, ValueParser.REGION)
				.multiValue(numCoefficients, 10, COEFFICIENT_KEY, ValueParser.FLOAT);
	}

	LineParser lineParser;

	@Override
	public MatrixMap3<Integer, String, Region, Float> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {
		final var regionIndicies = Arrays.asList(Region.values());
		final List<Integer> coeIndicies = Stream.iterate(1, x -> x + 1).limit(numCoefficients)
				.collect(Collectors.toList());
		final var speciesIndicies = SP0DefinitionParser.getSpeciesAliases(control);

		MatrixMap3<Integer, String, Region, Float> result = new MatrixMap3Impl<Integer, String, Region, Float>(
				coeIndicies, speciesIndicies, regionIndicies
		);
		lineParser.parse(is, result, (v, r) -> {
			var sp0 = (String) v.get(SP0_KEY);
			var region = (Region) v.get(REGION_KEY);
			@SuppressWarnings("unchecked")
			var coefficients = (List<Float>) v.get(COEFFICIENT_KEY);
			if (!speciesIndicies.contains(sp0)) {
				throw new ValueParseException(sp0, sp0 + " is not a valid species");
			}

			for (int coeIndex = 0; coeIndex < numCoefficients; coeIndex++) {
				r.put(coeIndex + 1, sp0, region, coefficients.get(coeIndex));
			}
			return r;
		}, control);
		return result;
	}

}
