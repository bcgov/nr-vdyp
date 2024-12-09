package ca.bc.gov.nrs.vdyp.forward;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMap;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public class ForwardProcessingState extends ProcessingState<ForwardResolvedControlMap, ForwardLayerProcessingState> {

	public ForwardProcessingState(Map<String, Object> controlMap) throws ProcessingException {
		super(controlMap);
	}

	@Override
	protected ForwardResolvedControlMap resolveControlMap(Map<String, Object> controlMap) {
		return new ForwardResolvedControlMapImpl(controlMap);
	}

	@Override
	protected LayerProcessingState<ForwardResolvedControlMap, ForwardLayerProcessingState> createLayerState(VdypPolygon polygon, VdypLayer layer)
			throws ProcessingException {
		return new ForwardLayerProcessingState(this, polygon, layer);
	}

}