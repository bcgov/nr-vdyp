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

	public QuadraticMeanDiameterLowException(RuntimeStandProcessingException cause) {
		super(cause, QuadraticMeanDiameterLowException.class);
	}

	public QuadraticMeanDiameterLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public QuadraticMeanDiameterLowException(
			LayerType layer, String name, Optional<Float> value, Optional<Float> threshold
	) {
		super(layer, name, value, threshold);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-7);
		return Optional.empty();
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
	public static Optional<QuadraticMeanDiameterLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException
				.checkExclusive(layer, name, value, threshold, QuadraticMeanDiameterLowException::new);
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
	public static Optional<QuadraticMeanDiameterLowException>
			check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
	}

}
