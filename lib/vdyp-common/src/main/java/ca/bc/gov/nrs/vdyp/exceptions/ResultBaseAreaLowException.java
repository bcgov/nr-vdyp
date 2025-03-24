package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Computed basal area is low.
 *
 * Equivalent to IPASS= -13 for FIP
 */
public class ResultBaseAreaLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Basal area";

	public ResultBaseAreaLowException(RuntimeStandProcessingException cause) {
		super(cause, ResultBaseAreaLowException.class);
	}

	public ResultBaseAreaLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public ResultBaseAreaLowException(LayerType layer, String name, Optional<Float> value, Optional<Float> threshold) {
		super(layer, name, value, threshold);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.FIP_START)
			return Optional.of(-13);
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
	public static Optional<ResultBaseAreaLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.check(layer, name, value, threshold, ResultBaseAreaLowException::new);
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
	public static Optional<ResultBaseAreaLowException> check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
	}

}
