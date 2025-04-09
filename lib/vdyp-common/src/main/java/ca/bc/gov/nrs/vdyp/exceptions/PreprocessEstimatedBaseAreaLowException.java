package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Basal area estimated during preprocessing is low or has not been set.
 *
 * Equivalent to IPASS= -13 for VRI
 */
public class PreprocessEstimatedBaseAreaLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Normative estimate of basal area";

	public PreprocessEstimatedBaseAreaLowException(RuntimeStandProcessingException cause) {
		super(cause, PreprocessEstimatedBaseAreaLowException.class);
	}

	public PreprocessEstimatedBaseAreaLowException(BaseAreaLowException cause) {
		super(cause.getLayer(), DEFAULT_NAME, cause.getValue(), cause.getThreshold(), cause);
	}

	public PreprocessEstimatedBaseAreaLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	public PreprocessEstimatedBaseAreaLowException(LayerType layer, String name, Optional<Float> value, Optional<Float> threshold) {
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
	public static Optional<PreprocessEstimatedBaseAreaLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.checkExclusive(
				layer, name, value, threshold, PreprocessEstimatedBaseAreaLowException::new
		);
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
	public static Optional<PreprocessEstimatedBaseAreaLowException> check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-13);
		return Optional.empty();
	}

}
