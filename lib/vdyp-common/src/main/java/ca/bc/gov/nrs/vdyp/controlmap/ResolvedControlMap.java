package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.DoubleCoefficients;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariables;

public interface ResolvedControlMap {

	Map<String, Object> getControlMap();

	/** 9 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.BEC_DEF} */
	BecLookup getBecLookup();

	/** 10 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SP0_DEF} */
	GenusDefinitionMap getGenusDefinitionMap();

	/** 20 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.VOLUME_EQN_GROUPS} */
	MatrixMap2<String, String, Integer> getVolumeEquationGroups();

	/** 21 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.DECAY_GROUPS} */
	MatrixMap2<String, String, Integer> getDecayEquationGroups();

	/** 22 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.BREAKAGE_GROUPS} */
	MatrixMap2<String, String, Integer> getBreakageEquationGroups();

	/** 26 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SITE_CURVE_AGE_MAX} */
	Map<Integer, SiteCurveAgeMaximum> getMaximumAgeBySiteCurveNumber();

	/** 30 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.DEFAULT_EQ_NUM} */
	MatrixMap2<String, String, Integer> getDefaultEquationGroup();

	/** 31 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.EQN_MODIFIERS} */
	MatrixMap2<Integer, Integer, Integer> getEquationModifierGroup();

	/** 40 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.COE_BA} */
	MatrixMap2<String, String, Coefficients> getBasalAreaCoefficients();

	/** 41 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.COE_DQ} */
	MatrixMap2<String, String, Coefficients> getQuadMeanDiameterCoefficients();

	/** 43 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.UPPER_BA_BY_CI_S0_P} */
	MatrixMap3<Region, String, Integer, Float> getUpperBoundsCoefficients();

	/** 50 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.HL_PRIMARY_SP_EQN_P1} */
	MatrixMap2<String, Region, Coefficients> getHl1Coefficients();

	/** 51 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.HL_PRIMARY_SP_EQN_P2} */
	MatrixMap2<String, Region, Coefficients> getHl2Coefficients();

	/** 52 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.HL_PRIMARY_SP_EQN_P3} */
	MatrixMap2<String, Region, Coefficients> getHl3Coefficients();

	/** 53 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.HL_NONPRIMARY} */
	MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getHlNonPrimaryCoefficients();

	/** 60 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.BY_SPECIES_DQ} */
	Map<String, DoubleCoefficients> getQuadMeanDiameterBySpeciesCoefficients();

	/** 61 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SPECIES_COMPONENT_SIZE_LIMIT} */
	MatrixMap2<String, Region, ComponentSizeLimits> getComponentSizeLimits();

	/** 70 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.UTIL_COMP_BA} */
	MatrixMap3<Integer, String, String, Coefficients> getBasalAreaDiameterUtilizationComponentMap();

	/** 71 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.UTIL_COMP_DQ} */
	MatrixMap3<Integer, String, String, Coefficients> getQuadMeanDiameterUtilizationComponentMap();

	/** 80 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SMALL_COMP_PROBABILITY} */
	Map<String, Coefficients> getSmallComponentProbabilityCoefficients();

	/** 81 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SMALL_COMP_BA} */
	Map<String, Coefficients> getSmallComponentBasalAreaCoefficients();

	/** 82 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SMALL_COMP_DQ} */
	Map<String, Coefficients> getSmallComponentQuadMeanDiameterCoefficients();

	/** 85 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SMALL_COMP_HL} */
	Map<String, Coefficients> getSmallComponentLoreyHeightCoefficients();

	/** 86 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.SMALL_COMP_WS_VOLUME} */
	Map<String, Coefficients> getSmallComponentWholeStemVolumeCoefficients();

	/** 90 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.TOTAL_STAND_WHOLE_STEM_VOL} */
	Map<Integer, Coefficients> getTotalStandWholeStepVolumeCoeMap();

	/** 91 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.UTIL_COMP_WS_VOLUME} */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getWholeStemUtilizationComponentMap();

	/** 92 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.CLOSE_UTIL_VOLUME} */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getCloseUtilizationCoeMap();

	/** 93 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.VOLUME_NET_DECAY} */
	MatrixMap2<Integer, Integer, Optional<Coefficients>> getNetDecayCoeMap();

	/** 94 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.VOLUME_NET_DECAY_WASTE} */
	Map<String, Coefficients> getNetDecayWasteCoeMap();

	/** 95 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.BREAKAGE} */
	Map<Integer, Coefficients> getNetBreakageMap();

	/** 97 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.VETERAN_LAYER_DQ} */
	MatrixMap2<String, Region, Coefficients> getVeteranQuadMeanDiameterCoefficients();

	/** 107 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.DQ_YIELD} */
	MatrixMap2<String, String, Coefficients> getQuadMeanDiameterYieldCoefficients();

	/** 108 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.BA_DQ_UPPER_BOUNDS} */
	Map<Integer, Coefficients> getUpperBounds();

	/** 198 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.MODIFIER_FILE} */
	MatrixMap2<String, Region, Float> getWasteModifierMap();

	/** 198 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.MODIFIER_FILE} */
	MatrixMap2<String, Region, Float> getDecayModifierMap();

	/** 198 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.MODIFIER_FILE} */
	MatrixMap2<String, Region, Float> getQuadMeanDiameterModifiers();

	/** 198 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.MODIFIER_FILE} */
	MatrixMap2<String, Region, Float> getBasalAreaModifiers();

	/** 199 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.DEBUG_SWITCHES} */
	DebugSettings getDebugSettings();

	/** 101 - {@link ca.bc.gov.nrs.vdyp.common.ControlKey.VTROL} */
	ControlVariables getControlVariables();

}
