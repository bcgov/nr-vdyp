package ca.bc.gov.nrs.vdyp.io.parse.control;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

public class TestProcessingControlParser extends ProcessingControlParser {

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

}
