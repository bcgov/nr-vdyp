package ca.bc.gov.nrs.vdyp.back.processing_state;

import java.util.function.Predicate;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public class BackLayerProcessingState
		extends LayerProcessingState<ForwardResolvedControlMap, BackLayerProcessingState> {

	protected BackLayerProcessingState(
			ProcessingState<ForwardResolvedControlMap, BackLayerProcessingState> ps, VdypPolygon polygon,
			LayerType subjectLayerType
	) throws ProcessingException {
		super(ps, polygon, subjectLayerType);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Predicate<VdypSpecies> getBankFilter() {
		// TODO Auto-generated method stub
		return x -> true;
	}

	@Override
	protected void applyCompatibilityVariables(VdypSpecies species, int i) {
		// TODO Auto-generated method stub

	}

	@Override
	protected VdypLayer updateLayerFromBank() {
		// TODO Auto-generated method stub
		return null;
	}

}
