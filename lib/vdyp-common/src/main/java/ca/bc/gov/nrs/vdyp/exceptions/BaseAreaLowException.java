package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Basal area is low or has not been set.
 * 
 * Equivalent to IPASS= -7 for VRI
 */
public class BaseAreaLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Basal area";

	public BaseAreaLowException(RuntimeStandProcessingException cause) {
		super(cause, BaseAreaLowException.class);
	}

	public BaseAreaLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public BaseAreaLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VDYP_BACK)
			return Optional.of(-7);
		return Optional.empty();
	}

}
