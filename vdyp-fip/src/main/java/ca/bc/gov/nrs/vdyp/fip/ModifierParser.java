package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.io.parse.OptionalResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParser;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;

public class ModifierParser implements OptionalResourceControlMapModifier {

	public static final String CONTROL_KEY = "MODIFIERS";

	/**
	 * MatrixMap3 of COE Index 1-3, Species ID, Region to Float
	 */
	public static final String CONTROL_KEY_MOD098 = "VET_BA_MODIFIERS";
	/**
	 * MatrixMap2 of Species ID, Region to Float
	 */
	public static final String CONTROL_KEY_MOD200_BA = "BA_MODIFIERS";
	/**
	 * MatrixMap2 of Species ID, Region to Float
	 */
	public static final String CONTROL_KEY_MOD200_DQ = "DQ_MODIFIERS";
	/**
	 * MatrixMap2 of Species ID, Region to Float
	 */
	public static final String CONTROL_KEY_MOD301_DECAY = "DECAY_MODIFIERS";
	/**
	 * MatrixMap2 of Species ID, Region to Float
	 */
	public static final String CONTROL_KEY_MOD301_WASTE = "WASTE_MODIFIERS";

	public static final String CONTROL_KEY_MOD400 = "HL_MODIFIERS";

	public static final int MAX_MODS = 60;

	@Override
	public void modify(Map<String, Object> control, InputStream data) throws ResourceParseException, IOException {
		// Modifiers, IPSJF155-Appendix XII

		// RD_E198

		defaultModify(control);
	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

	@Override
	public void defaultModify(Map<String, Object> control) {
		var spAliases = SP0DefinitionParser.getSpeciesAliases(control);
		var regions = Arrays.asList(Region.values());

		var baModifiers = new MatrixMap2Impl<String, Region, Float>(spAliases, regions);
		baModifiers.setAll(1.0f);
		control.put(CONTROL_KEY_MOD200_BA, baModifiers);

		var dqModifiers = new MatrixMap2Impl<String, Region, Float>(spAliases, regions);
		dqModifiers.setAll(1.0f);
		control.put(CONTROL_KEY_MOD200_DQ, dqModifiers);

		var decayModifiers = new MatrixMap2Impl<String, Region, Float>(spAliases, regions);
		decayModifiers.setAll(0.0f);
		control.put(CONTROL_KEY_MOD301_DECAY, decayModifiers);

		var wasteModifiers = new MatrixMap2Impl<String, Region, Float>(spAliases, regions);
		wasteModifiers.setAll(0.0f);
		control.put(CONTROL_KEY_MOD301_WASTE, wasteModifiers);
	}

}
