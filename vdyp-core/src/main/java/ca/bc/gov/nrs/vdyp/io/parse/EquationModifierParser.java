package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;

/**
 * Parser for an equation modifier mapping data file
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class EquationModifierParser
		implements ControlMapSubResourceParser<MatrixMap2<Integer, Integer, Optional<Integer>>> {

	// C_BAGRP1/BG1MODV

	public static final String CONTROL_KEY = "EQN_MODIFIERS";

	private static final String DEFAULT_KEY = "default";
	private static final String ITG_KEY = "itg";
	private static final String REASSIGNED_KEY = "reassigned";

	private static final List<Integer> DEFAULT_ID_RANGE = IntStream
			.rangeClosed(EquationGroupParser.MIN_GROUP, EquationGroupParser.MAX_GROUP).boxed().toList();
	private static final List<Integer> ITG_RANGE = IntStream.rangeClosed(1, 45).boxed().toList();

	LineParser lineParser = new LineParser().integer(3, DEFAULT_KEY).integer(4, ITG_KEY).integer(4, REASSIGNED_KEY);

	@Override
	public MatrixMap2<Integer, Integer, Optional<Integer>> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		MatrixMap2<Integer, Integer, Optional<Integer>> result = new MatrixMap2Impl<>(
				DEFAULT_ID_RANGE, ITG_RANGE, MatrixMap2Impl.emptyDefault()
		);
		result = lineParser.parse(is, result, (v, r) -> {
			final int defaultId = (int) v.get(DEFAULT_KEY);
			final int itg = (int) v.get(ITG_KEY);
			final int reassignedId = (int) v.get(REASSIGNED_KEY);

			r.put(defaultId, itg, Optional.of(reassignedId));
			return r;
		}, control);

		return result;
	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

}
