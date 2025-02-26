package ca.bc.gov.nrs.vdyp.math;

import java.io.IOException;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;

public class VdypMath {

	private VdypMath() {
	}

	public static double clamp(double x, double min, double max) {
		assert max >= min : "Maximum " + max + " was less than minimum " + min;
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	public static int clamp(int x, int min, int max) {
		assert max >= min : "Maximum " + max + " was less than minimum " + min;
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	public static double ratio(double arg, double radius) {
		if (arg < -radius) {
			return 0.0f;
		} else if (arg > radius) {
			return 1.0f;
		}
		return Math.exp(arg) / (1.0f + Math.exp(arg));
	}

	public static String toString(double[] vector) {
		var builder = new StringBuilder();
		try {
			toString(vector, builder);
		} catch (IOException e) {
			throw new IllegalStateException("StringBuilder should not throw IOException", e);
		}
		return builder.toString();
	}

	public static void toString(double[] vector, Appendable builder) throws IOException {
		builder.append("[");
		var first = true;
		for (var x : vector) {
			if (!first)
				builder.append(", ");
			first = false;
			builder.append(Double.toString(x));
		}
		builder.append("]");
	}

	public static String toString(int[] vector) {
		var builder = new StringBuilder();
		try {
			toString(vector, builder);
		} catch (IOException e) {
			throw new IllegalStateException("StringBuilder should not throw IOException", e);
		}
		return builder.toString();
	}

	public static void toString(int[] vector, Appendable builder) throws IOException {
		builder.append("[");
		var first = true;
		for (var x : vector) {
			if (!first)
				builder.append(", ");
			first = false;
			builder.append(Double.toString(x));
		}
		builder.append("]");
	}

	public static double exponentRatio(double logit) throws ProcessingException {
		double exp = safeExponent(logit);
		return exp / (1f + exp);
	}

	public static double safeExponent(double logit) throws ProcessingException {
		if (logit > 88f) {
			throw new ProcessingException("logit " + logit + " exceeds 88");
		}
		return Math.exp(logit);
	}

	/**
	 * Compute the maximum of three double values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @return as described
	 */
	public static double max(double f1, double f2, double f3) {
		return Math.max(f1, Math.max(f2, f3));
	}

	/**
	 * Compute the maximum of four double values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @param f4
	 * @return as described
	 */
	public static double max(double f1, double f2, double f3, double f4) {
		return Math.max(Math.max(f1, f2), Math.max(f3, f4));
	}

	/**
	 * Compute the maximum of three int values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @return as described
	 */
	public static int max(int f1, int f2, int f3) {
		return Math.max(f1, Math.max(f2, f3));
	}

	/**
	 * Compute the maximum of four int values, using <code>Math.max</code> to do pairwise comparisons.
	 *
	 * @param f1
	 * @param f2
	 * @param f3
	 * @param f4
	 * @return as described
	 */
	public static int max(int f1, int f2, int f3, int f4) {
		return Math.max(Math.max(f1, f2), Math.max(f3, f4));
	}
}
