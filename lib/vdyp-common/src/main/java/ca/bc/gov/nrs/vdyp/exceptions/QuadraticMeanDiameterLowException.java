package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Quadratic mean diameter is low or has not been set.
 * 
 * Equivalent to IPASS= -7 for VRI
 */
public class QuadraticMeanDiameterLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Quadratic mean diameter";

	QuadraticMeanDiameterLowException(RuntimeStandProcessingException cause) {
		super(cause, QuadraticMeanDiameterLowException.class);
	}

	QuadraticMeanDiameterLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	QuadraticMeanDiameterLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-7);
		return Optional.empty();
	}

}
