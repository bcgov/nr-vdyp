package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;

import ca.bc.gov.nrs.vdyp.common.DoubleBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.DoubleUnaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedDoubleBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedDoubleUnaryOperator;
import ca.bc.gov.nrs.vdyp.model.DoubleCoefficients;

public class DoubleCoefficientsProvider implements CoefficientsProvider<DoubleCoefficients> {
	private final DoubleBinaryOperator binaryTestOperator;
	private final DoubleUnaryOperator unaryTestOperator;
	private final IndexedDoubleBinaryOperator iBinaryTestOperator;
	private final IndexedDoubleUnaryOperator iUnaryTestOperator;

	public DoubleCoefficientsProvider(
			DoubleBinaryOperator binaryTestOperator, DoubleUnaryOperator unaryTestOperator,
			IndexedDoubleBinaryOperator iBinaryTestOperator, IndexedDoubleUnaryOperator iUnaryTestOperator
	) {
		this.binaryTestOperator = binaryTestOperator;
		this.unaryTestOperator = unaryTestOperator;
		this.iBinaryTestOperator = iBinaryTestOperator;
		this.iUnaryTestOperator = iUnaryTestOperator;
	}

	@Override
	public DoubleCoefficients create(double[] values, int startIndex) {
		return new DoubleCoefficients(values, startIndex);
	}

	@Override
	public double getCoe(DoubleCoefficients unit, int index) {
		return unit.getCoe(index);
	}

	@Override
	public DoubleCoefficients pairwise(DoubleCoefficients left, DoubleCoefficients right) {
		return left.pairwise(right, binaryTestOperator);
	}

	@Override
	public void pairwiseInPlace(DoubleCoefficients left, DoubleCoefficients right) {
		left.pairwiseInPlace(right, binaryTestOperator);
	}

	@Override
	public DoubleCoefficients scalar(DoubleCoefficients unit) {
		return unit.scalar(unaryTestOperator);
	}

	@Override
	public void scalarInPlace(DoubleCoefficients unit) {
		unit.scalarInPlace(unaryTestOperator);
	}

	@Override
	public void scalarInPlace(int i, DoubleCoefficients unit) {
		unit.scalarInPlace(i, unaryTestOperator);
	}

	@Override
	public DoubleCoefficients indexedPairwise(DoubleCoefficients left, DoubleCoefficients right) {
		return left.pairwise(right, iBinaryTestOperator);
	}

	@Override
	public void indexedPairwiseInPlace(DoubleCoefficients left, DoubleCoefficients right) {
		left.pairwiseInPlace(right, iBinaryTestOperator);
	}

	@Override
	public DoubleCoefficients indexedScalar(DoubleCoefficients unit) {
		return unit.scalar(iUnaryTestOperator);
	}

	@Override
	public void indexedScalarInPlace(DoubleCoefficients unit) {
		unit.scalarInPlace(iUnaryTestOperator);
	}

	@Override
	public void indexedScalarInPlace(int i, DoubleCoefficients unit) {
		unit.scalarInPlace(i, iUnaryTestOperator);
	}

	@Override
	public void assertValues(DoubleCoefficients unit, int startIndex, Double... expected) {
		assertThat(unit, VdypMatchers.dcoe(startIndex, expected));
	}

}
