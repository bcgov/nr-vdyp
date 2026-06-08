package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.RuntimeProcessingException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public abstract class ProcessingState<LS extends LayerProcessingState<LS>> {

	/** The control map defining the context of the execution */
	public final ProcessingResolvedControlMap controlMap;

	/** The estimators instance used by this engine */
	public final EstimationMethods estimators;

	/** The computation instance used by this engine */
	public final ComputationMethods computers;

	/** The polygon on which the Processor is currently operating */
	private VdypPolygon polygon;

	/** The processing state of the primary layer of <code>polygon</code> */
	private LS plps;

	/** The processing state of the veteran layer of <code>polygon</code> */
	private Optional<LS> vlps;

	protected ProcessingState(Map<String, Object> controlMap, VdypApplicationIdentifier appId) {
		this.controlMap = resolveControlMap(controlMap);
		this.estimators = new EstimationMethods(this.controlMap);
		this.computers = new ComputationMethods(estimators, appId);
	}

	protected abstract ProcessingResolvedControlMap resolveControlMap(Map<String, Object> controlMap);

	protected abstract LS createLayerState(VdypPolygon polygon, VdypLayer layer) throws ProcessingException;

	private LS createLayerStateSafe(VdypPolygon polygon, VdypLayer layer) {
		try {
			return createLayerState(polygon, layer);
		} catch (ProcessingException e) {
			throw new RuntimeProcessingException(e);
		}
	}

	protected Optional<VdypLayer> getLayer(LayerType type) {
		return Optional.ofNullable(polygon).flatMap(p -> Optional.ofNullable(p.getLayers().get(type)));
	}

	public EstimationMethods getEstimators() {
		return estimators;
	}

	public ComputationMethods getComputers() {
		return computers;
	}

	public void setPolygon(VdypPolygon polygon) throws ProcessingException {

		this.polygon = polygon;

		this.plps = createLayerState(
				polygon, getLayer(LayerType.PRIMARY).orElseThrow(() -> new IllegalStateException("No primary layer"))
		);
		try {
			this.vlps = getLayer(LayerType.VETERAN).map(layer -> createLayerStateSafe(polygon, layer));
		} catch (RuntimeProcessingException e) {
			throw e.getCause();
		}
	}

	/** @return the current polygon */
	public VdypPolygon getCurrentPolygon() {
		return polygon;
	}

	/** @return the compact form of the current polygon's identifier. Shortcut. */
	public String getCompactPolygonIdentifier() {
		return polygon.getPolygonIdentifier().toStringCompact();
	}

	/** @return the starting year of the current polygon. Shortcut. */
	public int getCurrentStartingYear() {
		return polygon.getPolygonIdentifier().getYear();
	}

	/** @return the bec zone of the current polygon. Shortcut. */
	public BecDefinition getCurrentBecZone() {
		return polygon.getBiogeoclimaticZone();
	}

	public LS getPrimaryLayerProcessingState() {
		return plps;
	}

	public Optional<LS> getVeteranLayerProcessingState() {
		return vlps;
	}

	public VdypPolygon updatePolygon() {

		polygon.getLayers().put(LayerType.PRIMARY, plps.updateLayerFromBank());
		vlps.ifPresent(vlps -> polygon.getLayers().put(LayerType.VETERAN, vlps.updateLayerFromBank()));

		return polygon;
	}

	public ProcessingResolvedControlMap getControlMap() {
		return controlMap;
	}
}