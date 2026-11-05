package ca.bc.gov.nrs.vdyp.back.io.parse;

import ca.bc.gov.nrs.vdyp.back.BackControlVariables;
import ca.bc.gov.nrs.vdyp.io.parse.projection.ProcessingControlVariableParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;

public class BackControlVariableParser extends ProcessingControlVariableParser {

	@Override
	protected ProcessingControlVariables create(Integer[] controlVariableValues) throws ValueParseException {

		return new BackControlVariables(controlVariableValues);
	}

}
