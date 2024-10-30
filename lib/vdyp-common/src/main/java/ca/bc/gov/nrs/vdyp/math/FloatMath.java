package ca.bc.gov.nrs.vdyp.math;

import java.io.IOException;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;

public class FloatMath {

	private FloatMath() {
	}

	// wrap standard library double math functions to work with floats so equations
	// aren't littered with explicit casts

	public static float log(float f) {

		return (float) Math.log(f);
	}

	public static float exp(float f) {
		return (float) Math.exp(f);
	}

	public static float pow(float b, float e) {
		return (float) Math.pow(b, e);
	}

	public static float abs(float f) {
		return Math.abs(f);
	}

	public static float sqrt(float f) {
		return (float) Math.sqrt(f);
	}

	public static float floor(float f) {
		return (float) Math.floor(f);
	}

	public static float ceil(float f) {
		return (float) Math.ceil(f);
	}

	public static float clamp(float x, float min, float max) {
		assert max >= min : "Maximum " + max + " was less than minimum " + min;
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	public static float ratio(float arg, float radius) {
		if (arg < -radius) {
			return 0.0f;
		} else if (arg > radius) {
			return 1.0f;
		}
		return exp(arg) / (1.0f + exp(arg));
	}

	public static String toString(float[] vector) {
		var builder = new StringBuilder();
		try {
			toString(vector, builder);
		} catch (IOException e) {
			throw new IllegalStateException("StringBuilder should not throw IOException", e);
		}
		return builder.toString();
	}

	public static void toString(float[] vector, Appendable builder) throws IOException {
		builder.append("[");
		var first = true;
		for (var x : vector) {
			if (!first)
				builder.append(", ");
			first = false;
			builder.append(Float.toString(x));
		}
		builder.append("]");
	}

	public static float exponentRatio(float logit) throws ProcessingException {
		float exp = safeExponent(logit);
		return exp / (1f + exp);
	}

	public static float safeExponent(float logit) throws ProcessingException {
		if (logit > 88f) {
			throw new ProcessingException("logit " + logit + " exceeds 88");
		}
		return exp(logit);
	}

	/**
	 * Compute the maximum of three float values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @return as described
	 */
	public static float max(float f1, float f2, float f3) {
		return Math.max(f1, Math.max(f2, f3));
	}

	/**
	 * Compute the maximum of four float values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @param f4
	 * @return as described
	 */
	public static float max(float f1, float f2, float f3, float f4) {
		return Math.max(Math.max(f1, f2), Math.max(f3, f4));
	}
}
