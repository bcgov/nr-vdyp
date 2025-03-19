package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Total age is low or has not been set.
 * 
 * Equivalent to IPASS=-6 for FIP or -5 for VRI
 */
public class TotalAgeLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Total age";

	TotalAgeLowException(RuntimeStandProcessingException cause) {
		super(cause, TotalAgeLowException.class);
	}

	TotalAgeLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	TotalAgeLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START:
			return Optional.of(-6);
		case VDYP_BACK:
			return Optional.of(-5);
		default:
			return Optional.empty();
		}
	}

}
