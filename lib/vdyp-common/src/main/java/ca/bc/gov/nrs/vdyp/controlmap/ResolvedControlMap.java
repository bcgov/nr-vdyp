package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;

public interface ResolvedControlMap {

	Map<String, Object> getControlMap();

	/** 9 - BEC_DEF */
	BecLookup getBecLookup();

	/** 10 - SP0_DEF */
	GenusDefinitionMap getGenusDefinitionMap();

	/** 20 - VOLUME_EQN_GROUPS */
	MatrixMap2<String, String, Integer> getVolumeEquationGroups();

	/** 21 - DECAY_GROUPS */
	MatrixMap2<String, String, Integer> getDecayEquationGroups();

	/** 22 - BREAKAGE_GROUPS */
	MatrixMap2<String, String, Integer> getBreakageEquationGroups();

	/** 26 - SITE_CURVE_AGE_MAX */
	Map<Integer, SiteCurveAgeMaximum> getMaximumAgeBySiteCurveNumber();

	/** 30 - DEFAULT_EQ_NUM */
	MatrixMap2<String, String, Integer> getDefaultEquationGroup();

	/** 31 - EQN_MODIFIERS */
	MatrixMap2<Integer, Integer, Integer> getEquationModifierGroup();

	/** 50 - HL_PRIMARY_SP_EQN_P1 */
	MatrixMap2<String, Region, Coefficients> getHl1Coefficients();

	/** 51 - HL_PRIMARY_SP_EQN_P2 */
	MatrixMap2<String, Region, Coefficients> getHl2Coefficients();

	/** 52 - HL_PRIMARY_SP_EQN_P3 */
	MatrixMap2<String, Region, Coefficients> getHl3Coefficients();

	/** 53 - HL_NONPRIMARY */
	MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getHlNonPrimaryCoefficients();

	/** 60 - BY_SPECIES_DQ */
	Map<String, Coefficients> getQuadMeanDiameterBySpeciesCoefficients();

	/** 61 - SPECIES_COMPONENT_SIZE_LIMIT */
	MatrixMap2<String, Region, ComponentSizeLimits> getComponentSizeLimits();

	/** 70 - UTIL_COMP_BA */
	MatrixMap3<Integer, String, String, Coefficients> getBasalAreaDiameterUtilizationComponentMap();

	/** 71 - UTIL_COMP_DQ */
	MatrixMap3<Integer, String, String, Coefficients> getQuadMeanDiameterUtilizationComponentMap();

	/** 80 - SMALL_COMP_PROBABILITY */
	Map<String, Coefficients> getSmallComponentProbabilityCoefficients();

	/** 81 - SMALL_COMP_BA */
	Map<String, Coefficients> getSmallComponentBasalAreaCoefficients();

	/** 82 - SMALL_COMP_DQ */
	Map<String, Coefficients> getSmallComponentQuadMeanDiameterCoefficients();

	/** 85 - SMALL_COMP_HL */
	Map<String, Coefficients> getSmallComponentLoreyHeightCoefficients();

	/** 86 - SMALL_COMP_WS_VOLUME */
	Map<String, Coefficients> getSmallComponentWholeStemVolumeCoefficients();

	/** 90 - TOTAL_STAND_WHOLE_STEM_VOL */
	Map<Integer, Coefficients> getTotalStandWholeStepVolumeCoeMap();

	/** 91 - UTIL_COMP_WS_VOLUME */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getWholeStemUtilizationComponentMap();

	/** 92 - CLOSE_UTIL_VOLUME */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getCloseUtilizationCoeMap();

	/** 93 - VOLUME_NET_DECAY */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getNetDecayCoeMap();

	/** 94 - VOLUME_NET_DECAY_WASTE */
	Map<String, Coefficients> getNetDecayWasteCoeMap();

	/** 95 - BREAKAGE */
	Map<Integer, Coefficients> getNetBreakageMap();

	/** 108 - BA_DQ_UPPER_BOUNDS */
	Map<Integer, Coefficients> getUpperBounds();

	/** 198 - MODIFIER_FILE */
	MatrixMap2<String, Region, Float> getWasteModifierMap();

	/** 198 - MODIFIER_FILE */
	MatrixMap2<String, Region, Float> getDecayModifierMap();
}
