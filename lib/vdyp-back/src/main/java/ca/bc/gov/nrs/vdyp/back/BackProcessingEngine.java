package ca.bc.gov.nrs.vdyp.back;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.offsetMultiply;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackLayerProcessingState;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.exceptions.BreastHeightAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.sindex.Sindxdll;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CommonCalculatorException;

public class BackProcessingEngine extends ProcessingEngine<BackProcessingState, BackLayerProcessingState> {

	private static final Logger logger = LoggerFactory.getLogger(BackProcessingEngine.class);

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
					(uc, vv) -> primaryState.getCVVolume(specIndex, uc, vv)
			);

			cvBasalArea[specIndex] = new EnumMap<>(UtilizationClass.class);
			cvQuadraticMeanDiameter[specIndex] = new EnumMap<>(UtilizationClass.class);
			cvPrimaryLayerSmall[specIndex] = new EnumMap<>(UtilizationClassVariable.class);

			for (var uc : UtilizationClass.values()) {
				cvBasalArea[specIndex].put(uc, primaryState.getCVBasalArea(specIndex, uc));
				cvQuadraticMeanDiameter[specIndex].put(uc, primaryState.getCVQuadraticMeanDiameter(specIndex, uc));
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
		state.setFinalQuadraticMeanDiameter(finalDiameters);

	}

	// BACKAPPL
	void applyBackupFactors(int currentYear /* IYRCUR */) throws ProcessingException {
		final BackLayerProcessingState plps = getState().getPrimaryLayerProcessingState();
		final BecDefinition bec = plps.getPolygon().getBiogeoclimaticZone();
		final var bank = plps.getBank();

		final EstimationMethods estimators = getState().getEstimators();

		final var primaryLayer = plps.getPolygon().requirePrimaryLayer();
		final var primarySpecies = primaryLayer.getPrimarySpeciesRecord()
				.orElseThrow(() -> new ProcessingException("No Primary species"));
		final var primarySite = primaryLayer.getPrimarySite()
				.orElseThrow(() -> new ProcessingException("No site information in primary species"));
		float regress = getState().getCurrentStartingYear() - currentYear;

		// AGEBHP
		final float yearsAtBreastHeight = primarySite.getYearsAtBreastHeight().map(y -> y - regress)
				.orElseThrow(() -> new ProcessingException("Missing age information in primary species"));
		primarySite.setYearsAtBreastHeight(yearsAtBreastHeight);
		// AGETOTP
		final float ageTotal = primarySite.getAgeTotal().map(y -> y - regress)
				.orElseThrow(() -> new ProcessingException("Missing age information in primary species"));
		primarySite.setAgeTotal(ageTotal);

		// SITEHADJ
		final float dominantHeight = FloatMath.offsetMultiply(
				heightFromSiteCurve(primarySite), //
				getState().getConvergenceDominantHeight().get(), //
				getState().getDominantHeightBackupFactor().get()
		);
		primarySite.setHeight(dominantHeight);

		float bap = applyBackFactorsToPrimaryBasalArea(bec, primaryLayer, yearsAtBreastHeight, dominantHeight);

		float dqp = applyBackupFactorsToPrimaryDiameter(
				bec, primaryLayer, regress, yearsAtBreastHeight, dominantHeight
		);

		// Lorey Height for ALL UC. for Primary species

		// EMP051
		float hlpl1 = estimators.primaryHeightFromLeadHeightInitial(
				dominantHeight, primaryLayer.getPrimaryGenus().orElseThrow(), bec.getRegion()
		);

		primarySpecies.getLoreyHeightByUtilization().setAll(hlpl1);

		// Lorey Height for ALL UC. for non-primary species

		for (var species : primaryLayer.getOrderedSpecies()) {
			if (species == primarySpecies)
				continue;
			// EMP053

			final float specLoreyHeight = estimators.estimateNonPrimaryLoreyHeight(
					species, //
					primarySpecies, //
					bec, dominantHeight, //
					hlpl1
			);
			species.getLoreyHeightByUtilization().setAll(specLoreyHeight);
		}

		// Lorey Height, apply backup factor

		float sumBaHl = 0;
		for (int i : plps.getIndices()) {
			float bf = getState().getSpeciesLoreyHeightBackupFactor(i);
			final var species = Utils.getSpeciesByIndexWithinLayer(primaryLayer, i);
			float speciesLoreyHeight;
			if (bf != 1f) {

				speciesLoreyHeight = min(
						FloatMath.offsetMultiply(
								species.getLoreyHeightByUtilization().getAll(), //
								getState().getSpeciesConvergenceLoreyHeight(i), //
								getState().getSpeciesLoreyHeightBackupFactor(i)
						), getState().getSpeciesLoreyHeightBackupFactorMaximum(i)
				);
				species.getLoreyHeightByUtilization().setAll(speciesLoreyHeight);
			} else {
				speciesLoreyHeight = species.getLoreyHeightByUtilization().getAll();
			}
			sumBaHl += speciesLoreyHeight * species.getBaseAreaByUtilization().getAll();
		}
		primaryLayer.getLoreyHeightByUtilization().setAll(sumBaHl / primaryLayer.getBaseAreaByUtilization().getAll());

		primaryLayer.getBaseAreaByUtilization().setAll(bap);
		primaryLayer.getQuadraticMeanDiameterByUtilization().setAll(dqp);
		BaseAreaTreeDensityDiameter.reconcileTreesPerHectare(primaryLayer, UtilizationClass.ALL);

		// Assign BA and DQ by species

		if (assignLayerValuesToSoleSpecies(primaryLayer)) {

			applyBackupFactorsSpeciesAreaAndDiameter(plps, bec, bank, primaryLayer, bap);

		}

	}

	protected void applyBackupFactorsSpeciesAreaAndDiameter(
			final BackLayerProcessingState plps, final BecDefinition bec, final Bank bank, final VdypLayer primaryLayer,
			float bap
	) throws FatalProcessingException {
		for (int i : plps.getIndices()) {
			var species = Utils.getSpeciesByIndexWithinLayer(primaryLayer, i);
			// Odd that this uses the bank rather than the percentage from main model data structure, but that's
			// what VDYP7 did
			species.getBaseAreaByUtilization().setAll(bap * bank.percentagesOfForestedLand[i] / 100);
		}

		// ROOTV01
		var dqLimits = getState().getComputers()
				.getDqBySpecies(primaryLayer, bec.getRegion(), getRootFinderLimits(primaryLayer));

		// Apply backup factors for DQ by species and calculate TPH

		float treesPerHectareSum = 0;

		for (int i : plps.getIndices()) {
			var species = Utils.getSpeciesByIndexWithinLayer(primaryLayer, i);
			if (getState().getSpeciesQuadMeanDiameterBackupFactor(i) > 0) {
				species.getQuadraticMeanDiameterByUtilization().scalarInPlace(
						UtilizationClass.ALL, v -> clamp(
								offsetMultiply(
										v, //
										getState().getSpeciesConvergenceQuadraticMeanDiameter(i), //
										getState().getSpeciesQuadMeanDiameterBackupFactor(i) //
								), dqLimits.minimum().get(species.getGenus()), //
								dqLimits.maximum().get(species.getGenus())
						)
				);
			} else {
				// We do not have a good backup factor for this species. Therefore, do not apply one.
				// But do put a cap on things that = actual at input year;

				final int yearDiff = getState().getCurrentStartingYear() - getState().getConvergenceYear().get();
				float slope = (getState().getSpeciesConvergenceQuadraticMeanDiameter(i) //
						- species.getQuadraticMeanDiameterByUtilization().getAll()) //
						/ yearDiff;
				float dqLimit = species.getQuadraticMeanDiameterByUtilization().getAll() + 2 * slope * yearDiff;
				species.getQuadraticMeanDiameterByUtilization()
						.scalarInPlace(UtilizationClass.ALL, v -> min(v, dqLimit));
			}

			species.getQuadraticMeanDiameterByUtilization().scalarInPlace(
					UtilizationClass.ALL, v -> max(v, getState().getSpeciesQuadMeanDiameterBackupFactorMinimum(i))
			);
			treesPerHectareSum += BaseAreaTreeDensityDiameter
					.reconcileTreesPerHectare(primaryLayer, UtilizationClass.ALL);
		}
		var err = treesPerHectareSum = primaryLayer.getTreesPerHectareByUtilization().getAll();

		if (Math.abs(err) > 0.5 && treesPerHectareSum > 0.5) {
			logger.warn(
					"Total TPH modified from {} to {}. Indicates a minor flaw in the system that could cause error in starting TPH. ",
					primaryLayer.getTreesPerHectareByUtilization().getAll(), treesPerHectareSum
			);
		}

		primaryLayer.getTreesPerHectareByUtilization().set(UtilizationClass.ALL, treesPerHectareSum);
		BaseAreaTreeDensityDiameter.reconcileQuadraticMeanDiameter(primaryLayer, UtilizationClass.ALL);
	}

	protected float applyBackupFactorsToPrimaryDiameter(
			final BecDefinition bec, final VdypLayer primaryLayer, float regress, final float yearsAtBreastHeight,
			final float dominantHeight
	) throws StandProcessingException {
		final EstimationMethods estimators = getState().getEstimators();
		// DQYield = EMP107(...)
		var quadraticMeanDiameterYield = estimators.estimateQuadMeanDiameterYield(
				dominantHeight, //
				yearsAtBreastHeight, //
				getState().getBaseAreaVeteran(), //
				primaryLayer.getSpecies().values(), //
				primaryLayer.getPrimaryGenus().orElseThrow(), //
				bec, //
				primaryLayer.getEmpiricalRelationshipParameterIndex().orElseThrow()
		);

		float dqp = FloatMath.offsetMultiply(
				quadraticMeanDiameterYield, //
				getState().getConvergenceQuadraticMeanDiameter().orElseThrow(), //
				getState().getQuadMeanDiameterBackupFactor().orElseThrow()
		);
		if (getState().getQuadMeanDiameterBackupFactor().orElseThrow() <= 0) {
			final float lowDQ = getState().getFinalQuadraticMeanDiameter(0) - regress;
			final float highDQ = getState().getFinalQuadraticMeanDiameter(0) + regress;

			dqp = FloatMath.clamp(dqp, lowDQ, highDQ);
		}
		dqp = max(dqp, getState().getQuadMeanDiameterBackupFactorMinimum().orElseThrow());
		return dqp;
	}

	protected float applyBackFactorsToPrimaryBasalArea(
			final BecDefinition bec, final VdypLayer primaryLayer, final float yearsAtBreastHeight,
			final float dominantHeight
	) throws BreastHeightAgeLowException, ProcessingException {
		final EstimationMethods estimators = getState().getEstimators();
		// BAYield = EMP106(...)
		var basalAreaYield = estimators.estimateBaseAreaYield(
				dominantHeight, //
				yearsAtBreastHeight, //
				getState().getBaseAreaVeteran(), //
				true, //
				primaryLayer.getSpecies().values(), //
				primaryLayer.getPrimaryGenus().orElseThrow(), //
				bec, //
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
		return bap;
	}

	public float heightFromSiteCurve(VdypSite site) throws ProcessingException {
		return heightFromSiteCurve(
				site.getSiteCurveNumber().orElseThrow(), site.getYearsAtBreastHeight().orElseThrow(),
				site.getYearsToBreastHeight().orElseThrow(), site.getSiteIndex().orElseThrow()
		);
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

	/**
	 * Calculate convergence age, basal area, year, and dominant height and store them in the state.
	 *
	 * @return years of regression
	 * @throws ProcessingException
	 */
	public int calculateConvergenceAge() throws ProcessingException {
		int startYear = getState().getCurrentStartingYear(); // IYRFIRST
		if (startYear <= 1600) {
			throw new ProcessingException("Start year " + startYear + "was too early.  Expected to be after 1600 CE.");
		}

		final VdypPolygon polygon = getState().getCurrentPolygon();
		var primaryLayer = polygon.requirePrimaryLayer();
		var primarySite = primaryLayer.getPrimarySite()
				.orElseThrow(() -> new ProcessingException("Primary layer has no site information"));

		/*
		 * The current age information is needed here for one purpose: establish fractional years, as we must go back an
		 * integer number of years.
		 */
		final Float yearsAtBreastHeight = primaryLayer.getYearsAtBreastHeight()
				.orElseThrow(() -> new ProcessingException("Years at breast height not set"));
		var yearFraction = yearsAtBreastHeight % 1f;
		if (yearFraction < 0.001)
			yearFraction = 0f;

		// Find an age that puts stand into suitable condition with EMP106 predicting reasonable BA
		float basalAreaTarget = 2f;
		float heightTarget = 9f;
		float ageTarget = 5f;

		int yearsToRegress = 0;
		int convergenceYear = 0;
		float convergenceHeight = 0;

		float yabh0 = ageTarget - 1f + yearFraction;
		float convergenceAge = 0;
		float basalArea = 0;
		var overstoryArea = Optional.ofNullable(polygon.getLayers().get(LayerType.VETERAN))
				.map(VdypLayer::getBaseAreaByUtilization).map(UtilizationVector::getAll);
		for (int increase = 0; increase < 1000; increase++) {
			float candiadateConvergenceAge = yabh0 + increase;
			if (candiadateConvergenceAge > yearsAtBreastHeight) {
				break;
			}
			float candiadateConvergenceHeight = heightFromSiteCurve(
					primarySite.getSiteCurveNumber().orElseThrow(), candiadateConvergenceAge,
					primarySite.getYearsToBreastHeight().orElseThrow(), primarySite.getSiteIndex().orElseThrow()
			);
			final String primarySpeciesId = primaryLayer.getPrimaryGenus()
					.orElseThrow(() -> new ProcessingException("Primary species group ID not set"));
			final Integer baseAreaGroup = primaryLayer.getEmpiricalRelationshipParameterIndex()
					.orElseThrow(() -> new ProcessingException("Empirical relationship parameter index not set"));
			basalArea = getState().getEstimators().estimateBaseAreaYield(
					candiadateConvergenceHeight, candiadateConvergenceAge, overstoryArea, true,
					primaryLayer.getOrderedSpecies(), primarySpeciesId, polygon.getBiogeoclimaticZone(), baseAreaGroup
			);
			if (candiadateConvergenceHeight >= heightTarget && candiadateConvergenceAge >= ageTarget
					&& basalArea >= basalAreaTarget) {
				convergenceHeight = candiadateConvergenceHeight;
				convergenceAge = candiadateConvergenceAge;
				yearsToRegress = (int) (yearsAtBreastHeight - convergenceAge);
				convergenceYear = startYear - yearsToRegress;
				getState().setConvergenceBasalArea(basalArea);
				break;
			}
		}
		getState().setConvergenceAge(convergenceAge);
		getState().setConvergenceYear(convergenceYear);
		getState().setConvergenceDominantHeight(convergenceHeight);
		return yearsToRegress;
	}

	/**
	 * Calculate yields at convergence and set the site values to convergence values
	 *
	 * @return years of regression
	 * @throws ProcessingException
	 */
	public void calculateConvergenceYield() throws ProcessingException {
		final VdypPolygon polygon = getState().getCurrentPolygon();
		final var primaryLayer = polygon.requirePrimaryLayer();
		final var primarySite = primaryLayer.getPrimarySite().orElseThrow();
		final String primarySpeciesGroupId = primaryLayer.getPrimaryGenus().orElseThrow();
		final VdypSpecies primarySpecies = primaryLayer.getPrimarySpeciesRecord().orElseThrow();
		final BecDefinition bec = polygon.getBiogeoclimaticZone();

		// VDYP7 is constantly messing with the global variables used to represent this data structure. In VDYP8 it
		// might be better to just duplicate the structure.

		// HD = HD_CNV, HDL1 = HD
		final float dominantHeight = getState().getConvergenceDominantHeight().orElseThrow();
		primarySite.setHeight(dominantHeight);
		// AGEBHP = AGE_CNV, AGEBHL1 = AGEBHP
		primarySite.setYearsAtBreastHeight(getState().getConvergenceAge());
		// AGETOTP = AGE_CNV + YTBHP, AGETOTL1 = AGETOTP
		primarySite.setAgeTotal(
				Utils.mapBoth(
						getState().getConvergenceAge(), primarySite.getYearsToBreastHeight(),
						(atBh, toBh) -> atBh + toBh
				)
		);

		// BFMAXHLI
		getState().setSpeciesLoreyHeightBackupFactorMaximum(
				mapTo1IndexedArray(primaryLayer.getOrderedSpecies(), e -> e.getLoreyHeightByUtilization().getAll())
		);

		// DQ_CNV = EMP107(...)
		float convergenceDiameter = getState().getEstimators().estimateQuadMeanDiameterYield(
				dominantHeight, getState().getConvergenceAge().orElseThrow(), getState().getBaseAreaVeteran(),
				primaryLayer.getOrderedSpecies(), primarySpeciesGroupId, bec,
				primaryLayer.getEmpiricalRelationshipParameterIndex().orElseThrow()
		);
		getState().setConvergenceQuadraticMeanDiameter(convergenceDiameter);

		// HLPL1 = EMP051(...)
		float primaryLoreyHeight = getState().getEstimators()
				.primaryHeightFromLeadHeightInitial(dominantHeight, primarySpeciesGroupId, bec.getRegion());
		primarySpecies.getLoreyHeightByUtilization().setAll(primaryLoreyHeight);

		// Lorey height for non-primary species
		for (var species : primaryLayer.getOrderedSpecies()) {
			// HLsp = EMP053(...)
			var specHeight = getState().getEstimators()
					.estimateNonPrimaryLoreyHeight(species, primarySpecies, bec, dominantHeight, primaryLoreyHeight);
			species.getLoreyHeightByUtilization().setAll(specHeight);
		}

		// Enforce maximum values for lorey height
		for (var species : primaryLayer.getOrderedSpecies()) {
			species.getLoreyHeightByUtilization().scalarInPlace(
					value -> min(value, getState().getSpeciesLoreyHeightBackupFactorMaximum(species.getGenus()))
			);
		}

		// Calculate overall lorey height
		float baHlSum = 0;
		for (var species : primaryLayer.getOrderedSpecies()) {
			baHlSum += species.getLoreyHeightByUtilization().getAll() * species.getBaseAreaByUtilization().getAll();
		}
		primaryLayer.getLoreyHeightByUtilization().setAll(baHlSum / primaryLayer.getBaseAreaByUtilization().getAll());

		// At this point we need to estimate DQ by species. If there are
		// multiple species, this requires reconciliation with
		// the overall DQ.

		getState().getConvergenceBasalArea().ifPresent(primaryLayer.getBaseAreaByUtilization()::setAll);
		getState().getConvergenceQuadraticMeanDiameter()
				.ifPresent(primaryLayer.getQuadraticMeanDiameterByUtilization()::setAll);
		BaseAreaTreeDensityDiameter.reconcileTreesPerHectare(primaryLayer, UtilizationClass.ALL);

		if (assignLayerValuesToSoleSpecies(primaryLayer)) {
			float layerBasalArea = primaryLayer.getBaseAreaByUtilization().getAll();
			for (var species : primaryLayer.getOrderedSpecies()) {
				species.getBaseAreaByUtilization().setAll(layerBasalArea * species.getPercentGenus() / 100);
			}

			// ROOTV01
			getState().getComputers().getDqBySpecies(primaryLayer, bec.getRegion(), getRootFinderLimits(primaryLayer));

		}

		getState().setSpeciesConvergenceLoreyHeight(
				mapTo1IndexedArray(primaryLayer.getOrderedSpecies(), e -> e.getLoreyHeightByUtilization().getAll())
		);
		getState().setSpeciesConvergenceQuadraticMeanDiameter(
				mapTo1IndexedArray(
						primaryLayer.getOrderedSpecies(), e -> e.getQuadraticMeanDiameterByUtilization().getAll()
				)
		);

	}

	protected BiFunction<String, Region, ComponentSizeLimits> getRootFinderLimits(final VdypLayer primaryLayer) {
		return (s, r) -> getState().getLimits(Utils.indexOfSpeciesWithinLayer(s, primaryLayer));
	}

	/**
	 * If the layer only has one species group, assign BA, DQ, and TPH for ALL class of the species group to match the
	 * layer and return false, otherwise return true.
	 *
	 * @param layer
	 * @return false if there is exactly one species, true otherwise.
	 */
	private boolean assignLayerValuesToSoleSpecies(VdypLayer layer) {
		if (layer.getOrderedSpecies().size() == 1) {
			var primarySpecies = layer.getOrderedSpecies().get(0);
			primarySpecies.getBaseAreaByUtilization().setAll(layer.getBaseAreaByUtilization().getAll());
			primarySpecies.getQuadraticMeanDiameterByUtilization()
					.setAll(layer.getQuadraticMeanDiameterByUtilization().getAll());
			primarySpecies.getTreesPerHectareByUtilization().setAll(layer.getTreesPerHectareByUtilization().getAll());
			return false;
		}
		return true;
	}

	/**
	 *
	 * @param <T>
	 * @param elements
	 * @param func
	 * @return
	 */
	<T> float[] mapTo1IndexedArray(List<T> elements, ToDoubleFunction<T> func) {
		float[] result = new float[elements.size() + 1];
		int i = 0;
		for (var element : elements) {
			i++;
			result[i] = (float) func.applyAsDouble(element);
		}
		return result;
	}

	public void calculateCompatibilityVariables(int currentYear /* IYRCUR */) {
		final BackLayerProcessingState plps = getState().getPrimaryLayerProcessingState();
		int startYear = getState().getCurrentStartingYear(); // IYRFIRST
		float fraction;
		if (currentYear >= startYear) {
			fraction = 1f;
		} else {
			int convergenceYear = getState().getConvergenceYear().orElseThrow(); // IYR_CNV
			fraction = 1.0f * (currentYear - convergenceYear) / (startYear - convergenceYear);
		}
		plps.setFractionalCompatibilityVariables(fraction);
	}
}
