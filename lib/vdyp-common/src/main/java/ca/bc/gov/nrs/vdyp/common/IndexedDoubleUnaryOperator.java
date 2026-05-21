package ca.bc.gov.nrs.vdyp.common;

/**
 * Applies a double floating point operator for a particular indexed location in a sequence
 *
 * This class represents a Double interface of {@link IndexedFloatUnaryOperator}
 *
 * @author Peter Minter, Vivid Solutions
 *
 */
@FunctionalInterface
public interface IndexedDoubleUnaryOperator {
	/**
	 * Applies this operator to the given operand.
	 *
	 * @param value the operand
	 * @param index the index
	 * @return the operator result
	 */
	double applyAsDoubleWithIndex(double value, int index);
}
