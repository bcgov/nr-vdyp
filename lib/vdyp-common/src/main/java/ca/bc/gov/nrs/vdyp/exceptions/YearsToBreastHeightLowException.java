package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Years to breast height is low or has not been set.
 * 
 * Equivalent to IPASS= -5 for VRI
 */
public class YearsToBreastHeightLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Years to breast height";

	YearsToBreastHeightLowException(RuntimeStandProcessingException cause) {
		super(cause, YearsToBreastHeightLowException.class);
	}

	YearsToBreastHeightLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	YearsToBreastHeightLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VDYP_BACK)
			return Optional.of(-5);
		return Optional.empty();
	}

}
