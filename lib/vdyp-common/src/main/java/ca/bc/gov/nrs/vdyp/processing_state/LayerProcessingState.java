package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;

public abstract class LayerProcessingState<RCM extends ResolvedControlMap, Self extends LayerProcessingState<RCM, Self>> {

	private static final Logger logger = LoggerFactory.getLogger(LayerProcessingState.class);

	private static final String COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY = "CompatibilityVariablesSet can be set once only";
	private static final String UNSET_CV_VOLUMES = "unset cvVolumes";
	private static final String UNSET_CV_BASAL_AREAS = "unset cvBasalAreas";

	/** The containing ForwardProcessingState */
	private final ProcessingState<RCM, Self> ps;

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

	private MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float>[] cvVolume;
	private MatrixMap2<UtilizationClass, LayerType, Float>[] cvBasalArea;
	private MatrixMap2<UtilizationClass, LayerType, Float>[] cvQuadraticMeanDiameter;
	private Map<UtilizationClassVariable, Float>[] cvPrimaryLayerSmall;

	protected LayerProcessingState(ProcessingState<RCM, Self> ps, VdypPolygon polygon, LayerType subjectLayerType) {

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

	public int getNSpecies() {
		return bank.getNSpecies();
	}

	protected abstract VdypLayer updateLayerFromBank();

	public void setCompatibilityVariableDetails(
			MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float>[] cvVolume,
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

	public float getCVVolume(
			int speciesIndex, UtilizationClass uc, UtilizationClassVariable volumeVariable, LayerType layerType
	) {
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

	public MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float>[] getCvVolume() {
		return cvVolume;
	}

	public MatrixMap2<UtilizationClass, LayerType, Float>[] getCvBasalArea() {
		return cvBasalArea;
	}

	public MatrixMap2<UtilizationClass, LayerType, Float>[] getCvQuadraticMeanDiameter() {
		return cvQuadraticMeanDiameter;
	}

	public Map<UtilizationClassVariable, Float>[] getCvPrimaryLayerSmall() {
		return cvPrimaryLayerSmall;
	}

}