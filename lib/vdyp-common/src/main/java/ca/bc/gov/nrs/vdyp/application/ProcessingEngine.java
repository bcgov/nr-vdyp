package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.log;

import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.ReconcilationMethods;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public abstract class ProcessingEngine<C extends ResolvedControlMap, S extends ProcessingState<C, L>, L extends LayerProcessingState<C, L>, E extends ProcessingEngine.ExecutionStep<E>> {

	public static final float[] DEFAULT_QUAD_MEAN_DIAMETERS = new float[] { Float.NaN, 10.0f, 15.0f, 20.0f, 25.0f };
	public static final float V_BASE_MIN = 0.1f;
	public static final float B_BASE_MIN = 0.01f;
	public static float calculateCompatibilityVariable(float actualVolume, float baseVolume, float staticVolume) {
	
		float staticRatio = staticVolume / baseVolume;
		float staticLogit;
		if (staticRatio <= 0.0f) {
			staticLogit = -7.0f;
		} else if (staticRatio >= 1.0f) {
			staticLogit = 7.0f;
		} else {
			staticLogit = clamp(log(staticRatio / (1.0f - staticRatio)), -7.0f, 7.0f);
		}
	
		float actualRatio = actualVolume / baseVolume;
		float actualLogit;
		if (actualRatio <= 0.0f) {
			actualLogit = -7.0f;
		} else if (actualRatio >= 1.0f) {
			actualLogit = 7.0f;
		} else {
			actualLogit = clamp(log(actualRatio / (1.0f - actualRatio)), -7.0f, 7.0f);
		}
	
		return actualLogit - staticLogit;
	}

	public S fps;

	protected ProcessingEngine(Map<String, Object> controlMap) throws ProcessingException {

		this.fps = getState(controlMap);
	}

	protected abstract S getState(Map<String, Object> controlMap) throws ProcessingException;

	/**
	 * Run all steps of the engine on the given polygon up to and including the given <code>lastStep</code>.
	 *
	 * @param polygon           the polygon on which to operate
	 * @param lastStepInclusive execute up to and including this step
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public abstract void processPolygon(VdypPolygon polygon, E lastStepInclusive) throws ProcessingException;

	public static interface ExecutionStep<E extends ExecutionStep<E>> extends Comparable<E> {

		/**
		 * @return The previous execution step
		 * @throws IllegalStateException if this is the first step
		 */
		public E predecessor() throws IllegalStateException;

		/**
		 * @return The next execution step
		 * @throws IllegalStateException if this is the last step
		 */
		public E successor() throws IllegalStateException;

		public default boolean lt(E that) {
			return this.compareTo(that) < 0;
		}

		public default boolean le(E that) {
			return this.compareTo(that) <= 0;
		}

		public default boolean eq(E that) {
			return this.compareTo(that) == 0;
		}

		public default boolean ge(E that) {
			return this.compareTo(that) >= 0;
		}

		public default boolean gt(E that) {
			return this.compareTo(that) > 0;

		}
	}

	protected abstract E getFirstStep();

	protected abstract E getLastStep();

	/**
	 * Run all steps of the engine on the given polygon.
	 *
	 * @param polygon the polygon on which to operate
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public void processPolygon(VdypPolygon polygon) throws ProcessingException {

		processPolygon(polygon, getLastStep());
	}

	/**
	 * CVSET1 - computes cvVolume, cvBasalArea, cvQuadraticMeanDiameter and cvSmall and assigns them to the current
	 * LayerProcessingState.
	 *
	 * @throws ProcessingException
	 */
	@SuppressWarnings("unchecked")
	protected void setCompatibilityVariables() throws ProcessingException {
	
		Coefficients aAdjust = new Coefficients(new float[] { 0.0f, 0.0f, 0.0f, 0.0f }, 1);
	
		var growthDetails = fps.getControlMap().getForwardControlVariables();
		var lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();
	
		// Note: L1COM2 (INL1VGRP, INL1DGRP, INL1BGRP) is initialized when
		// PolygonProcessingState (volumeEquationGroups, decayEquationGroups
		// breakageEquationGroups, respectively) is constructed. Copying
		// the values into LCOM1 is not necessary. Note, however, that
		// VolumeEquationGroup 10 is mapped to 11 (VGRPFIND) - this is done
		// when volumeEquationGroups is built (i.e., when the equivalent to
		// INL1VGRP is built, rather than when LCOM1 VGRPL is built in the
		// original code.)
	
		var cvVolume = new MatrixMap3[lps.getNSpecies() + 1];
		var cvBasalArea = new MatrixMap2[lps.getNSpecies() + 1];
		var cvQuadraticMeanDiameter = new MatrixMap2[lps.getNSpecies() + 1];
		var cvSmall = new HashMap[lps.getNSpecies() + 1];
	
		for (int s : lps.getIndices()) {
	
			String genusName = bank.speciesNames[s];
	
			float spLoreyHeight_All = bank.loreyHeights[s][UtilizationClass.ALL.ordinal()];
	
			UtilizationVector basalAreas = Utils.utilizationVector();
			UtilizationVector wholeStemVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecay = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecayAndWaste = Utils.utilizationVector();
			UtilizationVector quadMeanDiameters = Utils.utilizationVector();
			UtilizationVector treesPerHectare = Utils.utilizationVector();
	
			cvVolume[s] = new MatrixMap3Impl<UtilizationClass, UtilizationClassVariable, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES,
					LayerType.ALL_USED, (k1, k2, k3) -> 0f
			);
			cvBasalArea[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);
			cvQuadraticMeanDiameter[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);
	
			for (UtilizationClass uc : UtilizationClass.ALL_BUT_SMALL) {
	
				basalAreas.setCoe(uc.index, bank.basalAreas[s][uc.ordinal()]);
				wholeStemVolumes.setCoe(uc.index, bank.wholeStemVolumes[s][uc.ordinal()]);
				closeUtilizationVolumes.setCoe(uc.index, bank.closeUtilizationVolumes[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecay.setCoe(uc.index, bank.cuVolumesMinusDecay[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecayAndWaste
						.setCoe(uc.index, bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()]);
	
				quadMeanDiameters.setCoe(uc.index, bank.quadMeanDiameters[s][uc.ordinal()]);
				if (uc != UtilizationClass.ALL && quadMeanDiameters.getCoe(uc.index) <= 0.0f) {
					quadMeanDiameters.setCoe(uc.index, DEFAULT_QUAD_MEAN_DIAMETERS[uc.ordinal()]);
				}
			}
	
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
	
				float adjustment;
				float baseVolume;
	
				// Volume less decay and waste
				adjustment = 0.0f;
				baseVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];
	
				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {
	
					// EMP094
					fps.getEstimators().estimateNetDecayAndWasteVolume(
							lps.getBecZone().getRegion(), uc, aAdjust, bank.speciesNames[s], spLoreyHeight_All,
							quadMeanDiameters, closeUtilizationVolumes, closeUtilizationVolumesNetOfDecay,
							closeUtilizationVolumesNetOfDecayAndWaste
					);
	
					float actualVolume = bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecayAndWaste.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}
	
				cvVolume[s].put(
						uc, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY,
						adjustment
				);
	
				// Volume less decay
				adjustment = 0.0f;
				baseVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];
	
				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {
	
					// EMP093
					int decayGroup = lps.getDecayEquationGroups()[s];
					fps.getEstimators().estimateNetDecayVolume(
							bank.speciesNames[s], lps.getBecZone().getRegion(), uc, aAdjust, decayGroup,
							lps.getPrimarySpeciesAgeAtBreastHeight(), quadMeanDiameters, closeUtilizationVolumes,
							closeUtilizationVolumesNetOfDecay
					);
	
					float actualVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecay.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}
	
				cvVolume[s].put(uc, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY, adjustment);
	
				// Volume
				adjustment = 0.0f;
				baseVolume = bank.wholeStemVolumes[s][uc.ordinal()];
	
				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {
	
					// EMP092
					int volumeGroup = lps.getVolumeEquationGroups()[s];
					fps.getEstimators().estimateCloseUtilizationVolume(
							uc, aAdjust, volumeGroup, spLoreyHeight_All, quadMeanDiameters, wholeStemVolumes,
							closeUtilizationVolumes
					);
	
					float actualVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumes.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}
	
				cvVolume[s].put(uc, UtilizationClassVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, adjustment);
			}
	
			int primarySpeciesVolumeGroup = lps.getVolumeEquationGroups()[s];
			float primarySpeciesQMDAll = bank.quadMeanDiameters[s][UC_ALL_INDEX];
			var wholeStemVolume = bank.treesPerHectare[s][UC_ALL_INDEX] * fps.getEstimators()
					.estimateWholeStemVolumePerTree(primarySpeciesVolumeGroup, spLoreyHeight_All, primarySpeciesQMDAll);
	
			wholeStemVolumes.setCoe(UC_ALL_INDEX, wholeStemVolume);
	
			fps.getEstimators().estimateWholeStemVolume(
					UtilizationClass.ALL, 0.0f, primarySpeciesVolumeGroup, spLoreyHeight_All, quadMeanDiameters,
					basalAreas, wholeStemVolumes
			);
	
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float adjustment = 0.0f;
				float basalArea = basalAreas.getCoe(uc.index);
				if (growthDetails.allowCalculation(basalArea, B_BASE_MIN, (l, r) -> l > r)) {
					adjustment = calculateWholeStemVolume(
							bank.wholeStemVolumes[s][uc.ordinal()], basalArea, wholeStemVolumes.getCoe(uc.index)
					);
				}
	
				cvVolume[s].put(uc, UtilizationClassVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, adjustment);
			}
	
			fps.getEstimators().estimateQuadMeanDiameterByUtilization(lps.getBecZone(), quadMeanDiameters, genusName);
	
			fps.getEstimators()
					.estimateBaseAreaByUtilization(lps.getBecZone(), quadMeanDiameters, basalAreas, genusName);
	
			// Calculate trees-per-hectare per utilization
			treesPerHectare.setCoe(UtilizationClass.ALL.index, bank.treesPerHectare[s][UC_ALL_INDEX]);
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				treesPerHectare.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreas.getCoe(uc.index), quadMeanDiameters.getCoe(uc.index))
				);
			}
	
			ReconcilationMethods.reconcileComponents(basalAreas, treesPerHectare, quadMeanDiameters);
	
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float baCvValue = bank.basalAreas[s][uc.ordinal()] - basalAreas.getCoe(uc.index);
				cvBasalArea[s].put(uc, LayerType.PRIMARY, baCvValue);
	
				float originalQmd = bank.quadMeanDiameters[s][uc.ordinal()];
				float adjustedQmd = quadMeanDiameters.getCoe(uc.index);
	
				float qmdCvValue;
				if (growthDetails.allowCalculation(() -> originalQmd < B_BASE_MIN)) {
					qmdCvValue = 0.0f;
				} else if (originalQmd > 0 && adjustedQmd > 0) {
					qmdCvValue = originalQmd - adjustedQmd;
				} else {
					qmdCvValue = 0.0f;
				}
	
				cvQuadraticMeanDiameter[s].put(uc, LayerType.PRIMARY, qmdCvValue);
			}
	
			// Small components
	
			cvSmall[s] = calculateSmallCompatibilityVariables(s, growthDetails);
		}
	
		lps.setCompatibilityVariableDetails(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvSmall);
	}

}
