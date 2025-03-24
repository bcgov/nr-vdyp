package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Crown closure is low or has not been set.
 *
 * Equivalent to IPASS= -9 for VRI
 */
public class CrownClosureLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Crown closure";

	CrownClosureLowException(RuntimeStandProcessingException cause) {
		super(cause, CrownClosureLowException.class);
	}

	CrownClosureLowException(LayerType layer, Optional<Float> value, Optional<Float> threshold) {
		this(layer, DEFAULT_NAME, value, threshold);
	}

	CrownClosureLowException(LayerType layer, String name, Optional<Float> value, Optional<Float> threshold) {
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
	public static Optional<CrownClosureLowException>
			check(LayerType layer, String name, Optional<Float> value, float threshold) {
		return LayerValueLowException.check(layer, name, value, threshold, CrownClosureLowException::new);
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
	public static Optional<CrownClosureLowException> check(LayerType layer, Optional<Float> value, float threshold) {
		return check(layer, DEFAULT_NAME, value, threshold);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-9);
		return Optional.empty();
	}

}
