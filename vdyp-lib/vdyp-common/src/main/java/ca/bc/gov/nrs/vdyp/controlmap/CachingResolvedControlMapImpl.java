package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;

public class CachingResolvedControlMapImpl implements ResolvedControlMap {

	private final BecLookup becLookup;
	private final GenusDefinitionMap genusDefinitionMap;
	private final Map<String, Coefficients> netDecayWasteCoeMap;
	private final MatrixMap2<Integer, Integer, Optional<Coefficients>> netDecayCoeMap;
	private final MatrixMap2<String, Region, Float> wasteModifierMap;
	private final MatrixMap2<String, Region, Float> decayModifierMap;
	private final MatrixMap2<Integer, Integer, Optional<Coefficients>> closeUtilizationCoeMap;
	private final Map<Integer, Coefficients> totalStandWholeStepVolumeCoeMap;
	private final MatrixMap2<Integer, Integer, Optional<Coefficients>> wholeStemUtilizationComponentMap;
	private final MatrixMap3<Integer, String, String, Coefficients> quadMeanDiameterUtilizationComponentMap;
	private final MatrixMap3<Integer, String, String, Coefficients> basalAreaDiameterUtilizationComponentMap;
	private final Map<String, Coefficients> smallComponentWholeStemVolumeCoefficients;
	private final Map<String, Coefficients> smallComponentLoreyHeightCoefficients;
	private final Map<String, Coefficients> smallComponentQuadMeanDiameterCoefficients;
	private final Map<String, Coefficients> smallComponentBasalAreaCoefficients;
	private final Map<String, Coefficients> smallComponentProbabilityCoefficients;
	private final Map<Integer, SiteCurveAgeMaximum> maximumAgeBySiteCurveNumber;
	private final Map<Integer, Coefficients> upperBounds;
	private final MatrixMap2<String, String, Integer> defaultEquationGroup;
	private final MatrixMap2<Integer, Integer, Optional<Integer>> equationModifierGroup;
	private final MatrixMap2<String, Region, Coefficients> hl1Coefficients;
	private final MatrixMap2<String, Region, Coefficients> hl2Coefficients;
	private final MatrixMap2<String, Region, Coefficients> hl3Coefficients;
	private final MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> hlNonPrimaryCoefficients;
	private final MatrixMap2<String, Region, ComponentSizeLimits> componentSizeLimitCoefficients;
	private final Map<Integer, Coefficients> breakageMap;
	private final MatrixMap2<String, String, Integer> volumeEquationGroups;
	private final MatrixMap2<String, String, Integer> decayEquationGroups;
	private final MatrixMap2<String, String, Integer> breakageEquationGroups;
	private final Map<String, Coefficients> quadMeanDiameterBySpeciesCoefficients;

	private final Map<String, Object> controlMap;

	public CachingResolvedControlMapImpl(Map<String, Object> controlMap) {

		this.controlMap = controlMap;

		this.becLookup = this.get(ControlKey.BEC_DEF, BecLookup.class);

		this.genusDefinitionMap = this.get(ControlKey.SP0_DEF, GenusDefinitionMap.class);
		this.netDecayWasteCoeMap = this.get(ControlKey.VOLUME_NET_DECAY_WASTE, Map.class);
		this.netDecayCoeMap = this.get(ControlKey.VOLUME_NET_DECAY, MatrixMap2.class);
		this.wasteModifierMap = this.get(ControlKey.WASTE_MODIFIERS, MatrixMap2.class);
		this.decayModifierMap = this.get(ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class);
		this.closeUtilizationCoeMap = this.get(ControlKey.CLOSE_UTIL_VOLUME, MatrixMap2.class);
		this.totalStandWholeStepVolumeCoeMap = this.get(ControlKey.TOTAL_STAND_WHOLE_STEM_VOL, Map.class);
		this.wholeStemUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_WS_VOLUME, MatrixMap2.class);
		this.quadMeanDiameterUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_DQ, MatrixMap3.class);
		this.basalAreaDiameterUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_BA, MatrixMap3.class);
		this.smallComponentWholeStemVolumeCoefficients = this.get(ControlKey.SMALL_COMP_WS_VOLUME, Map.class);
		this.smallComponentLoreyHeightCoefficients = this.get(ControlKey.SMALL_COMP_HL, Map.class);
		this.smallComponentQuadMeanDiameterCoefficients = this.get(ControlKey.SMALL_COMP_DQ, Map.class);
		this.smallComponentBasalAreaCoefficients = this.get(ControlKey.SMALL_COMP_BA, Map.class);
		this.smallComponentProbabilityCoefficients = this.get(ControlKey.SMALL_COMP_PROBABILITY, Map.class);
		this.maximumAgeBySiteCurveNumber = this.get(ControlKey.SITE_CURVE_AGE_MAX, Map.class);
		this.defaultEquationGroup = this.get(ControlKey.DEFAULT_EQ_NUM, MatrixMap2.class);
		this.hl1Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
		this.hl2Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P2, MatrixMap2.class);
		this.hl3Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P3, MatrixMap2.class);
		this.hlNonPrimaryCoefficients = this.get(ControlKey.HL_NONPRIMARY, MatrixMap3.class);
		this.upperBounds = this.get(ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
		this.equationModifierGroup = this.get(ControlKey.EQN_MODIFIERS, MatrixMap2.class);
		this.componentSizeLimitCoefficients = this.get(ControlKey.SPECIES_COMPONENT_SIZE_LIMIT, MatrixMap2.class);
		this.breakageMap = this.get(ControlKey.BREAKAGE, Map.class);
		this.volumeEquationGroups = this.get(ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class);
		this.decayEquationGroups = this.get(ControlKey.DECAY_GROUPS, MatrixMap2.class);
		this.breakageEquationGroups = this.get(ControlKey.BREAKAGE_GROUPS, MatrixMap2.class);
		this.quadMeanDiameterBySpeciesCoefficients = this.get(ControlKey.BY_SPECIES_DQ, Map.class);
	}

	/**
	 * @return the underlying control map
	 */
	@Override
	public Map<String, Object> getControlMap() {
		return Collections.unmodifiableMap(controlMap);
	}

	protected <U> U get(ControlKey key, Class<? super U> clazz) {
		return Utils.expectParsedControl(controlMap, key, clazz);
	}

	@Override
	public BecLookup getBecLookup() {
		return becLookup;
	}

	@Override
	public GenusDefinitionMap getGenusDefinitionMap() {
		return genusDefinitionMap;
	}

	@Override
	public Map<String, Coefficients> getNetDecayWasteCoeMap() {
		return netDecayWasteCoeMap;
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getNetDecayCoeMap() {
		return netDecayCoeMap;
	}

	@Override
	public MatrixMap2<String, Region, Float> getWasteModifierMap() {
		return wasteModifierMap;
	}

	@Override
	public MatrixMap2<String, Region, Float> getDecayModifierMap() {
		return decayModifierMap;
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getCloseUtilizationCoeMap() {
		return closeUtilizationCoeMap;
	}

	@Override
	public Map<Integer, Coefficients> getTotalStandWholeStepVolumeCoeMap() {
		return totalStandWholeStepVolumeCoeMap;
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getWholeStemUtilizationComponentMap() {
		return wholeStemUtilizationComponentMap;
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getQuadMeanDiameterUtilizationComponentMap() {
		return quadMeanDiameterUtilizationComponentMap;
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getBasalAreaDiameterUtilizationComponentMap() {
		return basalAreaDiameterUtilizationComponentMap;
	}

	@Override
	public Map<String, Coefficients> getSmallComponentWholeStemVolumeCoefficients() {
		return smallComponentWholeStemVolumeCoefficients;
	}

	@Override
	public Map<String, Coefficients> getSmallComponentLoreyHeightCoefficients() {
		return smallComponentLoreyHeightCoefficients;
	}

	@Override
	public Map<String, Coefficients> getSmallComponentQuadMeanDiameterCoefficients() {
		return smallComponentQuadMeanDiameterCoefficients;
	}

	@Override
	public Map<String, Coefficients> getSmallComponentBasalAreaCoefficients() {
		return smallComponentBasalAreaCoefficients;
	}

	@Override
	public Map<String, Coefficients> getSmallComponentProbabilityCoefficients() {
		return smallComponentProbabilityCoefficients;
	}

	@Override
	public Map<Integer, SiteCurveAgeMaximum> getMaximumAgeBySiteCurveNumber() {
		return maximumAgeBySiteCurveNumber;
	}

	@Override
	public Map<Integer, Coefficients> getUpperBounds() {
		return upperBounds;
	}

	@Override
	public MatrixMap2<String, String, Integer> getDefaultEquationGroup() {
		return defaultEquationGroup;
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Integer>> getEquationModifierGroup() {
		return equationModifierGroup;
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl1Coefficients() {
		return hl1Coefficients;
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl2Coefficients() {
		return hl2Coefficients;
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl3Coefficients() {
		return hl3Coefficients;
	}

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getHlNonPrimaryCoefficients() {
		return hlNonPrimaryCoefficients;
	}

	@Override
	public MatrixMap2<String, Region, ComponentSizeLimits> getComponentSizeLimits() {
		return componentSizeLimitCoefficients;
	}

	@Override
	public Map<Integer, Coefficients> getNetBreakageMap() {
		return breakageMap;
	}

	@Override
	public MatrixMap2<String, String, Integer> getVolumeEquationGroups() {
		return volumeEquationGroups;
	}

	@Override
	public MatrixMap2<String, String, Integer> getDecayEquationGroups() {
		return decayEquationGroups;
	}

	@Override
	public MatrixMap2<String, String, Integer> getBreakageEquationGroups() {
		return breakageEquationGroups;
	}

	@Override
	public Map<String, Coefficients> getQuadMeanDiameterBySpeciesCoefficients() {
		return quadMeanDiameterBySpeciesCoefficients;
	}
}
