package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.BreakageParser;
import ca.bc.gov.nrs.vdyp.io.parse.BySpeciesDqCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.CloseUtilVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.CoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.ComponentSizeParser;
import ca.bc.gov.nrs.vdyp.io.parse.ControlFileParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.SiteCurveAgeMaximumParser;
import ca.bc.gov.nrs.vdyp.io.parse.SiteCurveParser;
import ca.bc.gov.nrs.vdyp.io.parse.SmallComponentBaseAreaParser;
import ca.bc.gov.nrs.vdyp.io.parse.SmallComponentDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.SmallComponentHLParser;
import ca.bc.gov.nrs.vdyp.io.parse.SmallComponentProbabilityParser;
import ca.bc.gov.nrs.vdyp.io.parse.SmallComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.StockingClassFactorParser;
import ca.bc.gov.nrs.vdyp.io.parse.TotalStandWholeStemParser;
import ca.bc.gov.nrs.vdyp.io.parse.UpperCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentBaseAreaParser;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentDQParser;
import ca.bc.gov.nrs.vdyp.io.parse.UtilComponentWSVolumeParser;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranBQParser;
import ca.bc.gov.nrs.vdyp.io.parse.VeteranLayerVolumeAdjustParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayParser;
import ca.bc.gov.nrs.vdyp.io.parse.VolumeNetDecayWasteParser;
import ca.bc.gov.nrs.vdyp.io.parse.EquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.EquationModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.HLCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.HLNonprimaryCoefficientParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SP0Definition;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;

/**
 * Parser for FIP control files
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class FipControlParser {

	public static final String FIP_YIELD_POLY_INPUT = "FIP_YIELD_POLY_INPUT";
	public static final String FIP_YIELD_LAYER_INPUT = "FIP_YIELD_LAYER_INPUT";
	public static final String FIP_YIELD_LX_SP0_INPUT = "FIP_YIELD_LxSP0_INPUT";
	public static final String VDYP_POLYGON = "VDYP_POLYGON";
	public static final String VDYP_LAYER_BY_SPECIES = "VDYP_LAYER_BY_SPECIES";
	public static final String VDYP_LAYER_BY_SP0_BY_UTIL = "VDYP_LAYER_BY_SP0_BY_UTIL";
	public static final String VOLUME_EQN_GROUPS = EquationGroupParser.VOLUME_CONTROL_KEY;
	public static final String DECAY_GROUPS = EquationGroupParser.DECAY_CONTROL_KEY;
	public static final String BREAKAGE_GROUPS = EquationGroupParser.BREAKAGE_CONTROL_KEY;
	public static final String SITE_CURVE_NUMBERS = SiteCurveParser.CONTROL_KEY;
	public static final String SITE_CURVE_AGE_MAX = SiteCurveAgeMaximumParser.CONTROL_KEY;
	public static final String DEFAULT_EQ_NUM = EquationGroupParser.DEFAULT_CONTROL_KEY;
	public static final String EQN_MODIFIERS = EquationModifierParser.CONTROL_KEY;
	public static final String STOCKING_CLASS_FACTORS = StockingClassFactorParser.CONTROL_KEY;
	public static final String COE_BA = CoefficientParser.BA_CONTROL_KEY;
	public static final String COE_DQ = CoefficientParser.DQ_CONTROL_KEY;
	public static final String UPPER_BA_BY_CI_S0_P = UpperCoefficientParser.CONTROL_KEY;
	public static final String HL_PRIMARY_SP_EQN_P1 = HLCoefficientParser.CONTROL_KEY_P1;
	public static final String HL_PRIMARY_SP_EQN_P2 = HLCoefficientParser.CONTROL_KEY_P2;
	public static final String HL_PRIMARY_SP_EQN_P3 = HLCoefficientParser.CONTROL_KEY_P3;
	public static final String HL_NONPRIMARY = HLNonprimaryCoefficientParser.CONTROL_KEY;
	public static final String BY_SPECIES_DQ = BySpeciesDqCoefficientParser.CONTROL_KEY;
	public static final String SPECIES_COMPONENT_SIZE_LIMIT = ComponentSizeParser.CONTROL_KEY;
	public static final String UTIL_COMP_BA = UtilComponentBaseAreaParser.CONTROL_KEY;
	public static final String UTIL_COMP_DQ = UtilComponentDQParser.CONTROL_KEY;
	public static final String SMALL_COMP_PROBABILITY = SmallComponentProbabilityParser.CONTROL_KEY;
	public static final String SMALL_COMP_BA = SmallComponentBaseAreaParser.CONTROL_KEY;
	public static final String SMALL_COMP_DQ = SmallComponentDQParser.CONTROL_KEY;
	public static final String SMALL_COMP_HL = SmallComponentHLParser.CONTROL_KEY;
	public static final String SMALL_COMP_WS_VOLUME = SmallComponentWSVolumeParser.CONTROL_KEY;
	public static final String TOTAL_STAND_WHOLE_STEM_VOL = TotalStandWholeStemParser.CONTROL_KEY;
	public static final String UTIL_COMP_WS_VOLUME = UtilComponentWSVolumeParser.CONTROL_KEY;
	public static final String CLOSE_UTIL_VOLUME = CloseUtilVolumeParser.CONTROL_KEY;
	public static final String VOLUME_NET_DECAY = VolumeNetDecayParser.CONTROL_KEY;
	public static final String VOLUME_NET_DECAY_WASTE = VolumeNetDecayWasteParser.CONTROL_KEY;
	public static final String BREAKAGE = BreakageParser.CONTROL_KEY;
	public static final String VETERAN_LAYER_VOLUME_ADJUST = VeteranLayerVolumeAdjustParser.CONTROL_KEY;
	public static final String VETERAN_LAYER_DQ = "VETERAN_LAYER_DQ";
	public static final String VETERAN_BQ = VeteranBQParser.CONTROL_KEY;
	public static final String MINIMA = "MINIMA";
	public static final String MODIFIER_FILE = "MODIFIER_FILE";
	public static final String DEBUG_SWITCHES = "DEBUG_SWITCHES";
	public static final String MAX_NUM_POLY = "MAX_NUM_POLY";
	public static final String BEC_DEF = BecDefinitionParser.CONTROL_KEY;
	public static final String SP0_DEF = SP0DefinitionParser.CONTROL_KEY;

	// TODO
	public static final String TODO = "TODO";

	static final ValueParser<String> FILENAME = String::strip;

	ControlFileParser controlParser = new ControlFileParser().record(1, MAX_NUM_POLY, ValueParser.INTEGER)

			.record(9, BEC_DEF, FILENAME) // RD_BECD
			.record(10, SP0_DEF, FILENAME) // RD_SP0

			.record(11, FIP_YIELD_POLY_INPUT, FILENAME) // GET_FIPP
			.record(12, FIP_YIELD_LAYER_INPUT, FILENAME) // GET_FIPL
			.record(13, FIP_YIELD_LX_SP0_INPUT, FILENAME) // GET_FIPS

			.record(15, VDYP_POLYGON, FILENAME) //
			.record(16, VDYP_LAYER_BY_SPECIES, FILENAME) //
			.record(18, VDYP_LAYER_BY_SP0_BY_UTIL, FILENAME) //

			.record(20, VOLUME_EQN_GROUPS, FILENAME) // RD_VGRP
			.record(21, DECAY_GROUPS, FILENAME) // RD_DGRP
			.record(22, BREAKAGE_GROUPS, FILENAME) // RD_BGRP IPSJF157

			.record(25, SITE_CURVE_NUMBERS, ValueParser.optional(FILENAME)) // RD_E025
			.record(26, SITE_CURVE_AGE_MAX, ValueParser.optional(FILENAME)) // RD_E026

			.record(30, DEFAULT_EQ_NUM, FILENAME) // RD_GRBA1
			.record(31, EQN_MODIFIERS, FILENAME) // RD_GMBA1
			.record(33, STOCKING_CLASS_FACTORS, FILENAME) // RD_STK33

			.record(40, COE_BA, FILENAME) // RD_E040 IPSJF128
			.record(41, COE_DQ, FILENAME) // RD_E041 IPSJF129
			.record(43, UPPER_BA_BY_CI_S0_P, FILENAME) // RD_E043 IPSJF128

			.record(50, HL_PRIMARY_SP_EQN_P1, FILENAME) // RD_YHL1
			.record(51, HL_PRIMARY_SP_EQN_P2, FILENAME) // RD_YHL2
			.record(52, HL_PRIMARY_SP_EQN_P3, FILENAME) // RD_YHL3
			.record(53, HL_NONPRIMARY, FILENAME) // RD_YHL4

			.record(60, BY_SPECIES_DQ, FILENAME) // RD_E060 IPFJF125
			.record(61, SPECIES_COMPONENT_SIZE_LIMIT, FILENAME) // RD_E061 IPSJF158

			.record(70, UTIL_COMP_BA, FILENAME) // RD_UBA1
			.record(71, UTIL_COMP_DQ, FILENAME) // RD_UDQ1

			.record(80, SMALL_COMP_PROBABILITY, FILENAME) // RD_SBA1
			.record(81, SMALL_COMP_BA, FILENAME) // RD_SBA2
			.record(82, SMALL_COMP_DQ, FILENAME) // RD_SDQ1
			.record(85, SMALL_COMP_HL, FILENAME) // RD_SHL1
			.record(86, SMALL_COMP_WS_VOLUME, FILENAME) // RD_SVT1

			.record(90, TOTAL_STAND_WHOLE_STEM_VOL, FILENAME) // RD_YVT1 IPSJF117
			.record(91, UTIL_COMP_WS_VOLUME, FILENAME) // RD_YVT2 IPSJF121
			.record(92, CLOSE_UTIL_VOLUME, FILENAME) // RD_YVC1 IPSJF122
			.record(93, VOLUME_NET_DECAY, FILENAME) // RD_YVD1 IPSJF123
			.record(94, VOLUME_NET_DECAY_WASTE, FILENAME) // RD_YVW1 IPSJF123
			.record(95, BREAKAGE, FILENAME) // RD_EMP95 IPSJF157

			.record(96, VETERAN_LAYER_VOLUME_ADJUST, FILENAME) // RD_YVET
			.record(97, VETERAN_LAYER_DQ, FILENAME) // RD_YDQV
			.record(98, VETERAN_BQ, FILENAME) // RD_E098

			.record(197, MINIMA, ValueParser.list(ValueParser.FLOAT)) // Minimum Height, Minimum BA, Min BA fully
																		// stocked.

			.record(198, MODIFIER_FILE, FILENAME) // RD_E198 IPSJF155, XII

			.record(199, DEBUG_SWITCHES, ValueParser.list(ValueParser.INTEGER)) // IPSJF155
	/*
	 * Debug switches (25) 0=default See IPSJF155, App IX 1st: 1: Do NOT apply BA
	 * limits from SEQ043 2nd: 1: Do NOT apply DQ limits from SEQ043 4th: Future
	 * Development. Choice of upper limits 9th: 0: Normal - Suppress MATH77 error
	 * messages. 1: show some MATH77 errors; 2: show all. 22nd 1: extra preference
	 * for preferred sp (SEQ 010).
	 */

	;

	public FipControlParser() {

	}

	Map<String, ?> parse(Path inputFile) throws IOException, ResourceParseException {
		try (var is = Files.newInputStream(inputFile)) {

			return parse(is, new FileResolver() {

				@Override
				public InputStream resolve(String filename) throws IOException {
					return Files.newInputStream(inputFile.resolveSibling(filename));
				}

				@Override
				public String toString(String filename) throws IOException {
					return inputFile.resolveSibling(filename).toString();
				}
			});
		}
	}

	Map<String, ?> parse(Class<?> klazz, String resourceName) throws IOException, ResourceParseException {
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parse(is, fileResolver(klazz));
		}
	}

	static FileResolver fileResolver(Class<?> klazz) {
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

	Map<String, ?> parse(InputStream is, FileResolver fileResolver) throws IOException, ResourceParseException {
		var map = controlParser.parse(is, Collections.emptyMap());

		// RD_BEC
		loadData(map, BecDefinitionParser.CONTROL_KEY, fileResolver, this::RD_BEC);

		// DEF_BEC
		// TODO

		// Read Definitions

		// RD_SP0
		loadData(map, SP0_DEF, fileResolver, this::RD_SP0);

		// Read Groups

		// RD_VGRP
		loadData(map, VOLUME_EQN_GROUPS, fileResolver, this::RD_VGRP);

		// RD_DGRP
		loadData(map, DECAY_GROUPS, fileResolver, this::RD_DGRP);

		// RD_BGRP
		loadData(map, BREAKAGE_GROUPS, fileResolver, this::RD_BGRP);

		// RD_GRBA1
		loadData(map, DEFAULT_EQ_NUM, fileResolver, this::RD_GRBA1);

		// RD_GMBA1
		loadData(map, EQN_MODIFIERS, fileResolver, this::RD_GMBA1);

		int jprogram = 1; // FIPSTART only TODO Track this down
		if (jprogram == 1) {
			// RD_STK33
			loadData(map, STOCKING_CLASS_FACTORS, fileResolver, this::RD_STK33);

			// TODO minima?
			/*
			 * READ(CNTRV(197), 197, ERR= 912 ) FMINH, FMINBA, FMINBAF,FMINVetH IF (FMINVetH
			 * .le. 0.0) FMINVetH=10.0
			 */
		}

		// User-assigned SC's (Site Curve Numbers)
		//
		// RD_E025
		loadDataOptional(map, SITE_CURVE_NUMBERS, fileResolver, this::RD_E025, Collections::emptyMap);

		// Max tot ages to apply site curves (by SC)
		//
		// RD_E026
		loadDataOptional(map, SITE_CURVE_AGE_MAX, fileResolver, this::RD_E026, SiteCurveAgeMaximumParser::defaultMap);

		// Coeff for Empirical relationships

		// RD_E040
		loadData(map, COE_BA, fileResolver, this::RD_E040);

		// RD_E041
		loadData(map, COE_DQ, fileResolver, this::RD_E041);

		// RD_E043
		loadData(map, UPPER_BA_BY_CI_S0_P, fileResolver, this::RD_E043);

		// RD_YHL1
		loadData(map, HL_PRIMARY_SP_EQN_P1, fileResolver, this::RD_YHL1);

		// RD_YHL2
		loadData(map, HL_PRIMARY_SP_EQN_P2, fileResolver, this::RD_YHL2);

		// RD_YHL3
		loadData(map, HL_PRIMARY_SP_EQN_P3, fileResolver, this::RD_YHL3);

		// RD_YHL4
		loadData(map, HL_NONPRIMARY, fileResolver, this::RD_YHL4);

		// RD_E060
		loadData(map, BY_SPECIES_DQ, fileResolver, this::RD_E060);

		// Min and max DQ by species

		// RD_E061
		loadData(map, SPECIES_COMPONENT_SIZE_LIMIT, fileResolver, this::RD_E061);

		// RD_UBA1
		loadData(map, UTIL_COMP_BA, fileResolver, this::RD_UBA1);

		// RD_UDQ1
		loadData(map, UTIL_COMP_DQ, fileResolver, this::RD_UDQ1);

		// Small Component (4.5 to 7.5 cm)

		// RD_SBA1
		loadData(map, SMALL_COMP_PROBABILITY, fileResolver, this::RD_SBA1);

		// RD_SBA2
		loadData(map, SMALL_COMP_BA, fileResolver, this::RD_SBA2);

		// RD_SDQ1
		loadData(map, SMALL_COMP_DQ, fileResolver, this::RD_SDQ1);

		// RD_SHL1
		loadData(map, SMALL_COMP_HL, fileResolver, this::RD_SHL1);

		// RD_SVT1
		loadData(map, SMALL_COMP_WS_VOLUME, fileResolver, this::RD_SVT1);

		// Standard Volume Relationships

		// RD_YVT1
		loadData(map, TOTAL_STAND_WHOLE_STEM_VOL, fileResolver, this::RD_YVT1);

		// RD_YVT2
		loadData(map, UTIL_COMP_WS_VOLUME, fileResolver, this::RD_YVT2);

		// RD_YVC1
		loadData(map, CLOSE_UTIL_VOLUME, fileResolver, this::RD_YVC1);

		// RD_YVD1
		loadData(map, VOLUME_NET_DECAY, fileResolver, this::RD_YVD1);

		// RD_YVW1
		loadData(map, VOLUME_NET_DECAY_WASTE, fileResolver, this::RD_YVW1);

		// RD_E095
		loadData(map, BREAKAGE, fileResolver, this::RD_E095);

		// Veterans

		// RD_YVVET
		loadData(map, VETERAN_LAYER_VOLUME_ADJUST, fileResolver, this::RD_YVVET);

		// RD_YDQV
		loadData(map, VETERAN_LAYER_DQ, fileResolver, this::RD_YDQV);

		// RD_E098
		loadData(map, VETERAN_BQ, fileResolver, this::RD_E098);

		// Initiation items NOT for FIPSTART
		if (jprogram > 1) {

			// RD_E106
			loadData(map, TODO, fileResolver, this::RD_E106);

			// RD_E107
			loadData(map, TODO, fileResolver, this::RD_E107);

			// RD_E108
			loadData(map, TODO, fileResolver, this::RD_E108);

			// Minima again, differently?
			// TODO

			/*
			 * READ(CNTRV(197), 197, ERR= 912 ) VMINH, VMINBA, VMINBAeqn,VMINvetH IF
			 * (VMINVetH .le. 0.0) VMINVetH=10.0
			 */

			// RD_E112
			// Was commented out in Fortran

			// RD_E116
			// Was commented out in Fortran
		}

		// Modifiers, IPSJF155-Appendix XII

		// RD_E198
		loadData(map, MODIFIER_FILE, fileResolver, this::RD_E198);

		// Debug switches (normally zero)
		// TODO
		/*
		 * 141 FORMAT(25I2) DO 140 I=1,25 if ((NDEBUG(I) .ne.0) .and. (IU_WRITE .gt. 0))
		 * & WRITE(IU_WRITE,142) I, NDEBUG(I) 142 FORMAT(' CTR Line 199 has
		 * NDEBUG(',i2,') =',I3) 140 CONTINUE
		 *
		 * if (isDBG) then call dbgMsg( logr, 0 ) "Debug Switches (Ctl Line 199)" ) call
		 * dbgI4( logr, 6 ) " 1: Applying BA Limits               ", NDEBUG( 1) ) call
		 * dbgMsg( logr, 6 ) "       0 - Apply BA limits from SEQ043." ) call dbgMsg(
		 * logr, 6 ) "       1 - Do not apply BA limits from SEQ043." ) call dbgI4(
		 * logr, 6 ) " 2: Applying BA Limits               ", NDEBUG( 2) ) call dbgMsg(
		 * logr, 6 ) "       0 - Apply DQ limits from SEQ043." ) call dbgMsg( logr, 6 )
		 * "       1 - Do not apply DQ limits from SEQ043." ) call dbgI4( logr, 6 )
		 * " 3: Not used                         ", NDEBUG( 3) ) call dbgI4( logr, 6 )
		 * " 4: Not implemented. Upper bounds    ", NDEBUG( 4) ) call dbgMsg( logr, 6 )
		 * "       0 - Will default to (2)." ) call dbgMsg( logr, 6 )
		 * "       1 - Limits from Ctl Line 108 (GRPBA1)." ) call dbgMsg( logr, 6 )
		 * "       2 - Limits from Ctl Line 043 (Coast/Interior)" ) call dbgI4( logr, 6
		 * ) " 5: Not used                         ", NDEBUG( 5) ) call dbgI4( logr, 6 )
		 * " 6: Not used                         ", NDEBUG( 6) ) call dbgI4( logr, 6 )
		 * " 7: Not used                         ", NDEBUG( 7) ) call dbgI4( logr, 6 )
		 * " 8: Not used                         ", NDEBUG( 8) ) call dbgI4( logr, 6 )
		 * " 9: Error handling                   ", NDEBUG( 9) ) call dbgMsg( logr, 6 )
		 * "       0 - Stop on all errors." ) call dbgMsg( logr, 6 )
		 * "       1 - Continue to next poly (non-I/O errs)." ) call dbgI4( logr, 6 )
		 * "10: Not used                         ", NDEBUG(10) ) call dbgI4( logr, 6 )
		 * "11: Not used                         ", NDEBUG(11) ) call dbgI4( logr, 6 )
		 * "12: Not used                         ", NDEBUG(12) ) call dbgI4( logr, 6 )
		 * "13: Not used                         ", NDEBUG(13) ) call dbgI4( logr, 6 )
		 * "14: Not used                         ", NDEBUG(14) ) call dbgI4( logr, 6 )
		 * "15: Not used                         ", NDEBUG(15) ) call dbgI4( logr, 6 )
		 * "16: Not used                         ", NDEBUG(16) ) call dbgI4( logr, 6 )
		 * "17: Not used                         ", NDEBUG(17) ) call dbgI4( logr, 6 )
		 * "18: Not used                         ", NDEBUG(18) ) call dbgI4( logr, 6 )
		 * "19: Not used                         ", NDEBUG(19) ) call dbgI4( logr, 6 )
		 * "20: Not used                         ", NDEBUG(20) ) call dbgI4( logr, 6 )
		 * "21: Not used                         ", NDEBUG(21) ) call dbgI4( logr, 6 )
		 * "22: Determining primary species      ", NDEBUG(22) ) call dbgMsg( logr, 6 )
		 * "       0 - Normal " ) call dbgMsg( logr, 6 )
		 * "       1 - Extra preference for preferred sp. as primary" ) call dbgI4(
		 * logr, 6 ) "23: Not used                         ", NDEBUG(23) ) call dbgI4(
		 * logr, 6 ) "24: Not used                         ", NDEBUG(24) ) call dbgI4(
		 * logr, 6 ) "25: Not used                         ", NDEBUG(25) ) end if
		 */

		return map;
	}

	void loadData(Map<String, Object> map, String key, FileResolver fileResolver, ResourceParser<?> parser)
			throws IOException, ResourceParseException {
		try (var is = fileResolver.resolve((String) map.get(key))) {
			map.put(key, parser.parse(is, map));
		}
	}

	<T> void loadDataOptional(
			Map<String, Object> map, String key, FileResolver fileResolver, ResourceParser<T> parser,
			Supplier<T> defaultSupplier
	) throws IOException, ResourceParseException {
		@SuppressWarnings("unchecked")
		Optional<String> path = (Optional<String>) map.get(key);
		if (!path.isPresent()) {
			// TODO Log
			map.put(key, defaultSupplier.get());
			return;
		}
		try (var is = fileResolver.resolve(path.get())) {
			map.put(key, parser.parse(is, map));
		}
	}

	/**
	 * Loads the information that was in the global arrays BECV, BECNM, and
	 * BECCOASTAL in Fortran
	 */
	private Map<String, BecDefinition> RD_BEC(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		var parser = new BecDefinitionParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global arrays SP0V, SP0NAMEV in Fortran
	 */
	private List<SP0Definition> RD_SP0(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		var parser = new SP0DefinitionParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array VGRPV in Fortran
	 */
	private Object RD_VGRP(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new EquationGroupParser();
		parser.setHiddenBecs(Arrays.asList("BG"));
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array DGRPV in Fortran
	 */
	private Object RD_DGRP(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new EquationGroupParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array BGRPV in Fortran
	 */
	private Object RD_BGRP(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new EquationGroupParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array BG1DEFV in Fortran
	 */
	private Object RD_GRBA1(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new EquationGroupParser(5);
		parser.setHiddenBecs(Arrays.asList("BG", "AT"));
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array BG1MODV in Fortran
	 */
	private Object RD_GMBA1(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new EquationModifierParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global arrays STKV, FACTV, and NPCTAREA
	 * in Fortran
	 */
	private Object RD_STK33(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new StockingClassFactorParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global arrays ISCURVE and SP_SCV in
	 * Fortran
	 */
	private Object RD_E025(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		var parser = new SiteCurveParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global V7COE026 arrays ASITELIM,
	 * T1SITELIM, T2SITELIM in Fortran
	 */
	private Map<Integer, SiteCurveAgeMaximum> RD_E026(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// ASITELIM for each site curve number and region, the maximum total age that
		// site curve will increment to.
		// Defaults to 140.0
		// Default to all 140.0 if file not specified
		// integer(3, 'siteCurve').float(7, 'ageCoast').float(7,'ageInt').float(7,
		// 't1').float(7,'t2')
		// ages of <=0.0 should be mapped to 1999.0
		// site curve number 999 is end of data
		// site curve number <-1 or >40 invalid.
		// site curve number -1 is special. map to max position in array
		// Structure: store the age for each region plus t1 and t2 for each site curve.

		var parser = new SiteCurveAgeMaximumParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE040 in Fortran
	 */
	private MatrixMap3<Integer, String, Integer, Float> RD_E040(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// 10 coefficients by species (SP0) by becs
		// 4 String BEC Alias, 2 gap, 1 int coefficient index, 2 int "Indicate", 16x8
		// Float coefficient for species
		// Blank bec is ignore line

		// if indicate is 2, map the coefficients in directly
		// if indicate is 0 write the first coeffecient from the file to all in the
		// array
		// if the indicate is 1, add the first from the file to each subsequent one.

		var parser = new CoefficientParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE041 in Fortran
	 */
	private MatrixMap3<Integer, String, Integer, Float> RD_E041(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		var parser = new CoefficientParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE043 in Fortran
	 */
	private MatrixMap3<Region, String, Integer, Float> RD_E043(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Species and Region mapped to two coefficients
		var parser = new UpperCoefficientParser();

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE050 in Fortran
	 */
	private MatrixMap3<Integer, String, Region, Float> RD_YHL1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Species and Region mapped to three coefficients
		// coeIndex from 1
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, CI, C1, C2, C3
		// 11 FORMAT( A2 , 1x, A1, 3f10.0)

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P1);

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE051 in Fortran
	 */
	private MatrixMap3<Integer, String, Region, Float> RD_YHL2(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Species and Region mapped to two coefficients

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P2);

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE052 in Fortran
	 */
	private MatrixMap3<Integer, String, Region, Float> RD_YHL3(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Species and Region mapped to four coefficients
		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P3);

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE053 in Fortran
	 */
	private MatrixMap3<String, String, Region, NonprimaryHLCoefficients>
			RD_YHL4(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		// Two Species and a Region mapped to 2 coefficients and an equation index
		var parser = new HLNonprimaryCoefficientParser();

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE060 in Fortran
	 */
	private List<Coefficients> RD_E060(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Map from coefficient index to per-species coefficient
		var parser = new BySpeciesDqCoefficientParser();

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE061 in Fortran
	 */
	private MatrixMap2<String, Region, Coefficients> RD_E061(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Species and Region to 4 floats
		var parser = new ComponentSizeParser(control);

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE070 in Fortran
	 */
	private MatrixMap3<Integer, String, String, Coefficients> RD_UBA1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// COMMON /BECIgrow/ NBECGROW, IBECGV(14), IBECGIC(14)

		// Sets
		// C 2 coef BY 3 UC by (16 SP0) by (12 BEC)
		// COMMON /V7COE070/ COE070( 2 , 3, 16, 12)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) CODE, SP0, BECSCOPE,
		// 1 (C(I),I=1,2)
		// 11 FORMAT( A4,1x, A2, 1x, A4, 2F10.0)

		// Ignore if first segment is blank

		// If BECSCOPE is empty, apply to all BECs, if it's I or C, apply to BECs in
		// that region, otherwise only the one BEC.

		var parser = new UtilComponentBaseAreaParser();

		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE071 in Fortran
	 */
	private Object RD_UDQ1(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {

		// Uses
		// COMMON /BECIgrow/ NBECGROW, IBECGV(14), IBECGIC(14)

		// Sets
		// C 4 coef BY 4 UC by (16 SP0) by (12 BEC)
		// COMMON /V7COE071/ COE071( 4 , 4, 16, 12)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) CODE, SP0, BECSCOPE,
		// 1 (C(I),I=1,4)
		// 11 FORMAT( A4,9x, A2, 1x, A4, 4F10.0)

		// Ignore if first segment is blank

		// If BECSCOPE is empty, apply to all BECs, if it's I or C, apply to BECs in
		// that region, otherwise only the one BEC.

		var parser = new UtilComponentDQParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE080 in Fortran
	 *
	 * @see SMALL_COMP_PROBABILITY
	 */
	private Map<String, Coefficients> RD_SBA1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// Sets
		// C 4 coe for each of 16 SP0's
		// COMMON /V7COE080/ COE080(4, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, (C(I),I=1,4)
		// 11 FORMAT( A2, 4f10.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new SmallComponentProbabilityParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE081 in Fortran
	 *
	 * @see SMALL_COMP_BA
	 */
	private Map<String, Coefficients> RD_SBA2(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// C 4 coe for each of 16 SP0's
		// COMMON /V7COE081/ COE081(4, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, (C(I),I=1,4)
		// 11 FORMAT( A2, 4f10.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new SmallComponentBaseAreaParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE082 in Fortran
	 *
	 * @see SMALL_COMP_DQ
	 */
	private Map<String, Coefficients> RD_SDQ1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// C 2 coe for each of 16 SP0's
		// COMMON /V7COE082/ COE082(2, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, (C(I),I=1,2)
		// 11 FORMAT( A2, 4f10.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new SmallComponentDQParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE085 in Fortran
	 *
	 * @see SMALL_COMP_HL
	 */
	private Map<String, Coefficients> RD_SHL1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// C 2 coe for each of 16 SP0's
		// COMMON /V7COE085/ COE085(2, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, (C(I),I=1,2)
		// 11 FORMAT( A2, 4f10.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new SmallComponentHLParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE086 in Fortran
	 *
	 * @see SMALL_COMP_WS_VOLUME
	 */
	private Map<String, Coefficients> RD_SVT1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// C 4 coe for each of 16 SP0's
		// COMMON /V7COE086/ COE086(4, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, (C(I),I=1,4)
		// 11 FORMAT( A2, 4f10.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new SmallComponentWSVolumeParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE090 in Fortran
	 *
	 * @see TOTAL_STAND_WHOLE_STEM_VOL
	 */
	private Map<Integer, Coefficients> RD_YVT1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// PARAMETER (MAXGROUP = 80)

		// Sets
		// C 9 coe for each of (Up to 80 groups)
		// COMMON /V7COE090/ COE090(0:8, MAXGROUP)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) VGRP, (C(I),I=0,8)
		// 11 FORMAT( I3, 9f10.0)

		// Ignore if first segment is 0

		// Coefficient is 0 indexed
		// Group is 1 indexed

		var parser = new TotalStandWholeStemParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE091 in Fortran
	 *
	 * @see UTIL_COMP_WS_VOLUME
	 */
	private MatrixMap2<Integer, Integer, Coefficients> RD_YVT2(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// PARAMETER (MAXGROUP = 80)

		// Sets
		// C 4 coe for (4 UC's) for (Up to 80 groups)
		// COMMON /V7COE091/ COE091(4, 4, MAXGROUP)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) UC, VGRP, (C(I),I=1,4)
		// 11 FORMAT( I2, I4, 4f10.0)

		// Ignore if first segment is 0

		// Coefficient is 0 indexed
		// UC is 1 indexed
		// Group is 1 indexed

		var parser = new UtilComponentWSVolumeParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE092 in Fortran
	 *
	 * @see CLOSE_UTIL_VOLUME
	 */
	private MatrixMap2<Integer, Integer, Coefficients> RD_YVC1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// PARAMETER (MAXGROUP = 80)

		// Sets
		// C 3 coe for (4 UC's) for (Up to 80 groups)
		// COMMON /V7COE092/ COE092(3, 4, MAXGROUP)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) UC, VGRP, (C(I),I=1,3)
		// 11 FORMAT( I2, I4, 4f10.0)

		// Ignore if first segment is 0

		// Group is 1 indexed
		// UC is 1 indexed
		// Coefficient is 1 indexed

		var parser = new CloseUtilVolumeParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE093 in Fortran
	 *
	 * @see VOLUME_NET_DECAY
	 */
	private MatrixMap2<Integer, Integer, Coefficients> RD_YVD1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// PARAMETER (MAXGROUP = 80)

		// Sets
		// C 3 coe for (4 UC's) for (Up to 80 groups)
		// COMMON /V7COE093/ COE093(3, 4, MAXGROUP)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) UC, DGRP, (C(I),I=1,3)
		// 11 FORMAT( I2, I4, 4f10.0)

		// Ignore if first segment is 0

		// Group is 1 indexed
		// UC is 1 indexed
		// Coefficient is 1 indexed

		var parser = new VolumeNetDecayParser();
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE094 in Fortran
	 *
	 * @see VOLUME_NET_DECAY_WASTE
	 */
	private Map<String, Coefficients> RD_YVW1(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		//

		// Sets
		// Species to 6 coefficients
		// COMMON /V7COE094/ COE094(0:5,16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, A
		// 11 FORMAT( A2, 6F9.0)

		// Ignore if first segment is blank

		// Coefficient is 0 indexed

		var parser = new VolumeNetDecayWasteParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE095 in Fortran
	 *
	 * @see BREAKAGE
	 */
	// Example FIPSTART.CTR calls this RD_EMP95
	private Map<Integer, Coefficients> RD_E095(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Uses
		// PARAMETER (MAXBGRP = 40)

		// Sets
		// C 4 for (Up to 40 groups)
		// COMMON /V7COE095/ COE095(4, MAXBGRP)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) BGRP, A(1), A(2), A(3), A(4)
		// 11 FORMAT( I2, 4f9.0)

		// Ignore if first segment is 0

		// Group is 1 indexed
		// Coefficient is 1 indexed

		var parser = new BreakageParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE096 in Fortran
	 *
	 * @see VETERAN_LAYER_VOLUME_ADJUST
	 */
	// Example FIPSTART.CTR calls this RD_YVET
	private Map<String, Coefficients> RD_YVVET(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// COMMON /V7COE096/ COE096( 4, 16)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, C
		// 11 FORMAT( A2 , 4f9.5)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		var parser = new VeteranLayerVolumeAdjustParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Loads the information that was in the global array COE097 in Fortran
	 *
	 * @see VETERAN_LAYER_DQ
	 */
	private MatrixMap2<String, Region, Coefficients> RD_YDQV(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {
		// TODO

		// Sets
		// C 3 coef BY 16 SP0 BY C/I (2)
		// COMMON /V7COE097/ COE097( 3, 16, 2)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, CI, C1, C2, C3
		// 11 FORMAT( A2 , 1x, A1, 3f9.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed

		// Apply to both Regions if blank

		return null;
	}

	/**
	 * Loads the information that was in the global array COE098 in Fortran
	 *
	 * @see VETERAN_BQ
	 */
	private MatrixMap2<String, Region, Coefficients> RD_E098(InputStream data, Map<String, Object> control)
			throws IOException, ResourceParseException {

		// Sets
		// C 3 coef BY 16 SP0 BY C/I (2)
		// COMMON /V7COE098/ COE098( 3, 16, 2)

		// Parses
		// 10 READ(IU_TEMP, 11, ERR=90, END=70) SP0, CI, C1, C2, C3
		// 11 FORMAT( A2 , 1x, A1, 3f9.0)

		// Ignore if first segment is blank

		// Coefficient is 1 indexed
		var parser = new VeteranBQParser(control);
		return parser.parse(data, control);
	}

	/**
	 * Modifies loaded data based on modifier file
	 *
	 * @see MODIFIER_FILE
	 */
	private Object RD_E198(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		// TODO

		return null;
	}

	/**
	 * TODO
	 */
	private Object RD_E106(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		return null;
	}

	/**
	 * TODO
	 */
	private Object RD_E107(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		throw new UnsupportedOperationException();
	}

	/**
	 * TODO
	 */
	private Object RD_E108(InputStream data, Map<String, Object> control) throws IOException, ResourceParseException {
		throw new UnsupportedOperationException();
	}

	static interface FileResolver {
		InputStream resolve(String filename) throws IOException;

		String toString(String filename) throws IOException;
	}

}
