package ca.bc.gov.nrs.vdyp.back.processing_state;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariables;
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

	private static final Supplier<IllegalStateException> UNSET_LIMITS = unset("per species limits");

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

	@Deprecated
	public void setCompatibilityVariableDetails(
			MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume, Map<UtilizationClass, Float>[] cvBasalArea,
			Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter,
			Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall
	) {
		setCompatibilityVariableDetails(
				CompatibilityVariables.fromArrays(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvPrimaryLayerSmall)
		);
	}

	@Deprecated
	public void setCompatibilityVariableDetails(CompatibilityVariables[] details) {

		this.getPrimaryLayerProcessingState().setBackCompatibilityVariables(details);
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

	public void setConvergenceYear(int convergenceYear) {
		setConvergenceYear(Optional.of(convergenceYear));
	}

	public Optional<Float> getConvergenceAge() {
		return convergenceAge;
	}

	public void setConvergenceAge(Optional<Float> convergenceAge) {
		this.convergenceAge = convergenceAge;
	}

	public void setConvergenceAge(float convergenceAge) {
		setConvergenceAge(Optional.of(convergenceAge));
	}

	public Optional<Float> getConvergenceDominantHeight() {
		return convergenceDominantHeight;
	}

	public void setConvergenceDominantHeight(Optional<Float> convergenceDominantHeight) {
		this.convergenceDominantHeight = convergenceDominantHeight;
	}

	public void setConvergenceDominantHeight(float convergenceDominantHeight) {
		setConvergenceDominantHeight(Optional.of(convergenceDominantHeight));
	}

	public Optional<Float> getConvergenceBasalArea() {
		return convergenceBasalArea;
	}

	public void setConvergenceBasalArea(Optional<Float> convergenceBasalArea) {
		this.convergenceBasalArea = convergenceBasalArea;
	}

	public void setConvergenceBasalArea(float convergenceBasalArea) {
		setConvergenceBasalArea(Optional.of(convergenceBasalArea));
	}

	public Optional<Float> getConvergenceQuadraticMeanDiameter() {
		return convergenceQuadraticMeanDiameter;
	}

	public void setConvergenceQuadraticMeanDiameter(Optional<Float> convergenceLoreyHeight) {
		this.convergenceQuadraticMeanDiameter = convergenceLoreyHeight;
	}

	public void setConvergenceQuadraticMeanDiameter(float convergenceLoreyHeight) {
		setConvergenceQuadraticMeanDiameter(Optional.of(convergenceLoreyHeight));
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

	public float getSpeciesQuadMeanDiameterBackupFactorMinimum(String speciesGroup) {
		return getSpeciesQuadMeanDiameterBackupFactorMinimum(this.getSpeciesGroupIndex(speciesGroup));
	}

	public float getSpeciesLoreyHeightBackupFactorMaximum(int i) {
		return indexAccess(speciesLoreyHeightBackupFactorMaximum, i, 1, "speciesLoreyHeightBackupFactorMaximum");
	}

	public float getSpeciesLoreyHeightBackupFactorMaximum(String speciesGroup) {
		return getSpeciesLoreyHeightBackupFactorMaximum(getSpeciesGroupIndex(speciesGroup));
	}

	public void setDominantHeightBackupFactor(Optional<Float> dominantHeightBackupFactor) {
		this.dominantHeightBackupFactor = dominantHeightBackupFactor;
	}

	public void setDominantHeightBackupFactor(float dominantHeightBackupFactor) {
		setDominantHeightBackupFactor(Optional.of(dominantHeightBackupFactor));

	}

	public void setQuadMeanDiameterBackupFactor(Optional<Float> quadMeanDiameterBackupFactor) {
		this.quadMeanDiameterBackupFactor = quadMeanDiameterBackupFactor;
	}

	public void setQuadMeanDiameterBackupFactor(float quadMeanDiameterBackupFactor) {
		setQuadMeanDiameterBackupFactor(Optional.of(quadMeanDiameterBackupFactor));
	}

	public void setQuadMeanDiameterBackupFactorMinimum(Optional<Float> quadMeanDiameterBackupFactorMinimum) {
		this.quadMeanDiameterBackupFactorMinimum = quadMeanDiameterBackupFactorMinimum;
	}

	public void setQuadMeanDiameterBackupFactorMinimum(float quadMeanDiameterBackupFactorMinimum) {
		setQuadMeanDiameterBackupFactorMinimum(Optional.of(quadMeanDiameterBackupFactorMinimum));
	}

	public void setBasalAreaBackupFactor(Optional<Float> basalAreaBackupFactor) {
		this.basalAreaBackupFactor = basalAreaBackupFactor;
	}

	public void setBasalAreaBackupFactor(float basalAreaBackupFactor) {
		setBasalAreaBackupFactor(Optional.of(basalAreaBackupFactor));
	}

	float indexAccess(Optional<float[]> array, int index, int minimumIndex, String field) {
		if (index < minimumIndex) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		return array.orElseThrow(unset(field))[index];
	}

	public int getSpeciesGroupIndex(String speciesGroupId) {
		int i = 0;
		for (var species : this.getCurrentPolygon().requirePrimaryLayer().getOrderedSpecies()) {
			i++;
			if (species.getGenus().equals(speciesGroupId)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Species " + speciesGroupId + " is not present");
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
