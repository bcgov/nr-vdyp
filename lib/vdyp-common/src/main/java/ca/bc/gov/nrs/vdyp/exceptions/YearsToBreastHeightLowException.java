package ca.bc.gov.nrs.vdyp.exceptions;

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

	public YearsToBreastHeightLowException(RuntimeStandProcessingException cause) {
		super(cause, YearsToBreastHeightLowException.class);
	}

	public YearsToBreastHeightLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public YearsToBreastHeightLowException(
			LayerType layer, String name, Optional<Float> value, Optional<Float> threshold
	) {
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
	public static Optional<YearsToBreastHeightLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.check(layer, name, value, threshold, YearsToBreastHeightLowException::new);
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
	public static Optional<YearsToBreastHeightLowException>
			check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-7);
		return Optional.empty();
	}

}
