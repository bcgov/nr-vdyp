package ca.bc.gov.nrs.vdyp.sindex.calculators;

import java.util.function.DoubleUnaryOperator;

public interface SiteIndexUtilities {

	public static double ppow(double x, double y) {
		return (x <= 0) ? 0.0 : Math.pow(x, y);
	}

	public static double llog(double x) {
		return (x <= 0.0) ? Math.log(.00001) : Math.log(x);
	}

	public static final double METRES_PER_FOOT = 0.3048f;

	/**
	 * Converts the given value from metres to feet, does the given operation, and converts the result from feet to
	 * metres.
	 *
	 * @param value
	 * @param operation
	 * @return
	 */
	public static double computeInFeet(double value, DoubleUnaryOperator operation) {
		return operation.applyAsDouble(value / METRES_PER_FOOT) * METRES_PER_FOOT;
	}

}
