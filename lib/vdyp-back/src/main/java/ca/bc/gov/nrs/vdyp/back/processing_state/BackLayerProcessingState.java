package ca.bc.gov.nrs.vdyp.back.processing_state;

import java.util.Optional;
import java.util.function.Predicate;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;

public class BackLayerProcessingState extends LayerProcessingState<BackLayerProcessingState> {

	public static final int MAX_BANK_INSTANCES = 3;

	Optional<CompatibilityVariables[]> backCompatibilityVariables = Optional.empty();

	protected BackLayerProcessingState(
			ProcessingState<BackLayerProcessingState> ps, VdypPolygon polygon, LayerType subjectLayerType
	) throws ProcessingException {
		super(ps, polygon, subjectLayerType);
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

	public void setBackCompatibilityVariables(CompatibilityVariables[] backCompatibilityVariables) {
		this.backCompatibilityVariables = Optional.of(backCompatibilityVariables);
	}

	public void setFractionalCompatibilityVariables(float fraction) {

		this.updateCompatibilityVariables(
				(prev, ucv, i) -> backCompatibilityVariables.get()[i].primaryLayerSmall().get(ucv),
				(prev, uc, i) -> backCompatibilityVariables.get()[i].basalArea().get(uc),
				(prev, uc, i) -> backCompatibilityVariables.get()[i].quadraticMeanDiameter().get(uc),
				(prev, uc, vv, i) -> backCompatibilityVariables.get()[i].volume().get(uc, vv)
		);
	}
}
