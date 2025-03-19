package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Crown closure is low or has not been set.
 * 
 * Equivalent to IPASS= -9 for VRI
 */
public class CrownClosureLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Crown closure";

	CrownClosureLowException(RuntimeStandProcessingException cause) {
		super(cause, CrownClosureLowException.class);
	}

	CrownClosureLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	CrownClosureLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}
	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VDYP_BACK)
			return Optional.of(-9);
		return Optional.empty();
	}

}
