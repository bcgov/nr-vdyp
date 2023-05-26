package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.model.BaseAreaCode;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;

public class UtilComponentBaseAreaParser
		implements ResourceParser<MatrixMap3<BaseAreaCode, String, String, Coefficients>> {
	public static final String CONTROL_KEY = "UTIL_COMP_BA";

	public static final int NUM_COEFFICIENTS = 2;

	public static final String BA_UTIL_CODE_KEY = "baUtilCode";
	public static final String SPECIES_KEY = "species";
	public static final String REGION_KEY = "region";
	public static final String BEC_SCOPE_KEY = "becScope";
	public static final String COEFFICIENT_KEY = "coefficient";

	public UtilComponentBaseAreaParser() {
		super();
		this.lineParser = new LineParser() {

			@Override
			public boolean isStopLine(String line) {
				return line.startsWith("    ");
			}

		}.value(4, BA_UTIL_CODE_KEY, ValueParser.enumParser(BaseAreaCode.class)).space(1)
				.value(2, SPECIES_KEY, String::strip).space(1).value(4, BEC_SCOPE_KEY, String::strip)
				.multiValue(NUM_COEFFICIENTS, 10, COEFFICIENT_KEY, ValueParser.FLOAT);
	}

	LineParser lineParser;

	@Override
	public MatrixMap3<BaseAreaCode, String, String, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {
		final var becIndices = BecDefinitionParser.getBecAliases(control);
		final var speciesIndicies = SP0DefinitionParser.getSpeciesAliases(control);
		final var bauIndices = Set.of(BaseAreaCode.values());

		MatrixMap3<BaseAreaCode, String, String, Coefficients> result = new MatrixMap3Impl<BaseAreaCode, String, String, Coefficients>(
				bauIndices, speciesIndicies, becIndices
		);
		lineParser.parse(is, result, (v, r) -> {
			var bau = (BaseAreaCode) v.get(BA_UTIL_CODE_KEY);
			var sp0 = (String) v.get(SPECIES_KEY);
			var scope = (String) v.get(BEC_SCOPE_KEY);

			var becs = BecDefinitionParser.getBecsByScope(control, scope);
			if (becs.isEmpty()) {
				throw new ValueParseException(scope, "Could not find any BECs for scope " + scope);
			}

			@SuppressWarnings("unchecked")
			var coefficients = (List<Float>) v.get(COEFFICIENT_KEY);
			SP0DefinitionParser.checkSpecies(speciesIndicies, sp0);

			if (coefficients.size() < NUM_COEFFICIENTS) {
				throw new ValueParseException(null, "Expected " + NUM_COEFFICIENTS + " coefficients");
			}
			for (var bec : becs) {
				r.put(bau, sp0, bec.getAlias(), new Coefficients(coefficients, 1));
			}

			return r;
		});
		return result;
	}

}
