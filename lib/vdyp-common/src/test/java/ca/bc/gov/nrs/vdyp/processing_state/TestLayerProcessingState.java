package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.function.Predicate;

import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class TestLayerProcessingState extends LayerProcessingState<ResolvedControlMap, TestLayerProcessingState> {

	protected TestLayerProcessingState(
			ProcessingState<ResolvedControlMap, TestLayerProcessingState> ps, VdypPolygon polygon,
			LayerType subjectLayerType
	) {
		super(ps, polygon, subjectLayerType);
	}

	@Override
	protected Predicate<VdypSpecies> getBankFilter() {
		return X -> true;
	}

	@Override
	protected VdypLayer updateLayerFromBank() {
		return getBank().buildLayerFromBank();
	}

}
