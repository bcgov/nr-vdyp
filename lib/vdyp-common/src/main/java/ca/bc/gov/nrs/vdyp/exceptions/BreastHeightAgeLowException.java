package ca.bc.gov.nrs.vdyp.exceptions;

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

	public BreastHeightAgeLowException(RuntimeProcessingException cause) {
		super(cause, BreastHeightAgeLowException.class);
	}

	public BreastHeightAgeLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public BreastHeightAgeLowException(LayerType layer, String name, Optional<Float> value, Optional<Float> threshold) {
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
	public static Optional<BreastHeightAgeLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.checkExclusive(layer, name, value, threshold, BreastHeightAgeLowException::new);
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
	public static Optional<BreastHeightAgeLowException> check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
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
