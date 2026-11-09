package ca.bc.gov.nrs.vdyp.back.io.parse;

import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.projection.ProcessingControlVariableParser;

public class BackControlParser extends ProcessingControlParser {

	@Override
	protected ProcessingControlVariableParser getControlVariableParser() {
		return new BackControlVariableParser();
	}

}
