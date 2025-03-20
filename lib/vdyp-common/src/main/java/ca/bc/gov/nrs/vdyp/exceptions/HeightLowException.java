package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Height is low or has not been set.
 * 
 * If layer is PRIMARY it's equivalent to IPASS=-4/-6 for FIP/VRI, if layer is VETERAN, to -5/-10
 */
public class HeightLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Height";

	public HeightLowException(RuntimeStandProcessingException cause) {
		super(cause, HeightLowException.class);
	}

	public HeightLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public HeightLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START:
			switch (getLayer()) {
			case PRIMARY:
				return Optional.of(-4);
			case VETERAN:
				return Optional.of(-5);
			default:
				return Optional.empty();
			}
		case VRI_START:
			switch (getLayer()) {
			case PRIMARY:
				return Optional.of(-6);
			case VETERAN:
				return Optional.of(-10);
			default:
				return Optional.empty();
			}
		default:
			return Optional.empty();
		}
	}

}
