package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Breast height age is low or has not been set.
 * 
 * Equivalent to IPASS= -10 for FIP or -5 for VRI
 */
public class BreastHeightAgeLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Breast height age";

	BreastHeightAgeLowException(RuntimeStandProcessingException cause) {
		super(cause, BreastHeightAgeLowException.class);
	}

	public BreastHeightAgeLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public BreastHeightAgeLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START:
			return Optional.of(-10);
		case VRI_START:
			return Optional.of(-5);
		default:
			return Optional.empty();
		}
	}

}
