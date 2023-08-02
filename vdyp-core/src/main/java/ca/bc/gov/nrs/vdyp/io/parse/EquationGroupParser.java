package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.bc.gov.nrs.vdyp.common.ExpectationDifference;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;

/**
 * Parser for a Volume Equation Group data file
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public abstract class EquationGroupParser implements ControlMapSubResourceParser<MatrixMap2<String, String, Integer>> {

	LineParser lineParser;

	private Collection<String> hiddenBecs = Collections.emptyList();

	protected EquationGroupParser() {
		this(3);
	}

	protected EquationGroupParser(int identifierLength) {
		lineParser = new LineParser().strippedString(2, "sp0Alias").space(1).strippedString(4, "becAlias").space(1)
				.integer(identifierLength, "grpId");
	}

	@Override
	public MatrixMap2<String, String, Integer> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		final var sp0Keys = GenusDefinitionParser.getSpeciesAliases(control);

		final var becKeys = BecDefinitionParser.getBecAliases(control);

		Map<String, Map<String, Integer>> resultMap = lineParser.parse(is, new HashMap<>(), (v, r) -> {
			final String sp0Alias = (String) v.get("sp0Alias");
			final String becAlias = (String) v.get("becAlias");

			if (!sp0Keys.contains(sp0Alias)) {
				throw new ValueParseException(sp0Alias, sp0Alias + " is not an SP0 identifier");
			}
			if (!becKeys.contains(becAlias)) {
				throw new ValueParseException(becAlias, becAlias + " is not a BEC identifier");
			}

			int grpId = (Integer) v.get("grpId");

			r.computeIfAbsent(sp0Alias, k -> new HashMap<>()).put(becAlias, grpId);
			return r;
		}, control);

		for (var e : resultMap.entrySet()) {
			resultMap.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
		}

		// Validate that the cartesian product of SP0 and BEC values has been provided,
		// excluding unused BECs.
		// The original fortran did a check that the number of values read matched a
		// hard coded number.

		List<String> errors = new ArrayList<>();

		var restrictedBecKeys = becKeys.stream().filter(k -> !hiddenBecs.contains(k)).toList();

		var sp0Diff = ExpectationDifference.difference(resultMap.keySet(), sp0Keys);

		sp0Diff.getMissing().stream()
				.map(sp0Key -> String.format("Expected mappings for SP0 %s but it was missing", sp0Key))
				.collect(Collectors.toCollection(() -> errors));
		sp0Diff.getUnexpected().stream().map(sp0Key -> String.format("Unexpected mapping for SP0 %s", sp0Key))
				.collect(Collectors.toCollection(() -> errors));

		for (var entry : resultMap.entrySet()) {
			var becDiff = ExpectationDifference.difference(entry.getValue().keySet(), restrictedBecKeys);
			var sp0Key = entry.getKey();
			becDiff.getMissing().stream().map(
					becKey -> String
							.format("Expected mappings for BEC %s but it was missing for SP0 %s", becKey, sp0Key)
			).collect(Collectors.toCollection(() -> errors));
			becDiff.getUnexpected().stream()
					.map(becKey -> String.format("Unexpected mapping for BEC %s under SP0 %s", becKey, sp0Key))
					.collect(Collectors.toCollection(() -> errors));
		}

		if (!errors.isEmpty()) {
			throw new ResourceParseValidException(String.join(System.lineSeparator(), errors));
		}

		// convert nested Map to a MatrixMap. Building as the nested map first makes
		// validation easier.

		return new MatrixMap2Impl<>(sp0Keys, becKeys, (k1, k2) -> resultMap.get(k1).get(k2));

	}

	public void setHiddenBecs(Collection<String> hiddenBecs) {
		this.hiddenBecs = hiddenBecs;
	}

}
