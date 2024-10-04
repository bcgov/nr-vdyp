package ca.bc.gov.nrs.vdyp.back;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;

public class BackProcessingEngine extends ProcessingEngine {

	/**
	 * 
	 * @throws StandProcessingException
	 */
	// BACKPREP
	void prepare(BackProcessingState state) throws ProcessingException {

		state.setBaseAreaVeteran(
				state.getVeteranLayerProcessingState()
						.map(vetState -> vetState.getBank().basalAreas[0][0 + 1])
		);


	}
}
