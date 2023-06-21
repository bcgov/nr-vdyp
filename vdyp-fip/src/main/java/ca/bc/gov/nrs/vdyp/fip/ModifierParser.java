package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.io.parse.HLCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.HLNonprimaryCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.OptionalResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;

public class ModifierParser implements OptionalResourceControlMapModifier {

	public static final String CONTROL_KEY = "MODIFIERS";

	/**
	 * MatrixMap2 of Species ID, Region to Coefficients (1-3)
	 */
	public static final String CONTROL_KEY_MOD098_VETERAN_BQ = VeteranBQParser.CONTROL_KEY;
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

	/**
	 * MatrixMap3<Integer, String, Region, Float
	 */
	public static final String CONTROL_KEY_MOD400_P1 = HLCoefficientParser.CONTROL_KEY_P1;
	/**
	 * MatrixMap3<Integer, String, Region, Float
	 */
	public static final String CONTROL_KEY_MOD400_P2 = HLCoefficientParser.CONTROL_KEY_P2;
	/**
	 * MatrixMap3<Integer, String, Region, Float
	 */
	public static final String CONTROL_KEY_MOD400_P3 = HLCoefficientParser.CONTROL_KEY_P3;
	/**
	 * MatrixMap3<String, String, Region, NonprimaryHLCoefficients>
	 */
	public static final String CONTROL_KEY_MOD400_NONPRIMARY = HLNonprimaryCoefficientParser.CONTROL_KEY;

	public static final int MAX_MODS = 60;

	int jprogram;
	
	static final int[] ipoint = {1, 0, 2, 0, 0, 3, 4, 5, 0};

	public ModifierParser(int jprogram) {
		super();
		this.jprogram = jprogram;
	}

	@Override
	public void modify(Map<String, Object> control, InputStream data) throws ResourceParseException, IOException {
		// Modifiers, IPSJF155-Appendix XII

		// RD_E198

		defaultModify(control);

		var parser = new LineParser() {

			@Override
			public boolean isStopSegment(List<String> entry) {
				return !entry.isEmpty() && entry.get(0).equals("999");
			}

			@Override
			public boolean isIgnoredSegment(List<String> entry) {
				var sequence = entry.get(0);
				try {
					return sequence.isBlank() || ValueParser.INTEGER.parse(sequence) == 0;
				} catch (ValueParseException e) {
					return false;
				}
			}

			@Override
			public boolean isIgnoredLine(String line) {
				return line.isBlank();
			}

		}.integer(3, "sequence").multiValue(6, 2, "programs", ValueParser.LOGICAL)
				.multiValue(10, 6, "mods", ValueParser.optional(ValueParser.FLOAT));

		@SuppressWarnings("unchecked")
		final var baMap = (MatrixMap2<String, Region, Float>) control.get(CONTROL_KEY_MOD200_BA);
		@SuppressWarnings("unchecked")
		final var dqMap = (MatrixMap2<String, Region, Float>) control.get(CONTROL_KEY_MOD200_DQ);

		parser.parse(data, control, (entry, result) -> {
			int sequence = (int) entry.get("sequence");

			if (!modIsForProgram(entry))
				return result;

			if (sequence == 200) {
				var mods = getMods(4, entry);
				var sp0Aliases = SP0DefinitionParser.getSpeciesAliases(control);
				for (var sp0Alias : sp0Aliases) {
					baMap.put(sp0Alias, Region.COASTAL, mods.get(0));
					baMap.put(sp0Alias, Region.INTERIOR, mods.get(1));
					dqMap.put(sp0Alias, Region.COASTAL, mods.get(2));
					dqMap.put(sp0Alias, Region.INTERIOR, mods.get(3));
				}
			} else if (sequence > 200 && sequence <= 299) {
				var sp0Index = sequence - 200;
				var sp0Alias = SP0DefinitionParser.getSpeciesByIndex(sp0Index, control).getAlias();
				var mods = getMods(4, entry);

				baMap.put(sp0Alias, Region.COASTAL, mods.get(0));
				baMap.put(sp0Alias, Region.INTERIOR, mods.get(1));
				dqMap.put(sp0Alias, Region.COASTAL, mods.get(2));
				dqMap.put(sp0Alias, Region.INTERIOR, mods.get(3));
			}
			return result;
		}, control);

	}

	boolean modIsForProgram(Map<String, Object> entry) {
		@SuppressWarnings("unchecked")
		var programs = (List<Boolean>) entry.get("programs");
		var index = ipoint[this.jprogram - 1];
		if (index<=0) throw new IllegalStateException("JProgram "+this.jprogram + " mapped to "+index);
		return programs.get(index-1);
	}

	List<Float> getMods(int num, Map<String, Object> entry) throws ValueParseException {
		@SuppressWarnings("unchecked")
		var raw = (List<Optional<Float>>) entry.get("mods");
		return getMods(num, raw);
	}

	<T> List<T> getMods(int num, List<Optional<T>> raw) throws ValueParseException {
		var it = raw.iterator();

		var result = new ArrayList<T>(num);
		for (int i = 0; i < num; i++) {
			result.add(
					it.next().orElseThrow(() -> new ValueParseException("", "Expected " + num + " modifier values"))
			);
		}
		// Possibly log a warning if there are extra unused values
		return result;
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
