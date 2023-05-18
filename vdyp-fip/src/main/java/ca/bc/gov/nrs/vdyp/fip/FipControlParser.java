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
import ca.bc.gov.nrs.vdyp.io.parse.ControlFileParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.SiteCurveAgeMaximumParser;
import ca.bc.gov.nrs.vdyp.io.parse.SiteCurveParser;
import ca.bc.gov.nrs.vdyp.io.parse.StockingClassFactorParser;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.EquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.EquationModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
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
	public static final String COE_BA = "COE_BA";
	public static final String COE_DQ = "COE_DQ";
	public static final String UPPER_BA_BY_CI_S0_P = "UPPER_BA_BY_CI_S0_P";
	public static final String HL_PRIMARY_SP_EQN_P1 = "HL_PRIMARY_SP_EQN_P1";
	public static final String HL_PRIMARY_SP_EQN_P2 = "HL_PRIMARY_SP_EQN_P2";
	public static final String HL_PRIMARY_SP_EQN_P3 = "HL_PRIMARY_SP_EQN_P3";
	public static final String HL_NONPRIMARY = "HL_NONPRIMARY";
	public static final String BY_SPECIES_DQ = "BY_SPECIES_DQ";
	public static final String SPECIES_COMPONENT_SIZE_LIMIT = "SPECIES_COMPONENT_SIZE_LIMIT";
	public static final String UTIL_COMP_BA = "UTIL_COMP_BA";
	public static final String UTIL_COMP_DQ = "UTIL_COMP_DQ";
	public static final String SMALL_COMP_PROBABILITY = "SMALL_COMP_PROBABILITY";
	public static final String SMALL_COMP_BA = "SMALL_COMP_BA";
	public static final String SMALL_COMP_DQ = "SMALL_COMP_DQ";
	public static final String SMALL_COMP_HL = "SMALL_COMP_HL";
	public static final String SMALL_COMP_WS_VOLUME = "SMALL_COMP_WS_VOLUME";
	public static final String TOTAL_STAND_WHOLE_STEM_VOL = "TOTAL_STAND_WHOLE_STEM_VOL";
	public static final String UTIL_COMP_WS_VOLUME = "UTIL_COMP_WS_VOLUME";
	public static final String CLOSE_UTIL_VOLUME = "CLOSE_UTIL_VOLUME";
	public static final String VOLUME_NET_DECAY = "VOLUME_NET_DECAY";
	public static final String VOLUME_NET_DECAY_WASTE = "VOLUME_NET_DECAY_WASTE";
	public static final String BREAKAGE = "BREAKAGE";
	public static final String VETERAN_LAYER_VOLUME_ADJUST = "VETERAN_LAYER_VOLUME_ADJUST";
	public static final String VETERAN_LAYER_DQ = "VETERAN_LAYER_DQ";
	public static final String VETERAN_BQ = "VETERAN_BQ";
	public static final String MINIMA = "MINIMA";
	public static final String MODIFIER_FILE = "MODIFIER_FILE";
	public static final String DEBUG_SWITCHES = "DEBUG_SWITCHES";
	public static final String MAX_NUM_POLY = "MAX_NUM_POLY";
	public static final String BEC_DEF = BecDefinitionParser.CONTROL_KEY;
	public static final String SP0_DEF = SP0DefinitionParser.CONTROL_KEY;

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
		// TODO

		// RD_E041
		// TODO

		// RD_E043
		// TODO

		// RD_YHL1
		// TODO

		// RD_YHL2
		// TODO

		// RD_YHL3
		// TODO

		// RD_YHL4
		// TODO

		// RD_E060
		// TODO

		// Min and max DQ by species

		// RD_E061
		// TODO

		// RD_UBA1
		// TODO

		// RD_UDQ1
		// TODO

		// Small Component (4.5 to 7.5 cm)

		// RD_SBA1
		// TODO

		// RD_SBA2
		// TODO

		// RD_SDQ1
		// TODO

		// RD_SHL1
		// TODO

		// RD_SVT1
		// TODO

		// Standard Volume Relationships

		// RD_YVT1
		// TODO

		// RD_YVT2
		// TODO

		// RD_YVC1
		// TODO

		// RD_YVD1
		// TODO

		// RD_YVW1
		// TODO

		// RD_E095
		// TODO

		// Veterans

		// RD_YVVET
		// TODO

		// RD_YDQV
		// TODO

		// RD_E098
		// TODO

		// Initiation items NOT for FIPSTART
		if (jprogram == 1) {

			// RD_E106
			// TODO

			// RD_E107
			// TODO

			// RD_E108
			// TODO

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
		// TODO

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

	static interface FileResolver {
		InputStream resolve(String filename) throws IOException;

		String toString(String filename) throws IOException;
	}

}
