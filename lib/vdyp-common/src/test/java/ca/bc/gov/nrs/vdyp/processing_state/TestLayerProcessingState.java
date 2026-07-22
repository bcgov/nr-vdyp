package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.function.Predicate;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class TestLayerProcessingState extends LayerProcessingState<TestLayerProcessingState> {

	protected TestLayerProcessingState(
			ProcessingState<TestLayerProcessingState> ps, VdypPolygon polygon, LayerType subjectLayerType
	) throws ProcessingException {
		super(ps, polygon, subjectLayerType);
	}

	@Override
	protected Predicate<VdypSpecies> getBankFilter() {
		return s -> true;
	}

	@Override
	protected void applyCompatibilityVariables(VdypSpecies species, int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected VdypLayer updateLayerFromBank() {
		throw new UnsupportedOperationException();
	}

}
