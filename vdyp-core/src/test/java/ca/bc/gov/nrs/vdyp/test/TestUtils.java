package ca.bc.gov.nrs.vdyp.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeEquationGroupParser;
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
	 * Add a mock control map entry for BEC parse results with species "B1" and "B2"
	 * for Coastal and Interior Regions respectively
	 */
	public static void populateControlMapReal(Map<String, Object> controlMap) {
		List<BecDefinition> becs = new ArrayList<>();

		becs.add(makeBec("BG", Region.INTERIOR, "Bunchgrass"));
		becs.add(makeBec("BWBS", Region.INTERIOR, "Boreal White and Black Spruce"));
		becs.add(makeBec("CDF", Region.INTERIOR, "Coastal Dougfir"));
		becs.add(makeBec("CWH", Region.INTERIOR, "Coastal Western Hemlock"));
		becs.add(makeBec("ESSF", Region.INTERIOR, "Englemann Sruce -SubAlpine Fir"));
		becs.add(makeBec("ICH", Region.INTERIOR, "Interior Cedar-Hemlock"));
		becs.add(makeBec("IDF", Region.INTERIOR, "Interior DougFir"));
		becs.add(makeBec("MH", Region.INTERIOR, "Mountain Hemlock"));
		becs.add(makeBec("MS", Region.INTERIOR, "Montane Spruce"));
		becs.add(makeBec("PP", Region.INTERIOR, "Ponderosa Pine"));
		becs.add(makeBec("SBPS", Region.INTERIOR, "SubBoreal Pine-Spruce"));
		becs.add(makeBec("SBS", Region.INTERIOR, "SubBoreal Spruce"));
		becs.add(makeBec("SWB", Region.INTERIOR, "Spruce-Willow-Birch"));

		controlMap.put(BecDefinitionParser.CONTROL_KEY, new BecLookup(becs));
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
	public static void populateControlMapGensuReal(Map<String, Object> controlMap) {
		populateControlMapGenus(controlMap, getSpeciesAliases());
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

		var volume = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases);
		volume.setAll(mapper.andThen(x -> x[0]));
		controlMap.put(VolumeEquationGroupParser.CONTROL_KEY, volume);

		var decay = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases);
		decay.setAll(mapper.andThen(x -> x[1]));
		controlMap.put(DecayEquationGroupParser.CONTROL_KEY, decay);

		var breakage = new MatrixMap2Impl<String, String, Integer>(genusAliases, becAliases);
		breakage.setAll(mapper.andThen(x -> x[2]));
		controlMap.put(BreakageEquationGroupParser.CONTROL_KEY, breakage);
	}

	/**
	 * Add mock control map entry for VeteranBQ Map
	 */
	public static void populateControlMapVeteranBq(Map<String, Object> controlMap) {
		var speciesDim = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var vetBqMap = new MatrixMap2Impl<>(speciesDim, Arrays.asList(Region.values()));

		vetBqMap.put("B", Region.INTERIOR, new Coefficients(Arrays.asList(0.70932f, 7.63269f, 0.62545f), 1));

		controlMap.put(VeteranBQParser.CONTROL_KEY, vetBqMap);
	}

	public static void
			populateControlMapVeteranDq(Map<String, Object> controlMap, BiFunction<String, Region, float[]> mapper) {
		var regions = Arrays.asList(Region.values());
		var genusAliases = GenusDefinitionParser.getSpeciesAliases(controlMap);

		var result = new MatrixMap2Impl<String, Region, Coefficients>(genusAliases, regions);
		result.setAll(mapper.andThen(x -> new Coefficients(x, 1)));
		controlMap.put(VeteranDQParser.CONTROL_KEY, result);
	}
}
