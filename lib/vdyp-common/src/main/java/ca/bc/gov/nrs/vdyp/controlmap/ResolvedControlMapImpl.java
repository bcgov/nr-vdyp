package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Collections;
import java.util.HashMap;
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

public class ResolvedControlMapImpl implements ResolvedControlMap {

	private final Map<String, Object> controlMap;

	private final Map<ControlKey, Object> entityCache = new HashMap<>();

	public ResolvedControlMapImpl(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
	}

	/**
	 * @return the underlying control map
	 */
	@Override
	public Map<String, Object> getControlMap() {
		return Collections.unmodifiableMap(controlMap);
	}

	@SuppressWarnings("unchecked")
	private <U> U get(ControlKey key, Class<? super U> clazz) {

		U entity = (U) entityCache.get(key);
		if (entity == null) {
			entity = (U) Utils.expectParsedControl(controlMap, key.name(), clazz);
			entityCache.put(key, entity);
		}

		return entity;
	}

	@Override
	public GenusDefinitionMap getGenusDefinitionMap() {
		GenusDefinitionMap genusDefinitions = this
				.<GenusDefinitionMap>get(ControlKey.SP0_DEF, GenusDefinitionMap.class);
		return genusDefinitions;
	}

	@Override
	public BecLookup getBecLookup() {
		return this.get(ControlKey.BEC_DEF, BecLookup.class);
	}

	@Override
	public Map<String, Coefficients> getNetDecayWasteCoeMap() {
		return this.get(ControlKey.VOLUME_NET_DECAY_WASTE, Map.class);
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getNetDecayCoeMap() {
		return this.get(ControlKey.VOLUME_NET_DECAY, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, Region, Float> getWasteModifierMap() {
		return this.get(ControlKey.WASTE_MODIFIERS, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, Region, Float> getDecayModifierMap() {
		return this.get(ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getCloseUtilizationCoeMap() {
		return this.get(ControlKey.CLOSE_UTIL_VOLUME, MatrixMap2.class);
	}

	@Override
	public Map<Integer, Coefficients> getTotalStandWholeStepVolumeCoeMap() {
		return this.get(ControlKey.TOTAL_STAND_WHOLE_STEM_VOL, Map.class);
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getWholeStemUtilizationComponentMap() {
		return this.get(ControlKey.UTIL_COMP_WS_VOLUME, MatrixMap2.class);
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getQuadMeanDiameterUtilizationComponentMap() {
		return this.get(ControlKey.UTIL_COMP_DQ, MatrixMap3.class);
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getBasalAreaDiameterUtilizationComponentMap() {
		return this.get(ControlKey.UTIL_COMP_BA, MatrixMap3.class);
	}

	@Override
	public Map<String, Coefficients> getSmallComponentWholeStemVolumeCoefficients() {
		return this.get(ControlKey.SMALL_COMP_WS_VOLUME, Map.class);
	}

	@Override
	public Map<String, Coefficients> getSmallComponentLoreyHeightCoefficients() {
		return this.get(ControlKey.SMALL_COMP_HL, Map.class);
	}

	@Override
	public Map<String, Coefficients> getSmallComponentQuadMeanDiameterCoefficients() {
		return this.get(ControlKey.SMALL_COMP_DQ, Map.class);
	}

	@Override
	public Map<String, Coefficients> getSmallComponentBasalAreaCoefficients() {
		return this.get(ControlKey.SMALL_COMP_BA, Map.class);
	}

	@Override
	public Map<String, Coefficients> getSmallComponentProbabilityCoefficients() {
		return this.get(ControlKey.SMALL_COMP_PROBABILITY, Map.class);
	}

	@Override
	public Map<Integer, SiteCurveAgeMaximum> getMaximumAgeBySiteCurveNumber() {
		return this.get(ControlKey.SITE_CURVE_AGE_MAX, Map.class);
	}

	@Override
	public Map<Integer, Coefficients> getUpperBounds() {
		return this.get(ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
	}

	@Override
	public MatrixMap2<String, String, Integer> getDefaultEquationGroup() {
		return this.get(ControlKey.DEFAULT_EQ_NUM, Map.class);
	}

	@Override
	public MatrixMap2<Integer, Integer, Integer> getEquationModifierGroup() {
		return this.get(ControlKey.BA_MODIFIERS, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl1Coefficients() {
		return this.get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl2Coefficients() {
		return this.get(ControlKey.HL_PRIMARY_SP_EQN_P2, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl3Coefficients() {
		return this.get(ControlKey.HL_PRIMARY_SP_EQN_P3, MatrixMap2.class);
	}

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getHlNonPrimaryCoefficients() {
		return this.get(ControlKey.HL_NONPRIMARY, MatrixMap3.class);
	}

	@Override
	public MatrixMap2<String, Region, ComponentSizeLimits> getComponentSizeLimits() {
		return this.get(ControlKey.SPECIES_COMPONENT_SIZE_LIMIT, MatrixMap2.class);
	}

	@Override
	public Map<Integer, Coefficients> getNetBreakageMap() {
		return this.get(ControlKey.BREAKAGE, Map.class);
	}

	@Override
	public MatrixMap2<String, String, Integer> getVolumeEquationGroups() {
		return this.get(ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, String, Integer> getDecayEquationGroups() {
		return this.get(ControlKey.DECAY_GROUPS, MatrixMap2.class);
	}

	@Override
	public MatrixMap2<String, String, Integer> getBreakageEquationGroups() {
		return this.get(ControlKey.BREAKAGE_GROUPS, MatrixMap2.class);
	}

	@Override
	public Map<String, Coefficients> getQuadMeanDiameterBySpeciesCoefficients() {
		return this.get(ControlKey.BY_SPECIES_DQ, Map.class);
	}
}
