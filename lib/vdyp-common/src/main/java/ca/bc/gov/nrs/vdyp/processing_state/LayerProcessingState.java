package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
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
	private Optional<List<VdypCompatibilityVariables>> compatibilityVariables = Optional.empty();

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
		if (compatibilityVariables.isPresent()) {
			throw new IllegalStateException(COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY);
		}
		int n = cvVolume.length;

		compatibilityVariables = Optional.of(
				IntStream.range(0, n)
						.mapToObj(i -> {
							if (cvVolume[i] == null)
								return null;
							return VdypCompatibilityVariables.build(cvb -> {

								cvb.cvVolume(cvVolume[i]);
								cvb.cvBasalArea(cvBasalArea[i]);
								cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter[i]);
								cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall[i]);
							});
						}).toList()
		);
	}

	public float getCVVolume(
			int speciesIndex, UtilizationClass uc, UtilizationClassVariable volumeVariable, LayerType layerType
	) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvVolume(uc, volumeVariable, layerType);
	}

	public float getCVBasalArea(int speciesIndex, UtilizationClass uc, LayerType layerType) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvBasalArea(uc, layerType);
	}

	public float getCVQuadraticMeanDiameter(int speciesIndex, UtilizationClass uc, LayerType layerType) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvQuadraticMeanDiameter(uc, layerType);
	}

	public float getCVSmall(int speciesIndex, UtilizationClassVariable variable) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvPrimaryLayerSmall(variable);
	}

	public MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> getCvVolume(int speciesIndex) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvVolume();
	}

	public MatrixMap2<UtilizationClass, LayerType, Float> getCvBasalArea(int speciesIndex) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvBasalArea();
	}

	public MatrixMap2<UtilizationClass, LayerType, Float> getCvQuadraticMeanDiameter(int speciesIndex) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvQuadraticMeanDiameter();
	}

	public Map<UtilizationClassVariable, Float> getCvPrimaryLayerSmall(int speciesIndex) {
		return compatibilityVariables
				.orElseThrow(() -> new IllegalStateException(UNSET_CV_VOLUMES))
				.get(speciesIndex)
				.getCvPrimaryLayerSmall();

	}

}