package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class TestProcessingState extends ProcessingState<TestLayerProcessingState> {

	public TestProcessingState(Map<String, Object> controlMap, VdypApplicationIdentifier appId) {
		super(controlMap, appId);
	}

	@Override
	protected ProcessingResolvedControlMap resolveControlMap(Map<String, Object> controlMap) {
		return new ProcessingResolvedControlMapImpl(controlMap);
	}

	@Override
	protected TestLayerProcessingState createLayerState(VdypPolygon polygon, VdypLayer layer)
			throws ProcessingException {
		return new TestLayerProcessingState(this, polygon, layer.getLayerType());
	}

}
