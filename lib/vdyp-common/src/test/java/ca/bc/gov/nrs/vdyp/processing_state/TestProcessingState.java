package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class TestProcessingState extends ProcessingState<ResolvedControlMap, TestLayerProcessingState> {

	public TestProcessingState(Map<String, Object> controlMap) throws ProcessingException {
		super(controlMap);
	}

	@Override
	protected ResolvedControlMap resolveControlMap(Map<String, Object> controlMap) {
		return new ResolvedControlMapImpl(controlMap);
	}

	@Override
	protected TestLayerProcessingState createLayerState(VdypPolygon polygon, VdypLayer layer)
			throws ProcessingException {
		return new TestLayerProcessingState(this, this.getCurrentPolygon(), layer.getLayerType());
	}

}
