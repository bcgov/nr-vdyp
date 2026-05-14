package ca.bc.gov.nrs.vdyp.common;

/**
 * Applies a doubleing point operator for a particular indexed location in a sequence
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
@FunctionalInterface
public interface IndexedDoubleBinaryOperator {
	/**
	 * Applies this operator to the given operand.
	 *
	 * @param left  the first operand
	 * @param right the second operand
	 * @param index the index
	 * @return the operator result
	 */
	double applyAsDoubleWithIndex(double left, double right, int index);
}
