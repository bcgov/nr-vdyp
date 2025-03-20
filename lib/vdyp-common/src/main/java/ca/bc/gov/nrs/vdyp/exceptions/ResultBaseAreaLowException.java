package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Computed basal area is low.
 * 
 * Equivalent to IPASS= -13 for FIP
 */
public class ResultBaseAreaLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Basal area";

	public ResultBaseAreaLowException(RuntimeStandProcessingException cause) {
		super(cause, ResultBaseAreaLowException.class);
	}

	public ResultBaseAreaLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public ResultBaseAreaLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.FIP_START)
			return Optional.of(-13);
		return Optional.empty();
	}

}
