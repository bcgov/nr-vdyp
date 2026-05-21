package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;

import ca.bc.gov.nrs.vdyp.common.FloatBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.FloatUnaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedFloatBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedFloatUnaryOperator;
import ca.bc.gov.nrs.vdyp.model.Coefficients;

public class FloatCoefficientsProvider implements CoefficientsProvider<Coefficients> {
	private final FloatBinaryOperator binaryTestOperator;
	private final FloatUnaryOperator unaryTestOperator;
	private final IndexedFloatBinaryOperator iBinaryTestOperator;
	private final IndexedFloatUnaryOperator iUnaryTestOperator;

	public FloatCoefficientsProvider(
			FloatBinaryOperator binaryTestOperator, FloatUnaryOperator unaryTestOperator,
			IndexedFloatBinaryOperator iBinaryTestOperator, IndexedFloatUnaryOperator iUnaryTestOperator
	) {
		this.binaryTestOperator = binaryTestOperator;
		this.unaryTestOperator = unaryTestOperator;
		this.iBinaryTestOperator = iBinaryTestOperator;
		this.iUnaryTestOperator = iUnaryTestOperator;
	}

	@Override
	public Coefficients create(double[] values, int startIndex) {
		var floats = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			floats[i] = (float) values[i];
		}
		return new Coefficients(floats, startIndex);
	}

	@Override
	public double getCoe(Coefficients unit, int index) {
		return unit.getCoe(index);
	}

	@Override
	public Coefficients pairwise(Coefficients left, Coefficients right) {
		return left.pairwise(right, binaryTestOperator);
	}

	@Override
	public void pairwiseInPlace(Coefficients left, Coefficients right) {
		left.pairwiseInPlace(right, binaryTestOperator);
	}

	@Override
	public Coefficients scalar(Coefficients unit) {
		return unit.scalar(unaryTestOperator);
	}

	@Override
	public void scalarInPlace(Coefficients unit) {
		unit.scalarInPlace(unaryTestOperator);
	}

	@Override
	public void scalarInPlace(int i, Coefficients unit) {
		unit.scalarInPlace(i, unaryTestOperator);
	}

	@Override
	public Coefficients indexedPairwise(Coefficients left, Coefficients right) {
		return left.pairwise(right, iBinaryTestOperator);
	}

	@Override
	public void indexedPairwiseInPlace(Coefficients left, Coefficients right) {
		left.pairwiseInPlace(right, iBinaryTestOperator);
	}

	@Override
	public Coefficients indexedScalar(Coefficients unit) {
		return unit.scalar(iUnaryTestOperator);
	}

	@Override
	public void indexedScalarInPlace(Coefficients unit) {
		unit.scalarInPlace(iUnaryTestOperator);
	}

	@Override
	public void indexedScalarInPlace(int i, Coefficients unit) {
		unit.scalarInPlace(i, iUnaryTestOperator);
	}

	@Override
	public void assertValues(Coefficients unit, int startIndex, Double... expected) {

		Float[] fExpected = new Float[expected.length];
		for (int i = 0; i < expected.length; i++) {
			fExpected[i] = expected[i].floatValue();
		}
		assertThat(unit, VdypMatchers.coe(startIndex, fExpected));
	}

}
