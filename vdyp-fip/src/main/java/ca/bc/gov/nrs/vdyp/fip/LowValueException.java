package ca.bc.gov.nrs.vdyp.fip;

/**
 * A given or computer value was lower than expected.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class LowValueException extends ProcessingException {

	private static final long serialVersionUID = 4242156378933396508L;
	final private float value;
	final private float threshold;

	public LowValueException(String message, float value, float threshold) {
		super(String.format("%s %f was lass than %f", message, value, threshold));
		this.value = value;
		this.threshold = threshold;
	}

	public float getValue() {
		return value;
	}

	public float getThreshold() {
		return threshold;
	}

}
