package ca.bc.gov.nrs.vdyp.fip.test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;

import ca.bc.gov.nrs.vdyp.fip.ModifierParser;
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
}
