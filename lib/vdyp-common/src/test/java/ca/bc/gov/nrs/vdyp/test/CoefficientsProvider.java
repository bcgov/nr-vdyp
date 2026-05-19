package ca.bc.gov.nrs.vdyp.test;

public interface CoefficientsProvider<T> {
	T create(double[] values, int startIndex);

	double getCoe(T unit, int index);

	T pairwise(T left, T right);

	void pairwiseInPlace(T left, T right);

	T scalar(T unit);

	void scalarInPlace(T unit);

	void scalarInPlace(int i, T unit);

	T indexedPairwise(T left, T right);

	void indexedPairwiseInPlace(T left, T right);

	T indexedScalar(T unit);

	void indexedScalarInPlace(T unit);

	void indexedScalarInPlace(int i, T unit);

	void assertValues(T unit, int startIndex, Double... expected);
}
