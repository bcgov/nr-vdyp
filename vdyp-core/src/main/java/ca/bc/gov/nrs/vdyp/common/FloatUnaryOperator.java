package ca.bc.gov.nrs.vdyp.common;

/**
 * See {@link java.util.function.DoubleUnaryOperator}
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
@FunctionalInterface
public interface FloatUnaryOperator {
	/**
	 * Applies this operator to the given operand.
	 *
	 * @param value the operand
	 * @return the operator result
	 */
	float applyAsFloat(float value);
}
