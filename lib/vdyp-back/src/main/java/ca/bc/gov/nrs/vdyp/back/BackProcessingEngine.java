package ca.bc.gov.nrs.vdyp.back;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackLayerProcessingState;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.sindex.Sindxdll;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CommonCalculatorException;

public class BackProcessingEngine extends ProcessingEngine<BackProcessingState, BackLayerProcessingState> {

	public BackProcessingEngine(BackProcessingState processingState) {
		super(processingState);
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
		MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume = new MatrixMap2[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClass, Float>[] cvBasalArea = new Map[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter = new Map[specCount + 1];
		@SuppressWarnings("unchecked")
		Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall = new Map[specCount + 1];

		for (int i = 0; i < primaryState.getNSpecies(); i++) {
			final int specIndex = i + 1;
			cvVolume[specIndex] = new MatrixMap2Impl<>(
					List.of(UtilizationClass.values()), List.of(VolumeVariable.values()),
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

			for (var ucv : UtilizationClassVariable.values()) {
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

	// BACKAPPL
	void applyBackupFactors(int currentYear /* IYRCUR */) throws ProcessingException {
		int layerIndex = 1;
		int instance = 2;
		final BackLayerProcessingState plps = getState().getPrimaryLayerProcessingState();
		final BecDefinition bec = plps.getPolygon().getBiogeoclimaticZone();

		final Bank bank = plps.getBank();
		final EstimationMethods estimators = getState().estimators;
		var primaryLayer = plps.getPolygon().getLayers().get(LayerType.PRIMARY);

		float regress = getState().getCurrentStartingYear() - currentYear;

		bank.yearsAtBreastHeight[0] -= regress; // AGEBHP
		bank.ageTotals[0] -= regress; // AGETOTP

		// SITEHADJ
		bank.dominantHeights[0] = heightFromSiteCurve(
				bank.siteCurveNumbers[0], bank.yearsAtBreastHeight[0], bank.yearsToBreastHeight[0], bank.siteIndices[0]
		);

		bank.dominantHeights[0] = FloatMath.offsetMultiply(
				bank.dominantHeights[0], getState().getConvergenceDominantHeight().get(),
				getState().getDominantHeightBackupFactor().get()
		);

		// BAYield = EMP106(...)
		var basalAreaYield = estimators.estimateBaseAreaYield(
				bank.dominantHeights[0], bank.yearsAtBreastHeight[0], getState().getBaseAreaVeteran(), true,
				primaryLayer.getSpecies().values(), primaryLayer.getPrimaryGenus().orElseThrow(), bec,
				primaryLayer.getEmpiricalRelationshipParameterIndex().orElseThrow()
		);

		// DQYield = EMP107(...)
		var quadraticMeanDiameterYield = estimators.estimateQuadMeanDiameterYield(
				bank.dominantHeights[0], bank.yearsAtBreastHeight[0], getState().getBaseAreaVeteran(),
				primaryLayer.getSpecies().values(), primaryLayer.getPrimaryGenus().orElseThrow(), bec,
				primaryLayer.getEmpiricalRelationshipParameterIndex().orElseThrow()
		);

		if (basalAreaYield < 0.995f * getState().getConvergenceBasalArea().orElseThrow()) {
			throw new ProcessingException(
					"basalAreaYield is low: " + basalAreaYield + " < "
							+ 0.995f * getState().getConvergenceBasalArea().orElseThrow()
			);
		}

		float bap = FloatMath.offsetMultiply(
				basalAreaYield, getState().getConvergenceBasalArea().orElseThrow(),
				getState().getBasalAreaBackupFactor().orElseThrow()
		);
		float dqp = FloatMath.offsetMultiply(
				basalAreaYield, getState().getConvergenceQuadraticMeanDiameter().orElseThrow(),
				getState().getQuadMeanDiameterBackupFactor().orElseThrow()
		);
		if (getState().getQuadMeanDiameterBackupFactor().orElseThrow() <= 0) {
			final float lowDQ = getState().getFinalQuadraticMeanDiameter(0) - regress;
			final float highDQ = getState().getFinalQuadraticMeanDiameter(0) + regress;

			dqp = FloatMath.clamp(dqp, lowDQ, highDQ);
		}
		dqp = max(dqp, getState().getQuadMeanDiameterBackupFactorMinimum().orElseThrow());

		// Lorey Height for ALL UC.

		// EMP051
		float hlpl1 = estimators.primaryHeightFromLeadHeightInitial(
				bank.dominantHeights[0], primaryLayer.getPrimaryGenus().orElseThrow(), bec.getRegion()
		);
		bank.loreyHeights[plps.getPrimarySpeciesIndex()][UC_ALL_INDEX] = hlpl1;

		for (int i : plps.getIndices()) {
			if (i == plps.getPrimarySpeciesIndex())
				continue;
			// EMP053
			bank.loreyHeights[i][UC_ALL_INDEX] = estimators.estimateNonPrimaryLoreyHeight(
					bank.speciesNames[i], primaryLayer.getPrimaryGenus().orElseThrow(), bec, bank.dominantHeights[0],
					hlpl1
			);
		}

		float sumBaHl = 0;
		for (int i : plps.getIndices()) {
			float bf = getState().getSpeciesLoreyHeightBackupFactor(i);
			if (bf == 1f)
				continue;
			bank.loreyHeights[i][UC_ALL_INDEX] = FloatMath.offsetMultiply(
					bank.loreyHeights[i][UC_ALL_INDEX], getState().getSpeciesConvergenceLoreyHeight(i),
					getState().getSpeciesLoreyHeightBackupFactor(i)
			);
			bank.loreyHeights[i][UC_ALL_INDEX] = min(
					bank.loreyHeights[i][UC_ALL_INDEX], getState().getSpeciesLoreyHeightBackupFactorMaximum(i)
			);

			sumBaHl += bank.loreyHeights[i][UC_ALL_INDEX] * bank.basalAreas[i][UC_ALL_INDEX];
		}
		bank.loreyHeights[0][UC_ALL_INDEX] = sumBaHl / bank.basalAreas[0][UC_ALL_INDEX];

		bank.basalAreas[0][UC_ALL_INDEX] = bap;
		bank.quadMeanDiameters[0][UC_ALL_INDEX] = bap;
		bank.treesPerHectare[0][UC_ALL_INDEX] = BaseAreaTreeDensityDiameter.treesPerHectare(bap, dqp);

		if (plps.getNSpecies() == 1) {
			bank.basalAreas[1][UC_ALL_INDEX] = bank.basalAreas[0][UC_ALL_INDEX];
			bank.quadMeanDiameters[1][UC_ALL_INDEX] = bank.quadMeanDiameters[0][UC_ALL_INDEX];
			bank.treesPerHectare[1][UC_ALL_INDEX] = bank.treesPerHectare[0][UC_ALL_INDEX];
		} else {

			// TODO this is slow and we might want to rework getDqBySpecies to avoid it.
			primaryLayer = bank.buildLayerFromBank();

		}

	}

	/**
	 * Get dominant height from site curve
	 *
	 * @throws ProcessingException
	 */
	// SITEHADJ
	public float heightFromSiteCurve(
			int siteCurveNumber, float yearsAtBreastHeight, float yearsToBreastHeight, float siteIndex
	) throws ProcessingException {
		if (siteCurveNumber < 0) {
			throw new IllegalArgumentException("siteCurveNumber was negative: " + siteCurveNumber);
		}
		final var maximumAgeBySiteCurveNumberMap = this.getState().getControlMap().getMaximumAgeBySiteCurveNumber();
		final var ageLimits = maximumAgeBySiteCurveNumberMap.get(siteCurveNumber);
		if (ageLimits == null) {
			throw new IllegalArgumentException("unknown siteCurveNumber: " + siteCurveNumber);
		}

		float breastHeightAgeToUse = yearsAtBreastHeight; // AGEBHUse
		final float totalAge = breastHeightAgeToUse + yearsToBreastHeight; // TAGE
		final float totalAgeLimit = ageLimits.getAgeMaximum(this.getState().getCurrentBecZone().getRegion()); // TAGELIM

		if (totalAge > totalAgeLimit) {
			breastHeightAgeToUse = totalAgeLimit - yearsToBreastHeight;
		}

		final var ageType = SiteIndexAgeType.SI_AT_BREAST;
		final var equation = SiteIndexEquation.getByIndex(siteCurveNumber);

		try {
			final double hdd = Sindxdll
					.AgeSIToHt(equation, breastHeightAgeToUse, ageType, siteIndex, yearsToBreastHeight); // HDD

			var ageDelta = yearsAtBreastHeight - (totalAgeLimit - yearsToBreastHeight); // DELT

			if (ageLimits.getT1() <= 0 || ageDelta <= 0) {
				return (float) hdd;
			} else {
				final double hddNext = Sindxdll
						.AgeSIToHt(equation, breastHeightAgeToUse + 1, ageType, siteIndex, yearsToBreastHeight);// HDD2
				final double rate = max(hddNext - hdd, 0.0005); // RATE0
				final var a = Math.log(0.5) / ageLimits.getT1(); // A
				ageDelta = Math.min(ageDelta, ageLimits.getT2());
				return (float) (hdd - rate / a * (1 - Math.exp(a * ageDelta)));
			}
		} catch (CommonCalculatorException e) {
			// TODO might want to be more specific
			throw new ProcessingException(e);
		}
	}
}
