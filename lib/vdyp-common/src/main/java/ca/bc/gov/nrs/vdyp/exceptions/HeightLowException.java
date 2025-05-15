package ca.bc.gov.nrs.vdyp.exceptions;

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

	public HeightLowException(RuntimeProcessingException cause) {
		super(cause, HeightLowException.class);
	}

	public HeightLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public HeightLowException(LayerType layer, String name, Optional<Float> value, Optional<Float> threshold) {
		super(layer, name, value, threshold);
	}

	/**
	 * Checks that the given value is present and greater than the given threshold. Returns an optional with an
	 * appropriate exception if it fails, an empty optional otherwise.
	 *
	 * @param layer
	 * @param name
	 * @param value
	 * @param threshold
	 * @return
	 */
	public static Optional<HeightLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.checkExclusive(layer, name, value, threshold, HeightLowException::new);
	}

	/**
	 * Checks that the given value is present and greater than the given threshold. Returns an optional with an
	 * appropriate exception if it fails, an empty optional otherwise.
	 *
	 * @param layer
	 * @param value
	 * @param threshold
	 * @return
	 */
	public static Optional<HeightLowException> check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
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
