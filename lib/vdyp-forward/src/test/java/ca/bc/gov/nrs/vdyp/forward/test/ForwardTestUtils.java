package ca.bc.gov.nrs.vdyp.forward.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.opentest4j.AssertionFailedError;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.forward.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingState;
import ca.bc.gov.nrs.vdyp.forward.model.ControlVariable;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ForwardTestUtils {

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
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY.name(), spec, regions, mapper);
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
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE.name(), spec, regions, mapper);
	}

	/**
	 * Apply modifiers to mock test map to simulate control file parser.
	 *
	 * @param controlMap
	 */
	public static void modifyControlMap(HashMap<String, Object> controlMap) {
		var jprogram = VdypApplicationIdentifier.VDYP_FORWARD;
		TestUtils.populateControlMapFromResource(controlMap, new ModifierParser(jprogram), "mod19813.prm");

	}

	public static Map<String, Object> loadControlMap(ForwardControlParser parser, Class<?> klazz, String resourceName)
			throws IOException, ResourceParseException {
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	/**
	 * Load the control map from resources in the test package using the full control map parser.
	 */
	public static Map<String, Object> loadControlMap() {
		var parser = new ForwardControlParser();
		try {
			return loadControlMap(parser, ForwardControlParser.class, "FIPSTART.CTR");
		} catch (IOException | ResourceParseException ex) {
			throw new AssertionFailedError(null, ex);
		}
	}

	public static Map<String, Object> parse(BaseControlParser parser, String resourceName)
			throws IOException, ResourceParseException {

		Class<?> klazz = TestUtils.class;
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	public static Float[] toFloatArray(float[] af) {
		Float[] aF = new Float[af.length];
		for (int i = 0; i < af.length; i++)
			aF[i] = af[i];
		return aF;
	}

	/**
	 * Explicitly override the per-polygon or overall target year settings to use the given value instead. This will
	 * apply for all polygons not yet processed, unless it is called again later, of course.
	 *
	 * @param fps        the Forward processing state
	 * @param targetYear the desired target year
	 */
	public static void setGrowthTargetYear(ForwardProcessingState fps, int targetYear) {
		try {
			fps.fcm.getForwardControlVariables().setControlVariable(ControlVariable.GROW_TARGET_1, targetYear);
		} catch (ValueParseException e) {
			throw new RuntimeException(e);
		}
	}

}
