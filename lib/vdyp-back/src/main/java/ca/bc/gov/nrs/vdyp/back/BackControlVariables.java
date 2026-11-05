package ca.bc.gov.nrs.vdyp.back;

import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.TargetYear;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;

public class BackControlVariables extends ProcessingControlVariables {

	public BackControlVariables(Integer[] controlVariableValues) throws ValueParseException {
		super(controlVariableValues);
	}

	@Override
	public TargetYear getTargetYear() {
		var target = super.getTargetYear();
		// Back interprets relative years as going into the past rather than the future.
		if (target.isRelative() && target.getValue() != 0) {
			target = TargetYear.relative(-target.getValue());
		}
		return target;
	}

}
