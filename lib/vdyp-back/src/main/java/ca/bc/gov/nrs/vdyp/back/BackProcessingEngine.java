package ca.bc.gov.nrs.vdyp.back;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;

public class BackProcessingEngine extends ProcessingEngine<BackProcessingEngine.BackExecutionStep> {

	public enum BackExecutionStep implements ProcessingEngine.ExecutionStep<BackExecutionStep> {
		// Must be first
		NONE, //

		GROW, //

		// Must be last
		ALL; //

		@Override
		public BackExecutionStep predecessor() throws IllegalStateException {
			return Utils.predecessorOrThrow(this, BackExecutionStep.values());
		}

		@Override
		public BackExecutionStep successor() {
			return Utils.successorOrThrow(this, BackExecutionStep.values());
		}

	}

	/**
	 *
	 * @throws StandProcessingException
	 */
	// BACKPREP
	void prepare(BackProcessingState state) throws ProcessingException {

		// Copy the basal area for the veteran layer if it exists to the polygon state

		state.setBaseAreaVeteran(
				state.getVeteranLayerProcessingState().map(vetState -> vetState.getBank().basalAreas[0][0 + 1])
		);

		// Copy slices of the compatibility variables for the primary layer to the polygon state

		var primaryState = state.getPrimaryLayerProcessingState();

		int specCount = primaryState.getNSpecies();

		@SuppressWarnings("unchecked")
		MatrixMap2<UtilizationClass, UtilizationClassVariable, Float>[] cvVolume = new MatrixMap2[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClass, Float>[] cvBasalArea = new Map[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter = new Map[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall = new Map[specCount + 1];

		for (int i = 0; i < primaryState.getNSpecies(); i++) {
			final int specIndex = i + 1;
			cvVolume[specIndex] = new MatrixMap2Impl<>(
					List.of(UtilizationClass.values()), VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES,
					(uc, vv) -> primaryState.getCVVolume(specIndex, uc, vv, LayerType.PRIMARY)
			);

			cvBasalArea[specIndex] = new EnumMap<>(UtilizationClass.class);
			cvQuadraticMeanDiameter[specIndex] = new EnumMap<>(UtilizationClass.class);
			cvPrimaryLayerSmall[specIndex] = new EnumMap<>(UtilizationClassVariable.class);

			for (var uc : UtilizationClass.values()) {
				cvBasalArea[specIndex].put(uc, primaryState.getCVBasalArea(specIndex, uc, LayerType.PRIMARY));
				cvQuadraticMeanDiameter[specIndex]
						.put(uc, primaryState.getCVQuadraticMeanDiameter(specIndex, uc, LayerType.PRIMARY));
			}

			for (var ucv : VdypCompatibilityVariables.SMALL_UTILIZATION_VARIABLES) {
				cvPrimaryLayerSmall[specIndex].put(ucv, primaryState.getCVSmall(specIndex, ucv));
			}

		}

		state.setCompatibilityVariableDetails(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvPrimaryLayerSmall);

		Bank primaryBank = state.getPrimaryLayerProcessingState().getBank();
		Region polygonRegion = state.getCurrentBecZone().getRegion();

		ComponentSizeLimits[] limits = new ComponentSizeLimits[primaryState.getNSpecies() + 1];
		float[] finalDiameters = new float[primaryState.getNSpecies() + 1];

		int ucIndexAll = UtilizationClass.ALL.ordinal(); // Intentionally use ordinal instead of index.

		finalDiameters[0] = primaryBank.quadMeanDiameters[0][ucIndexAll];
		for (int i = 0; i < primaryState.getNSpecies(); i++) {
			final int specIndex = i + 1;

			finalDiameters[specIndex] = primaryBank.quadMeanDiameters[specIndex][0];

			final var originalLimits = state.getEstimators()
					.getLimitsForHeightAndDiameter(primaryBank.speciesNames[specIndex], polygonRegion);

			final float specQuadMeanDiameter = primaryBank.quadMeanDiameters[specIndex][ucIndexAll];
			final float specLoreyHeight = primaryBank.loreyHeights[specIndex][ucIndexAll];

			final float quadMeanDiameterLoreyHeightRatio = specQuadMeanDiameter / specLoreyHeight;

			final float loreyHeightMaximum = max(originalLimits.loreyHeightMaximum(), specLoreyHeight);
			final float quadMeanDiameterMaximum = max(originalLimits.quadMeanDiameterMaximum(), specQuadMeanDiameter);

			final float minQuadMeanDiameterLoreyHeightRatio = min(
					originalLimits.minQuadMeanDiameterLoreyHeightRatio(), quadMeanDiameterLoreyHeightRatio
			);
			final float maxQuadMeanDiameterLoreyHeightRatio = max(
					originalLimits.maxQuadMeanDiameterLoreyHeightRatio(), quadMeanDiameterLoreyHeightRatio
			);

			limits[specIndex] = new ComponentSizeLimits(
					loreyHeightMaximum, quadMeanDiameterMaximum, minQuadMeanDiameterLoreyHeightRatio,
					maxQuadMeanDiameterLoreyHeightRatio
			);

		}
		state.setLimits(limits);
		state.setFinalQuadMeanDiameters(finalDiameters);

	}

	@Override
	public void processPolygon(VdypPolygon polygon, BackExecutionStep lastStepInclusive) throws ProcessingException {
		// TODO Auto-generated method stub

	}

	@Override
	protected BackExecutionStep getFirstStep() {
		return BackExecutionStep.NONE;
	}

	@Override
	protected BackExecutionStep getLastStep() {
		return BackExecutionStep.ALL;
	}
}
