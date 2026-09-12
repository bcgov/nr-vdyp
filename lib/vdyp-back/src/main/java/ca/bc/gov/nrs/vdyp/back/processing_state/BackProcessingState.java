package ca.bc.gov.nrs.vdyp.back.processing_state;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public class BackProcessingState extends ProcessingState<BackLayerProcessingState> {

	Optional<Float> baseAreaVeteran = Optional.empty(); // BACK1/BAV

	private static final String COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY = "CompatibilityVariablesSet can be set once only";

	private static final Supplier<IllegalStateException> UNSET_CV_VOLUMES = unset("cvVolumes");
	private static final Supplier<IllegalStateException> UNSET_CV_BASAL_AREAS = unset("cvBasalAreas");
	private static final Supplier<IllegalStateException> UNSET_CV_QUAD_MEAN_DIAMETER = unset("cvQuadraticMeanDiameter");
	private static final Supplier<IllegalStateException> UNSET_CV_SMALL = unset("cvSmall");
	private static final Supplier<IllegalStateException> UNSET_LIMITS = unset("per species limits");

	// Compatibility Variables - LCV1 & LCVS
	private boolean areCompatibilityVariablesSet = false;

	private MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume;
	private Map<UtilizationClass, Float>[] cvBasalArea;
	private Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter;
	private Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall;

	private Optional<Integer> convergenceYear = Optional.empty(); // BACK2/IYR_CNV
	private Optional<Float> convergenceAge = Optional.empty(); // BACK2/AGE_CNV
	private Optional<Float> convergenceDominantHeight = Optional.empty(); // BACK2/HD_CNV
	private Optional<Float> convergenceBasalArea = Optional.empty(); // BACK2/BA_CNV

	private Optional<Float> convergenceQuadraticMeanDiameter = Optional.empty(); // BACK3/DQ_CNV
	private Optional<float[]> speciesConvergenceLoreyHeight = Optional.empty(); // BACK3/HLI_CNV
	private Optional<float[]> speciesConvergenceQuadraticMeanDiameter = Optional.empty(); // BACK3/DQI_CNV

	private Optional<ComponentSizeLimits[]> speciesLimits = Optional.empty(); // BACK7/...

	private Optional<float[]> finalQuadraticMeanDiameter = Optional.empty(); // BACK8/DQFinal

	private Optional<Float> dominantHeightBackupFactor = Optional.empty(); // BACK5/BFH
	private Optional<Float> basalAreaBackupFactor = Optional.empty(); // BACK5/BFB
	private Optional<Float> quadMeanDiameterBackupFactor = Optional.empty(); // BACK5/BFDQ

	private Optional<float[]> speciesLoreyHeightBackupFactor = Optional.empty(); // BACK5/BFLHI
	private Optional<float[]> speciesQuadMeanDiameterBackupFactor = Optional.empty(); // BACK5/BFDQI

	private Optional<Float> quadMeanDiameterBackupFactorMinimum = Optional.empty(); // BACK5/BFMINDQ
	private Optional<float[]> speciesQuadMeanDiameterBackupFactorMinimum = Optional.empty(); // BACK5/BFMINDQI

	private Optional<float[]> speciesLoreyHeightBackupFactorMaximum = Optional.empty(); // BACK5/BFMAXHLI

	public BackProcessingState(Map<String, Object> controlMap) throws ProcessingException {
		super(controlMap, VdypApplicationIdentifier.VDYP_BACK);
	}

	@Override
	public ProcessingResolvedControlMap resolveControlMap(Map<String, Object> controlMap) {
		return new ProcessingResolvedControlMapImpl(controlMap);
	}

	@Override
	protected BackLayerProcessingState createLayerState(VdypPolygon polygon, VdypLayer layer)
			throws ProcessingException {
		return new BackLayerProcessingState(this, polygon, layer.getLayerType());
	}

	public void setBaseAreaVeteran(Optional<Float> baseAreaVeteran) {
		this.baseAreaVeteran = baseAreaVeteran;
	}

	public void setBaseAreaVeteran(float baseAreaVeteran) {
		this.baseAreaVeteran = Optional.of(baseAreaVeteran);
	}

	public Optional<Float> getBaseAreaVeteran() {
		return baseAreaVeteran;
	}

	public void setCompatibilityVariableDetails(
			MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume, Map<UtilizationClass, Float>[] cvBasalArea,
			Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter,
			Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall
	) {
		if (areCompatibilityVariablesSet) {
			throw new IllegalStateException(COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY);
		}

		this.cvVolume = cvVolume;
		this.cvBasalArea = cvBasalArea;
		this.cvQuadraticMeanDiameter = cvQuadraticMeanDiameter;
		this.cvPrimaryLayerSmall = cvPrimaryLayerSmall;

		areCompatibilityVariablesSet = true;
	}

	public float getCVVolume(int speciesIndex, UtilizationClass uc, VolumeVariable volumeVariable) {
		if (!areCompatibilityVariablesSet) {
			throw UNSET_CV_VOLUMES.get();
		}

		return cvVolume[speciesIndex].get(uc, volumeVariable);
	}

	public float getCVBasalArea(int speciesIndex, UtilizationClass uc) {
		if (!areCompatibilityVariablesSet) {
			throw UNSET_CV_BASAL_AREAS.get();
		}

		return cvBasalArea[speciesIndex].get(uc);
	}

	public float getCVQuadraticMeanDiameter(int speciesIndex, UtilizationClass uc) {
		if (!areCompatibilityVariablesSet) {
			throw UNSET_CV_QUAD_MEAN_DIAMETER.get();
		}

		return cvQuadraticMeanDiameter[speciesIndex].get(uc);
	}

	public float getCVSmall(int speciesIndex, UtilizationClassVariable variable) {
		if (!areCompatibilityVariablesSet) {
			throw UNSET_CV_SMALL.get();
		}

		return cvPrimaryLayerSmall[speciesIndex].get(variable);
	}

	public void setLimits(ComponentSizeLimits[] limits) {
		this.speciesLimits = Optional.of(limits);
	}

	public ComponentSizeLimits getLimits(int speciesIndex) {
		return this.speciesLimits.orElseThrow(UNSET_LIMITS)[speciesIndex];
	}

	public float getFinalQuadraticMeanDiameter(int speciesIndex) {
		return indexAccess(finalQuadraticMeanDiameter, speciesIndex, 0, "final quadratic mean diameters");
	}

	protected static Supplier<IllegalStateException> unset(final String field) {
		final String message = MessageFormat.format("unset {0}", field);
		return () -> new IllegalStateException(message);
	}

	public Optional<Integer> getConvergenceYear() {
		return convergenceYear;
	}

	public void setConvergenceYear(Optional<Integer> convergenceYear) {
		this.convergenceYear = convergenceYear;
	}

	public Optional<Float> getConvergenceAge() {
		return convergenceAge;
	}

	public void setConvergenceAge(Optional<Float> convergenceAge) {
		this.convergenceAge = convergenceAge;
	}

	public Optional<Float> getConvergenceDominantHeight() {
		return convergenceDominantHeight;
	}

	public void setConvergenceDominantHeight(Optional<Float> convergenceDominantHeight) {
		this.convergenceDominantHeight = convergenceDominantHeight;
	}

	public Optional<Float> getConvergenceBasalArea() {
		return convergenceBasalArea;
	}

	public void setConvergenceBasalArea(Optional<Float> convergenceBasalArea) {
		this.convergenceBasalArea = convergenceBasalArea;
	}

	public Optional<Float> getConvergenceQuadraticMeanDiameter() {
		return convergenceQuadraticMeanDiameter;
	}

	public void setConvergenceQuadraticMeanDiameter(Optional<Float> convergenceLoreyHeight) {
		this.convergenceQuadraticMeanDiameter = convergenceLoreyHeight;
	}

	public float getSpeciesConvergenceLoreyHeight(int i) {
		return indexAccess(speciesConvergenceLoreyHeight, i, 1, "speciesConvergenceLoreyHeight");
	}

	public void setSpeciesConvergenceLoreyHeight(float[] speciesConvergenceLoreyHeight) {
		this.speciesConvergenceLoreyHeight = Optional.of(speciesConvergenceLoreyHeight);
	}

	public float getSpeciesConvergenceQuadraticMeanDiameter(int i) {
		return indexAccess(speciesConvergenceQuadraticMeanDiameter, i, 1, "speciesConvergenceQuadraticMeanDiameter");
	}

	public void setSpeciesConvergenceQuadraticMeanDiameter(float[] speciesConvergenceQuadraticMeanDiameter) {
		this.speciesConvergenceQuadraticMeanDiameter = Optional.of(speciesConvergenceQuadraticMeanDiameter);
	}

	public Optional<Float> getDominantHeightBackupFactor() {
		return dominantHeightBackupFactor;
	}

	public Optional<Float> getBasalAreaBackupFactor() {
		return basalAreaBackupFactor;
	}

	public Optional<Float> getQuadMeanDiameterBackupFactor() {
		return quadMeanDiameterBackupFactor;
	}

	public float getSpeciesLoreyHeightBackupFactor(int i) {
		return indexAccess(speciesLoreyHeightBackupFactor, i, 1, "speciesLoreyHeightBackupFactor");
	}

	public float getSpeciesQuadMeanDiameterBackupFactor(int i) {
		return indexAccess(speciesQuadMeanDiameterBackupFactor, i, 1, "speciesQuadMeanDiameterBackupFactor");
	}

	public Optional<Float> getQuadMeanDiameterBackupFactorMinimum() {
		return quadMeanDiameterBackupFactorMinimum;
	}

	public float getSpeciesQuadMeanDiameterBackupFactorMinimum(int i) {
		return indexAccess(
				speciesQuadMeanDiameterBackupFactorMinimum, i, 1, "speciesQuadMeanDiameterBackupFactorMinimum"
		);
	}

	public float getSpeciesLoreyHeightBackupFactorMaximum(int i) {
		return indexAccess(speciesLoreyHeightBackupFactorMaximum, i, 1, "speciesLoreyHeightBackupFactorMaximum");
	}

	public void setDominantHeightBackupFactor(Optional<Float> dominantHeightBackupFactor) {
		this.dominantHeightBackupFactor = dominantHeightBackupFactor;
	}

	public void setQuadMeanDiameterBackupFactor(Optional<Float> quadMeanDiameterBackupFactor) {
		this.quadMeanDiameterBackupFactor = quadMeanDiameterBackupFactor;
	}

	public void setQuadMeanDiameterBackupFactorMinimum(Optional<Float> quadMeanDiameterBackupFactorMinimum) {
		this.quadMeanDiameterBackupFactorMinimum = quadMeanDiameterBackupFactorMinimum;
	}

	public void setBasalAreaBackupFactor(Optional<Float> basalAreaBackupFactor) {
		this.basalAreaBackupFactor = basalAreaBackupFactor;
	}

	float indexAccess(Optional<float[]> array, int index, int minimumIndex, String field) {
		if (index < minimumIndex) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		return array.orElseThrow(unset(field))[index];
	}

	public void setFinalQuadraticMeanDiameter(float[] finalQuadraticMeanDiameters) {
		this.finalQuadraticMeanDiameter = Optional.of(finalQuadraticMeanDiameters);
	}

	public void setSpeciesLoreyHeightBackupFactor(float[] speciesLoreyHeightBackupFactor) {
		this.speciesLoreyHeightBackupFactor = Optional.of(speciesLoreyHeightBackupFactor);
	}

	public void setSpeciesQuadMeanDiameterBackupFactor(float[] speciesQuadMeanDiameterBackupFactor) {
		this.speciesQuadMeanDiameterBackupFactor = Optional.of(speciesQuadMeanDiameterBackupFactor);
	}

	public void setSpeciesQuadMeanDiameterBackupFactorMinimum(float[] speciesQuadMeanDiameterBackupFactorMinimum) {
		this.speciesQuadMeanDiameterBackupFactorMinimum = Optional.of(speciesQuadMeanDiameterBackupFactorMinimum);
	}

	public void setSpeciesLoreyHeightBackupFactorMaximum(float[] speciesLoreyHeightBackupFactorMaximum) {
		this.speciesLoreyHeightBackupFactorMaximum = Optional.of(speciesLoreyHeightBackupFactorMaximum);
	}

}
