package ca.bc.gov.nrs.vdyp.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageParser;
import ca.bc.gov.nrs.vdyp.io.parse.CloseUtilVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.ControlFileParserTest;
import ca.bc.gov.nrs.vdyp.io.parse.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranLayerVolumeAdjustParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayWasteParser;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;

public class TestUtils {

	/**
	 * Create a stream returning the given sequence of lines.
	 */
	public static InputStream makeStream(String... lines) {
		return new ByteArrayInputStream(String.join("\r\n", lines).getBytes());
	}

	/**
	 * Return a mock file resolver that expects to resolve one file with the given
	 * name and returns the given input stream.
	 *
	 * @param expectedFilename
	 * @param is
	 * @return
	 */
	public static FileResolver fileResolver(String expectedFilename, InputStream is) {
		return new FileResolver() {

			@Override
			public InputStream resolve(String filename) throws IOException {
				if (filename.equals(expectedFilename)) {
					return is;
				} else {
					fail("Attempted to resolve unexpected filename " + filename);
					return null;
				}
			}

			@Override
			public String toString(String filename) throws IOException {
				return "TEST:" + filename;
			}

		};
	}

	/**
	 * Add a mock control map entry for BEC parse results
	 */
	public static void populateControlMapBecReal(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new BecDefinitionParser(), "Becdef.dat");
	}

	/**
	 * Add a mock control map entry for SP0 parse results. Alternates assigning to
	 * Coastal and Interior regions, starting with Coastal.
	 */
	public static void populateControlMapBec(Map<String, Object> controlMap, String... aliases) {

		List<BecDefinition> becs = new ArrayList<>();

		int i = 0;
		for (var alias : aliases) {
			becs.add(new BecDefinition(alias, Region.values()[i % 2], "Test " + alias, 2, 2, 2));
			i++;
		}

		controlMap.put(BecDefinitionParser.CONTROL_KEY, new BecLookup(becs));
	}

	/**
	 * Add a mock control map entry for BEC parse results with species "B1" and "B2"
	 * for Coastal and Interior Regions respectively
	 */
	public static void populateControlMapBec(Map<String, Object> controlMap) {
		populateControlMapBec(controlMap, "B1", "B2");
	}

	private static BecDefinition makeBec(String id, Region region, String name) {
		return new BecDefinition(
				id, region, name, //
				BecDefinitionParser.GROWTH_INDEX.get(id), //
				BecDefinitionParser.VOLUME_INDEX.get(id), //
				BecDefinitionParser.DECAY_INDEX.get(id)
		);
	}

	/**
	 * Add a mock control map entry for SP0 parse results with species "S1" and "S2"
	 */
	public static void populateControlMapGenus(Map<String, Object> controlMap) {
		populateControlMapGenus(controlMap, "S1", "S2");
	}

	/**
	 * Add a mock control map entry for SP0 parse results with 16 species
	 */
	public static void populateControlMapGenusReal(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new GenusDefinitionParser(), "SP0DEF_v0.dat");
	}

	/**
	 * Get the species aliases expected
	 */
	public static String[] getSpeciesAliases() {
		return new String[] { "AC", "AT", "B", "C", "D", "E", "F", "H", "L", "MB", "PA", "PL", "PW", "PY", "S", "Y" };
	}

	/**
	 * Add a mock control map entry for SP0 parse results
	 */
	public static void populateControlMapGenus(Map<String, Object> controlMap, String... aliases) {

		List<GenusDefinition> sp0List = new ArrayList<>();

		for (var alias : aliases) {
			sp0List.add(new GenusDefinition(alias, java.util.Optional.empty(), "Test " + alias));
		}

		controlMap.put(GenusDefinitionParser.CONTROL_KEY, sp0List);
	}

	/**
	 * Add a mock control map entries for equation groups
	 */
	public static void populateControlMapEquationGroups(
			Map<String, Object> controlMap, //
			BiFunction<String, String, int[]> mapper
	) {

		var becAliases = BecDefinitionParser.getBecAliases(controlMap);
		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var volume = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[0]));
		controlMap.put(VolumeEquationGroupParser.CONTROL_KEY, volume);

		var decay = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[1]));
		controlMap.put(DecayEquationGroupParser.CONTROL_KEY, decay);

		var breakage = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases, mapper.andThen(x -> x[2]));
		controlMap.put(BreakageEquationGroupParser.CONTROL_KEY, breakage);
	}

	/**
	 * Add mock control map entry for VeteranBQ Map
	 */
	public static void populateControlMapVeteranBq(Map<String, Object> controlMap) {
		populateControlMapFromResource(controlMap, new VeteranBQParser(), "REGBAV01.COE");
	}

	public static void
			populateControlMapVeteranDq(Map<String, Object> controlMap, BiFunction<String, Region, float[]> mapper) {
		var regions = Arrays.asList(Region.values());
		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var result = new MatrixMap2Impl<String, Region, Coefficients>(
				genusAliases, regions, mapper.andThen(x -> new Coefficients(x, 1))
		);
		controlMap.put(VeteranDQParser.CONTROL_KEY, result);
	}

	public static void
			populateControlMapVeteranVolAdjust(Map<String, Object> controlMap, Function<String, float[]> mapper) {

		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var result = genusAliases.stream()
				.collect(Collectors.toMap(x -> x, mapper.andThen(x -> new Coefficients(x, 1))));

		controlMap.put(VeteranLayerVolumeAdjustParser.CONTROL_KEY, result);
	}

	public static void populateControlMapWholeStemVolume(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(UtilComponentWSVolumeParser.MAX_GROUPS);

		populateControlMap2(controlMap, UtilComponentWSVolumeParser.CONTROL_KEY, UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void populateControlMapCloseUtilization(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(CloseUtilVolumeParser.MAX_GROUPS);

		populateControlMap2(controlMap, CloseUtilVolumeParser.CONTROL_KEY, UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void populateControlMapNetDecay(
			Map<String, Object> controlMap, BiFunction<Integer, Integer, Optional<Coefficients>> mapper
	) {

		var groupIndicies = groupIndices(VolumeNetDecayParser.MAX_GROUPS);

		populateControlMap2(controlMap, VolumeNetDecayParser.CONTROL_KEY, UTIL_CLASSES, groupIndicies, mapper);
	}

	public static void
			populateControlMapNetWaste(Map<String, Object> controlMap, Function<String, Coefficients> mapper) {
		var speciesDim = Arrays.asList(getSpeciesAliases());

		populateControlMap1(controlMap, VolumeNetDecayWasteParser.CONTROL_KEY, speciesDim, mapper);
	}

	public static void
			populateControlMapNetBreakage(HashMap<String, Object> controlMap, Function<Integer, Coefficients> mapper) {
		var groupIndicies = groupIndices(BreakageParser.MAX_GROUPS);

		populateControlMap1(controlMap, BreakageParser.CONTROL_KEY, groupIndicies, mapper);
	}

	public static <K1, K2, V> void populateControlMap2(
			Map<String, Object> controlMap, String key, Collection<K1> keys1, Collection<K2> keys2,
			BiFunction<K1, K2, V> mapper
	) {

		var result = new MatrixMap2Impl<>(keys1, keys2, mapper);

		controlMap.put(key, result);
	}

	public static <K, V> void populateControlMap1(
			Map<String, Object> controlMap, String key, Collection<K> keys1, Function<K, V> mapper
	) {

		var result = keys1.stream().collect(Collectors.toMap(k -> k, mapper));

		controlMap.put(key, result);
	}

	static final Collection<Integer> UTIL_CLASSES = IntStream.rangeClosed(-1, 4).mapToObj(x -> x).toList();

	static Collection<Integer> groupIndices(int max) {
		return IntStream.rangeClosed(1, max).mapToObj(x -> x).toList();
	}

	public static void populateControlMapFromResource(
			Map<String, Object> controlMap, ResourceControlMapModifier parser, String resource
	) {
		try (var is = ControlFileParserTest.class.getResourceAsStream("coe/" + resource)) {
			parser.modify(controlMap, is);
		} catch (IOException | ResourceParseException ex) {
			fail(ex);
		}
	}
}
