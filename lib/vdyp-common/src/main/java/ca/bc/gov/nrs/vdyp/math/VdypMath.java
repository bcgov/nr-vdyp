package ca.bc.gov.nrs.vdyp.math;

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
