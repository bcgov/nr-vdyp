package ca.bc.gov.nrs.vdyp.fip.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.opentest4j.AssertionFailedError;

import ca.bc.gov.nrs.vdyp.fip.FipControlParser;
import ca.bc.gov.nrs.vdyp.fip.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.ControlFileParserTest;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class FipTestUtils {

	/**
	 * Fill in the decay modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapDecayModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY, spec, regions, mapper);
	}

	/**
	 * Fill in the waste modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapWasteModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE, spec, regions, mapper);
	}

	/**
	 * Apply modifiers to mock test map to simulate control file parser.
	 *
	 * @param controlMap
	 */
	public static void modifyControlMap(HashMap<String, Object> controlMap) {
		int jprogram = 1;
		TestUtils.populateControlMapFromResource(controlMap, new ModifierParser(jprogram), "mod19813.prm");

	}

	public static Map<String, Object> loadControlMap(FipControlParser parser, Class<?> klazz, String resourceName)
			throws IOException, ResourceParseException {
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, FipTestUtils.fileResolver(klazz));
		}
	}

	/**
	 * Load the control map from resources in the test package using the full
	 * control map parser.
	 */
	public static Map<String, Object> loadControlMap() {
		var parser = new FipControlParser();
		try {
			return loadControlMap(parser, ControlFileParserTest.class, "FIPSTART.CTR");
		} catch (IOException | ResourceParseException ex) {
			throw new AssertionFailedError(null, ex);
		}

	}

	public static FileResolver fileResolver(Class<?> klazz) {
		return new FileResolver() {

			@Override
			public InputStream resolve(String filename) throws IOException {
				InputStream resourceAsStream = klazz.getResourceAsStream(filename);
				if (resourceAsStream == null)
					throw new IOException("Could not load " + filename);
				return resourceAsStream;
			}

			@Override
			public String toString(String filename) throws IOException {
				return klazz.getResource(filename).toString();
			}
		};
	}
}
