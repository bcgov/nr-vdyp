package ca.bc.gov.nrs.vdyp.common;

/**
 * See {@link java.util.function.DoubleUnaryOperator}
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
@FunctionalInterface
public interface DoubleUnaryOperator extends IndexedDoubleUnaryOperator {
	/**
	 * Applies this operator to the given operand.
	 *
	 * @param value the operand
	 * @return the operator result
	 */
	double applyAsDouble(double value);

	@Override
	default double applyAsDoubleWithIndex(double value, int index) {
		return applyAsDouble(value);
	}
}
