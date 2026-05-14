package ca.bc.gov.nrs.vdyp.common;

/**
 * See {@link java.util.function.DoubleBinaryOperator}
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
@FunctionalInterface
public interface DoubleBinaryOperator extends IndexedDoubleBinaryOperator {
	/**
	 * Applies this operator to the given operands.
	 *
	 * @param left  the first operand
	 * @param right the second operand
	 * @return the operator result
	 */
	double applyAsDouble(double left, double right);

	@Override
	default double applyAsDoubleWithIndex(double left, double right, int index) {
		return applyAsDouble(left, right);
	}
}
