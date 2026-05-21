package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;

public abstract class LayerProcessingState<RCM extends ResolvedControlMap, Self extends LayerProcessingState<RCM, Self>> {

	@FunctionalInterface
	protected static interface SmallCVUpdate {
		float apply(float previousValue, UtilizationClassVariable variable, int index);
	}

	@FunctionalInterface
	protected static interface VolumeCVUpdate {
		float apply(float previousValue, UtilizationClass uClass, VolumeVariable variable, int index);
	}

	@FunctionalInterface
	protected static interface OtherCVUpdate {
		float apply(float previousValue, UtilizationClass uClass, int index);
	}

	private static final Logger logger = LoggerFactory.getLogger(LayerProcessingState.class);

	private static final String COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY = "CompatibilityVariablesSet can be set once only";
	private static final String UNSET_CV_VOLUMES = "unset cvVolumes";
	private static final String UNSET_CV_BASAL_AREAS = "unset cvBasalAreas";

	/** The containing ForwardProcessingState */
	protected final ProcessingState<RCM, Self> ps;

	/** The containing polygon of the layer on which the Processor is operating */
	private final VdypPolygon polygon;

	/** The type of Layer being processed */
	private final LayerType layerType;

	// L1COM1, L1COM4 and L1COM5 - these common blocks mirror BANK1, BANK2 and BANK3 and are initialized
	// when copied to "active" in ForwardProcessingEngine.

	/**
	 * State of the layer during processing.
	 */
	private Bank bank;

	// Compatibility Variables - LCV1 & LCVS
	private boolean areCompatibilityVariablesSet = false;

	private MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>[] cvVolume;
	private MatrixMap2<UtilizationClass, LayerType, Float>[] cvBasalArea;
	private MatrixMap2<UtilizationClass, LayerType, Float>[] cvQuadraticMeanDiameter;
	private Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall;

	protected LayerProcessingState(ProcessingState<RCM, Self> ps, VdypPolygon polygon, LayerType subjectLayerType)
			throws ProcessingException {

		this.ps = ps;
		this.polygon = polygon;
		this.layerType = subjectLayerType;

		BecDefinition becZone = polygon.getBiogeoclimaticZone();

		this.bank = new Bank(polygon.getLayers().get(subjectLayerType), becZone, getBankFilter());

	}

	protected abstract Predicate<VdypSpecies> getBankFilter();

	public VdypPolygon getPolygon() {
		return polygon;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public BecDefinition getBecZone() {
		return bank.getBecZone();
	}

	public static Logger getLogger() {
		return logger;
	}

	public ProcessingState<RCM, Self> getParent() {
		return ps;
	}

	public Bank getBank() {
		return bank;
	}

	protected abstract void applyCompatibilityVariables(VdypSpecies species, int i);

	public int getNSpecies() {
		return bank.getNSpecies();
	}

	protected abstract VdypLayer updateLayerFromBank();

	public void setCompatibilityVariableDetails(
			MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>[] cvVolume,
			MatrixMap2<UtilizationClass, LayerType, Float>[] cvBasalArea,
			MatrixMap2<UtilizationClass, LayerType, Float>[] cvQuadraticMeanDiameter,
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

	public float
			getCVVolume(int speciesIndex, UtilizationClass uc, VolumeVariable volumeVariable, LayerType layerType) {
		if (!areCompatibilityVariablesSet) {
			throw new IllegalStateException(UNSET_CV_VOLUMES);
		}

		return cvVolume[speciesIndex].get(uc, volumeVariable, layerType);
	}

	public float getCVBasalArea(int speciesIndex, UtilizationClass uc, LayerType layerType) {
		if (!areCompatibilityVariablesSet) {
			throw new IllegalStateException(UNSET_CV_BASAL_AREAS);
		}

		return cvBasalArea[speciesIndex].get(uc, layerType);
	}

	public float getCVQuadraticMeanDiameter(int speciesIndex, UtilizationClass uc, LayerType layerType) {
		if (!areCompatibilityVariablesSet) {
			throw new IllegalStateException(UNSET_CV_BASAL_AREAS);
		}

		return cvQuadraticMeanDiameter[speciesIndex].get(uc, layerType);
	}

	public float getCVSmall(int speciesIndex, UtilizationClassVariable variable) {
		if (!areCompatibilityVariablesSet) {
			throw new IllegalStateException(UNSET_CV_BASAL_AREAS);
		}

		return cvPrimaryLayerSmall[speciesIndex].get(variable);
	}

	/**
	 * Update each compatibility variable using the given methods. Each takes the existing value and identifying
	 * enums/indices, and returns the updated value.
	 *
	 * @param smallUpdate update small component CVs
	 * @param baUpdate    update basal area CVs
	 * @param dqUpdate    update quadratic mean diameter CVs
	 * @param volUpdate   update volume CVs
	 */
	protected void updateCompatibilityVariables(
			SmallCVUpdate smallUpdate, OtherCVUpdate baUpdate, OtherCVUpdate dqUpdate, VolumeCVUpdate volUpdate
	) {
		for (int i : getIndices()) {
			for (UtilizationClassVariable sucv : UtilizationClassVariable.values()) {
				cvPrimaryLayerSmall[i].put(sucv, smallUpdate.apply(cvPrimaryLayerSmall[i].get(sucv), sucv, i));
			}
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				cvBasalArea[i]
						.put(uc, LayerType.PRIMARY, baUpdate.apply(cvBasalArea[i].get(uc, LayerType.PRIMARY), uc, i));
				cvQuadraticMeanDiameter[i].put(
						uc, LayerType.PRIMARY,
						dqUpdate.apply(cvQuadraticMeanDiameter[i].get(uc, LayerType.PRIMARY), uc, i)
				);

				for (VolumeVariable vv : VolumeVariable.ALL) {
					cvVolume[i].put(
							uc, vv, LayerType.PRIMARY,
							volUpdate.apply(cvVolume[i].get(uc, vv, LayerType.PRIMARY), uc, vv, i)
					);
				}
			}
		}
	}

	public int[] getIndices() {
		return bank.getIndices();
	}

	protected void applyCompatibilityVariablesToSpecies(int i, VdypSpecies species) {
		species.setCompatibilityVariables(
				cvVolume[i], cvBasalArea[i], cvQuadraticMeanDiameter[i], cvPrimaryLayerSmall[i]
		);
	}

	public Map<UtilizationClassVariable, Float>[] getCvPrimaryLayerSmall() {
		return this.cvPrimaryLayerSmall;
	}

}