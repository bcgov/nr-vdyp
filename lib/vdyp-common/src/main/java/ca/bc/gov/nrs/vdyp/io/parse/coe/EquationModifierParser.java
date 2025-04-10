package ca.bc.gov.nrs.vdyp.io.parse.coe;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;

/**
 * Parser for an equation modifier mapping data file. This file contains a list of integer triples (3 chars, 4 chars, 4
 * chars)
 * <ol>
 * <li>default equation number (range: 1 - 180)
 * <li>an ITG number (range: 1 - 45)
 * <li>a reassigned equation number
 * </ol>
 * Only some of the possible pairs will have values.
 * <p>
 * FIP Control index: 031
 * <p>
 * Example: coe/GMODBA1.DAT
 *
 * @see ControlMapSubResourceParser
 * @author Kevin Smith, Vivid Solutions
 */
public class EquationModifierParser implements ControlMapSubResourceParser<MatrixMap2<Integer, Integer, Integer>> {

	// C_BAGRP1/BG1MODV
	public static final Integer DEFAULT_VALUE = 0;

	private static final String DEFAULT_KEY = "default";
	private static final String ITG_KEY = "itg";
	private static final String REASSIGNED_KEY = "reassigned";

	private static final List<Integer> DEFAULT_ID_RANGE = IntStream
			.rangeClosed(EquationGroupParser.MIN_GROUP, EquationGroupParser.MAX_GROUP).boxed().toList();
	private static final List<Integer> ITG_RANGE = IntStream.rangeClosed(1, 45).boxed().toList();

	LineParser lineParser = new LineParser().integer(3, DEFAULT_KEY).integer(4, ITG_KEY).integer(4, REASSIGNED_KEY);

	@Override
	public MatrixMap2<Integer, Integer, Integer> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		MatrixMap2<Integer, Integer, Integer> result = new MatrixMap2Impl<>(
				DEFAULT_ID_RANGE, ITG_RANGE, emptyDefault()
		);
		result = lineParser.parse(is, result, (value, r, line) -> {
			final int defaultId = (int) value.get(DEFAULT_KEY);
			final int itg = (int) value.get(ITG_KEY);
			final int reassignedId = (int) value.get(REASSIGNED_KEY);

			r.put(defaultId, itg, reassignedId);
			return r;
		}, control);

		return result;
	}

	/**
	 * For this matrix map, the default value is 0, as per VDYP7 RD_GMBA1.
	 */
	public static BiFunction<Integer, Integer, Integer> emptyDefault() {
		return (k1, k2) -> DEFAULT_VALUE;
	}

	@Override
	public ControlKey getControlKey() {
		return ControlKey.EQN_MODIFIERS;
	}

}
