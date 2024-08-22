package ca.bc.gov.nrs.vdyp.io.parse.coe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseValidException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;

/**
 * Parser for a Genus (SP0; Species) Definition data file.
 *
 * This file contains records defining the species on which the system functions. Each record contains:
 * <ol>
 * <li>(cols 0-1) a species alias
 * <li>(cols 3-34) the name of the species
 * <li>(cols 36-37) - int - the "preference" value.
 * </ol>
 * The preference values define an ordering of the species from 1 to 16. If a line provides no value or the value 0, the
 * ordering is implicitly chosen to be the line number. Otherwise, a value from 1 - 16 must be provided and this value
 * is used as the ordering. In the end, all values from 1 - 16 must be used.
 * <p>
 * All lines in the file are read; there is no provision for blank lines (except the last line, if empty).
 * <p>
 * In the end, a value for each of the 16 Species must be provided, and each must have a distinct ordering value.
 * <p>
 * FIP Control index: 010
 * <p>
 * Example: coe/SP0DEF_v0.dat
 *
 * @author Kevin Smith, Vivid Solutions
 * @see ControlMapSubResourceParser
 */
public class GenusDefinitionParser implements ControlMapSubResourceParser<GenusDefinitionMap> {

	private int numSp0;

	private LineParser lineParser = new LineParser().strippedString(2, "alias").space(1).strippedString(32, "name")
			.space(1).value(
					2, "preference", (s, c) -> ValueParser.optional(ValueParser.INTEGER).parse(s)
							.flatMap(v -> v == 0 ? Optional.empty() : Optional.of(v))
			);

	public GenusDefinitionParser() {
		super();
		this.numSp0 = 16;
	}

	public GenusDefinitionParser(int numSp0) {
		super();
		this.numSp0 = numSp0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public GenusDefinitionMap parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		GenusDefinition[] result = new GenusDefinition[numSp0];

		result = lineParser.parse(is, result, (value, r, line) -> {
			String alias = (String) value.get("alias");
			Optional<Integer> preference = (Optional<Integer>) value.get("preference");
			String name = (String) value.get("name");
			Integer lineNumber = (Integer) value.get(LineParser.LINE_NUMBER_KEY);

			int index;
			if (preference.isPresent() && preference.get() != 0) {
				index = preference.get();
			} else {
				index = lineNumber;
			}

			if (index > numSp0 || index < 1) {
				throw new ValueParseException(
						Integer.toString(index),
						String.format(
								"preference values must be between %d and %d (inclusive); saw value %d", 1, numSp0,
								index
						)
				);
			}
			if (r[index - 1] != null) {
				throw new ValueParseException(
						Integer.toString(index),
						String.format(
								"Genera ordering %d has already been specified for genera %s", index, r[index - 1]
										.getAlias()
						)
				);
			}

			r[index - 1] = new GenusDefinition(alias, index, name);
			return r;
		}, control);

		if (Arrays.stream(result).anyMatch(Objects::isNull)) {
			throw new ResourceParseValidException("Not all genus definitions were provided.");
		}

		return new GenusDefinitionMap(Arrays.asList(result));
	}

	public static void checkSpecies(final Collection<String> speciesIndicies, final String sp0)
			throws ValueParseException {
		if (!speciesIndicies.contains(sp0)) {
			throw new ValueParseException(sp0, sp0 + " is not a valid genus (SP0)");
		}
	}

	public static void checkSpecies(final Map<String, Object> controlMap, final String sp0) throws ValueParseException {
		final var speciesIndicies = getSpeciesAliases(controlMap);
		checkSpecies(speciesIndicies, sp0);
	}

	public static GenusDefinitionMap getSpecies(final Map<String, Object> controlMap) {
		return Utils.<GenusDefinitionMap>expectParsedControl(
				controlMap, ControlKey.SP0_DEF.name(), GenusDefinitionMap.class
		);
	}

	public static Collection<String> getSpeciesAliases(final Map<String, Object> controlMap) {
		return getSpecies(controlMap).getAllGeneraAliases();
	}

	/**
	 * Get a species based on its index from 1
	 *
	 * @param index
	 * @param controlMap
	 * @return
	 */
	public static GenusDefinition getSpeciesByIndex(final int index, final Map<String, Object> controlMap) {

		return getSpecies(controlMap).getByIndex(index);
	}

	public static int getIndex(final String alias, final Map<String, Object> controlMap) {
		return getSpecies(controlMap).getIndexByAlias(alias);
	}

	@Override
	public ControlKey getControlKey() {
		return ControlKey.SP0_DEF;
	}
}
