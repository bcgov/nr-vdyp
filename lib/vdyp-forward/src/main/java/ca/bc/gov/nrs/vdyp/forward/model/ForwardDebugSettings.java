package ca.bc.gov.nrs.vdyp.forward.model;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

/**
 * (1) Species Dynamics
 * <ul>
 * <li>Value 0: Full species dynamics. Not recommended.
 * <li>Value 1: No species dynamics. Species percents in TPH and BA are constant.
 * <li>Value 2: Limited dynamics. Percents by BA are constant, but TPH %’s vary.
 * </ul>
 * (2) Maximum Breast Height Age
 * <ul>
 * <li>Value 0: No upper age for application of BA and DQ yield equations.
 * <li>Value n: Maximum BH age for application of BA and DQ yields is 100 * n (years).
 * </ul>
 * (3) Basal Area Growth Model
 * <ul>
 * <li>Value 0: BA growth comes from fiat model (approach to yield eqn’s).
 * <li>Value 1: BA growth from empirical model (older ages suspect).
 * <li>Value 2: Mixed fiat & empirical model. (See SEQ111 and IPSJF162).
 * </ul>
 * (4) Maximum Breast Height Per Species and Region
 * <ul>
 * <li>Value 0: Will default to (2). Controls upper bounds in some models.
 * <li>Value 1: Limits from SEQ108 on control file, categorized by GRPBA1.
 * <li>Value 2: Limits from SEQ043 on control file, Coast Interior & Leading species.
 * </ul>
 * (5) Messaging Level. See note (1), below (6) Quadratic Mean Diameter growth model
 * <ul>
 * <li>Value 0: DQ growth comes from fiat model (approach to yield eqn’s).
 * <li>Value 1: DQ growth from empirical model (older ages suspect).
 * <li>Value 2: Mixed fiat & empirical model. (See SEQ117 and IPSJF178).
 * </ul>
 * (8) Lorey Height change strategy
 * <ul>
 * <li>Value 0: Normal changes in Lorey height (HL), all species.
 * <li>Value 1: Force change to zero for nonprimary species if change in HD is zero.
 * <li>Value 2: Force change to zero for all species if change in HD is zero. (option 2 precludes changes in primary sp
 * HL due solely to change in TPH).
 * </ul>
 * (9) Can limit BA if DQ has been limited (see Note 2, below.)
 * <p>
 * (11-21) HD/Age/SI fill-in controls (see Note 3, and IPSJF174.doc).
 * <p>
 * <b>Note 1</b>: for all programs the 5th debug switch should be zero except for program debugging. This will prevent
 * “SZERO” messages in the output and error messages produced by the MATH77 routines. Allowable values:
 * <ul>
 * <li>Value 0: activates the MATH77 process to suppress messages AND furthermore totally deactivates the messaging
 * capability.
 * <li>Value 1: implements the MATH77 deactivation mechanism, but there will still be a few error messages.
 * <li>Value 2: allows for normal error messaging.
 * </ul>
 * The usual underlying cause of the messages is that in reconciling results for the different species, the program has
 * been forced to violate a DQ/HL limit for a species.
 * <p>
 * <b>Note 2</b>: There has been a programming change to VDYP7 GROW, such that the behavior of the BA increment may be
 * altered subsequent to a projection having hit or exceeded a DQ limit. The configuration file for VDYP7 GROW is
 * affected, in that on sequence 199, Ndebug(9) is now defined.
 * <ul>
 * <li>Value 0: causes the program to operate as it always has to date (2009.03.24)
 * <li>Value 1: causes the indicated condition to be checked for, and may cause some predicted BA increments to be
 * smaller, or to be set to zero.
 * </ul>
 * An explanation of the change, and the reason for it, are given in a document JFmemo_20090311.doc. Detailed notes for
 * code developers and those with detailed knowledge of VDYP7 are in JFmemo_20090318.doc.
 * <p>
 * <b>Note 3</b>: Valid values from Ndebug(11) through Ndebug(20):
 * <ul>
 * <li>0: No action
 * <li>1: Move SI from non-primary to primary using sitetools si conversions. Checks start with second high-BA species,
 * then ordered by ISP. If no conversions found, first attempted conversion redone 1:1.
 * <li>2: Move SI from primary to non-primary using sitetools si conversions.
 * <li>3: Complete (TOTAGE, BHAGE, YTBH) if 2 out of 3 present, all sp.
 * <li>4: Move total age from non-primary to primary species (From second-most common species if present, else 1st
 * encountered).
 * <li>5: Estimate dominant height of primary species from Lorey height.
 * <li>6: Estimate dominant height of non-primary species from Lorey heights.
 * <li>7: Estimate SI of primary species from HD and total age. (*)
 * <li>8: Estimate SI of primary species from HD and BHAGE. (*)
 * <li>9: Estimate SI of non-primary species from HD and total age. (*)
 * <li>10: Estimate SI of non-primary species from HD and BHAGE. (*)
 * <li>11: Estimate ages (total and BH and YTBH) of primary species from SI, HD
 * <li>12: Estimate ages (total and BH and YTBH) of non-primary species from SI, HD.
 * <li>13: Estimate YTBH of primary species from SI.
 * <li>14: Estimate YTBH of non-primary species from SI.
 * <li>15: Same as option 1, condition on total age (primary) < 30.
 * </ul>
 */
public class ForwardDebugSettings extends DebugSettings {

	private static final Logger logger = LoggerFactory.getLogger(ForwardDebugSettings.class);

	public static final int MAX_FILL_INDEX_SETTINGS_INDEX = DebugSettings.MAX_DEBUG_SETTINGS;

	public static final int SPECIES_DYNAMICS = 1;
	public static final int MAX_BREAST_HEIGHT_AGE = 2;
	public static final int BA_GROWTH_MODEL = 3;
	public static final int DQ_GROWTH_MODEL = 6;
	public static final int LOREY_HEIGHT_CHANGE_STRATEGY = 8;
	public static final int DO_LIMIT_BA_WHEN_DQ_LIMITED = 9;

	public static final float MAX_BREAST_HEIGHT_AGE_MULTIPLIER = 100f;

	public ForwardDebugSettings(Integer[] settings) {
		super(settings);
	}

	@Override
	protected Object process(int settingNumber, int value) {
		switch (settingNumber) {
		case SPECIES_DYNAMICS:
			return SpeciesDynamics.fromIndex(value);
		case MAX_BREAST_HEIGHT_AGE:
			return Optional.of(value).filter(x -> x > 0).map(x -> x * MAX_BREAST_HEIGHT_AGE_MULTIPLIER);
		case BA_GROWTH_MODEL, DQ_GROWTH_MODEL:
			return GrowthModel.fromIndex(value);
		case LOREY_HEIGHT_CHANGE_STRATEGY:
			return LoreyHeightChangeStrategy.fromIndex(value);
		case DO_LIMIT_BA_WHEN_DQ_LIMITED:
			return value != 0;
		default:
			return super.process(settingNumber, value);
		}
	}

	public Integer[] getFillInValues() {

		var result = new ArrayList<Integer>();

		int settingValue;
		int index = 11;
		while (index <= MAX_FILL_INDEX_SETTINGS_INDEX && (settingValue = getValue(index++)) != 0) {
			result.add(settingValue);
		}

		return result.toArray(Integer[]::new);
	}

	/**
	 * Species dynamics mode
	 */
	public enum SpeciesDynamics {
		/**
		 * Full species dynamics. Not recommended.
		 */
		FULL,
		/**
		 * No species dynamics. Species percents in TPH and BA are constant.
		 */
		NONE,
		/**
		 * Limited dynamics. Percents by BA are constant, but TPH %’s vary.
		 */
		PARTIAL;

		public static SpeciesDynamics fromIndex(int value) {
			switch (value) {
			case 0:
				return SpeciesDynamics.FULL;
			case 1:
				return SpeciesDynamics.NONE;
			case 2:
				return SpeciesDynamics.PARTIAL;
			default:
				return logDefaultForUnknown(logger, "upper bounds mode", value, SpeciesDynamics.FULL);
			}

		}
	}

	/**
	 * Get the species dynamics mode
	 */
	public SpeciesDynamics getSpeciesDynamics() {
		return (SpeciesDynamics) getProcessedValue(SPECIES_DYNAMICS);
	}

	/**
	 * Get the maximum breast height age if there is a limit.
	 */
	@SuppressWarnings("unchecked")
	public Optional<Float> getMaxBreastHeightAge() {
		return (Optional<Float>) getProcessedValue(MAX_BREAST_HEIGHT_AGE);
	}

	/**
	 * What type of growth model to use
	 */
	public enum GrowthModel {
		/**
		 * Growth comes from fiat model (approach to yield eqn’s).
		 */
		FIAT,
		/**
		 * Growth from empirical model (older ages suspect).
		 */
		EMPERICAL,
		/**
		 * Mixed fiat & empirical model. (See SEQ111 (BA) or SEQ117 (DQ) and IPSJF162 (BA) or IPSJF178 (DQ)).
		 */
		MIXED;

		public static GrowthModel fromIndex(int value) {
			switch (value) {
			case 0:
				return GrowthModel.FIAT;
			case 1:
				return GrowthModel.EMPERICAL;
			case 2:
				return GrowthModel.MIXED;
			default:
				return logDefaultForUnknown(logger, "growth model", value, GrowthModel.FIAT);
			}

		}
	}

	/**
	 * The growth model for basal area
	 */
	public GrowthModel getBasalAreaGrowthModel() {
		return (GrowthModel) getProcessedValue(BA_GROWTH_MODEL);
	}

	/**
	 * The growth model for quadratic mean diameter
	 */
	public GrowthModel getQuadraticMeanDiameterGrowthModel() {
		return (GrowthModel) getProcessedValue(DQ_GROWTH_MODEL);
	}

	/**
	 * Change strategy for lorey height
	 */
	public enum LoreyHeightChangeStrategy {
		/**
		 * Normal changes in Lorey height (HL), all species.
		 */
		NORMAL,
		/**
		 * Force change to zero for nonprimary species if change in HD is zero.
		 */
		ZERO_NONPRIMARY,
		/**
		 * Force change to zero for all species if change in HD is zero. (option 2 precludes changes in primary sp HL
		 * due solely to change in TPH).
		 */
		ZERO_ALL;

		public static LoreyHeightChangeStrategy fromIndex(int value) {
			switch (value) {
			case 0:
				return LoreyHeightChangeStrategy.NORMAL;
			case 1:
				return LoreyHeightChangeStrategy.ZERO_NONPRIMARY;
			case 2:
				return LoreyHeightChangeStrategy.ZERO_ALL;
			default:
				return logDefaultForUnknown(
						logger, "lorey height change strategy", value, LoreyHeightChangeStrategy.NORMAL
				);
			}

		}
	}

	/**
	 * Change strategy for lorey height
	 */
	public LoreyHeightChangeStrategy getLoreyHeightChangeStrategy() {
		return (LoreyHeightChangeStrategy) getProcessedValue(LOREY_HEIGHT_CHANGE_STRATEGY);
	}

	/**
	 * Should basal area be limited when quadratic main diameter is limited.
	 */
	public boolean getDoLimitBaWhenDqLimited() {
		return (Boolean) getProcessedValue(DO_LIMIT_BA_WHEN_DQ_LIMITED);
	}

}
