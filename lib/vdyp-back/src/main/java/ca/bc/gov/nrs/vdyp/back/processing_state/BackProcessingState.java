package ca.bc.gov.nrs.vdyp.back.processing_state;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMap;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public class BackProcessingState extends ProcessingState<ForwardResolvedControlMap, BackLayerProcessingState> {

	Optional<Float> baseAreaVeteran = Optional.empty(); // BACK1/BAV

	private static final String COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY = "CompatibilityVariablesSet can be set once only";
	private static final Supplier<IllegalStateException> UNSET_CV_VOLUMES = unset("cvVolumes");
	private static final Supplier<IllegalStateException> UNSET_CV_BASAL_AREAS = unset("cvBasalAreas");
	private static final Supplier<IllegalStateException> UNSET_LIMITS = unset("per species limits");
	private static final Supplier<IllegalStateException> UNSET_FINAL_QUAD_MEAN_DIAMETER = unset(
			"final quadratic mean diameters"
	);

	// Compatibility Variables - LCV1 & LCVS
	private boolean areCompatibilityVariablesSet = false;

	private MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume;
	private Map<UtilizationClass, Float>[] cvBasalArea;
	private Map<UtilizationClass, Float>[] cvQuadraticMeanDiameter;
	private Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall;

	private Optional<ComponentSizeLimits[]> speciesLimits = Optional.empty();
	private Optional<float[]> finalQuadraticMeanDiameters = Optional.empty();

	public BackProcessingState(Map<String, Object> controlMap) throws ProcessingException {
		super(controlMap);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ForwardResolvedControlMap resolveControlMap(Map<String, Object> controlMap) {
		return new ForwardResolvedControlMapImpl(controlMap);
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
			throw UNSET_CV_BASAL_AREAS.get();
		}

		return cvQuadraticMeanDiameter[speciesIndex].get(uc);
	}

	public float getCVSmall(int speciesIndex, UtilizationClassVariable variable) {
		if (!areCompatibilityVariablesSet) {
			throw UNSET_CV_BASAL_AREAS.get();
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
		return this.finalQuadraticMeanDiameters.orElseThrow(UNSET_FINAL_QUAD_MEAN_DIAMETER)[speciesIndex];
	}

	protected static Supplier<IllegalStateException> unset(final String field) {
		final String message = MessageFormat.format("unset {0}", field);
		return () -> new IllegalStateException(message);
	}

	public void setFinalQuadMeanDiameters(float[] finalDiameters) {
		finalQuadraticMeanDiameters = Optional.of(finalDiameters);
	}

}
