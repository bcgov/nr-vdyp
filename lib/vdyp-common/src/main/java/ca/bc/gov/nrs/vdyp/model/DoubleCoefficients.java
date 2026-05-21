package ca.bc.gov.nrs.vdyp.model;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;

import ca.bc.gov.nrs.vdyp.common.DoubleBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.DoubleUnaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedDoubleBinaryOperator;
import ca.bc.gov.nrs.vdyp.common.IndexedDoubleUnaryOperator;

/**
 * Fixed length list of doubles that can be accessed using an offset index
 *
 * This class serves as a more precise implementation of the {@link Coefficients} class. It was implemented in an effort
 * to see if Java was losing precision from the coefficients read from control files.
 *
 * The implementations should remain in functionally equivalent, no edits should be made to one without edsiting the
 * other. They do not share an interface because of the desired primitive nature of the values held in the vectors.
 *
 * @author Peter Minter, Vivid Solutions
 *
 */
public class DoubleCoefficients extends AbstractList<Double> implements List<Double> {
	private double[] coe;
	private int indexFrom;

	public DoubleCoefficients(double[] coe, int indexFrom) {
		this.coe = coe;
		this.indexFrom = indexFrom;
	}

	public DoubleCoefficients(List<Double> coe, int indexFrom) {

		this(listToArray(coe), indexFrom);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < coe.length; i++) {
			sb.append(indexFrom + i).append(':').append(coe[i]).append(", ");
		}
		return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
	}

	private static double[] listToArray(List<Double> coe) {
		double[] doubleArray = new double[coe.size()];
		int i = 0;

		for (Double f : coe) {
			doubleArray[i++] = (f != null ? f : Double.NaN);
		}
		return doubleArray;
	}

	@Override
	public Double get(int i) {
		return coe[i];
	}

	public double getCoe(int i) {
		return coe[getRealIndex(i)];
	}

	public void setCoe(int i, double value) {
		coe[getRealIndex(i)] = value;
	}

	@Override
	public int size() {
		return coe.length;
	}

	@Override
	public boolean addAll(Collection<? extends Double> c) {
		return false;
	}

	/**
	 * Create a list of all the same double value
	 *
	 * @param size  number of elements
	 * @param value the value to repeat
	 * @return
	 */
	public static List<Double> sameSize(int size, double value) {
		return DoubleStream.generate(() -> value).limit(size).mapToObj(x -> (double) x).toList();
	}

	/**
	 * Create an empty (all 0.0) Coefficents object
	 *
	 * @param size
	 * @param indexFrom
	 * @return
	 */
	public static DoubleCoefficients empty(int size, int indexFrom) {
		return new DoubleCoefficients(sameSize(size, 0f), indexFrom);
	}

	/**
	 * Index of first coefficient
	 */
	public int getIndexFrom() {
		return indexFrom;
	}

	private int getRealIndex(int i) {
		var max = coe.length + indexFrom - 1;
		if (i < indexFrom || i > max) {
			throw new ArrayIndexOutOfBoundsException(
					"Coefficient index " + i + " out of bounds for [" + indexFrom + ":" + max + "]"
			);
		}
		return i - indexFrom;
	}

	/**
	 * Performs a pairwise operation in place with a compatible Coefficients object
	 *
	 * @param coe2 must have the same size and index offset
	 * @param op   operation to perform for each pair of coefficients
	 */
	public void pairwiseInPlace(DoubleCoefficients coe2, DoubleBinaryOperator op) {
		pairwiseInPlace(coe2, (IndexedDoubleBinaryOperator) op);
	}

	/**
	 * Performs a pairwise operation in place with a compatible Coefficients object
	 *
	 * @param coe2 must have the same size and index offset
	 * @param op   operation to perform for each pair of coefficients
	 */
	public void pairwiseInPlace(DoubleCoefficients coe2, IndexedDoubleBinaryOperator op) {
		checkCompatible(coe2);
		int max = getIndexFrom() + size();
		for (int i = getIndexFrom(); i < max; i++) {
			setCoe(i, op.applyAsDoubleWithIndex(getCoe(i), coe2.getCoe(i), i));
		}
	}

	private void checkCompatible(DoubleCoefficients coe2) throws IllegalArgumentException {
		if (coe2.getIndexFrom() != getIndexFrom()) {
			throw new IllegalArgumentException(
					"Expected Coefficients object indexed from " + getIndexFrom() + " but was indexed from "
							+ coe2.getIndexFrom()
			);
		}
		if (coe2.size() != size()) {
			throw new IllegalArgumentException(
					"Expected Coefficients object of size " + size() + " but was " + coe2.size()
			);
		}
	}

	/**
	 * Performs a pairwise operation with a compatible Coefficients object and returns the result.
	 *
	 * @param coe2 must have the same size and index offset
	 * @param op   operation to perform for each pair of coefficients
	 */
	public DoubleCoefficients pairwise(DoubleCoefficients coe2, DoubleBinaryOperator op) {
		return pairwise(coe2, (IndexedDoubleBinaryOperator) op);
	}

	/**
	 * Performs a pairwise operation with a compatible Coefficients object and returns the result.
	 *
	 * @param coe2 must have the same size and index offset
	 * @param op   operation to perform for each pair of coefficients
	 */
	public DoubleCoefficients pairwise(DoubleCoefficients coe2, IndexedDoubleBinaryOperator op) {
		var result = new DoubleCoefficients(this, this.getIndexFrom());
		result.pairwiseInPlace(coe2, op);
		return result;
	}

	/**
	 * Perform the operation on each coefficient in place
	 *
	 * @param op
	 */
	public void scalarInPlace(DoubleUnaryOperator op) {
		scalarInPlace((IndexedDoubleUnaryOperator) op);
	}

	/**
	 * Perform the operation on one particular coefficient in place
	 *
	 * @param op
	 */
	public void scalarInPlace(int i, IndexedDoubleUnaryOperator op) {
		setCoe(i, op.applyAsDoubleWithIndex(getCoe(i), i));
	}

	/**
	 * Perform the operation on each coefficient in place
	 *
	 * @param op
	 */
	public void scalarInPlace(IndexedDoubleUnaryOperator op) {
		int max = getIndexFrom() + size();
		for (int i = getIndexFrom(); i < max; i++) {
			scalarInPlace(i, op);
		}
	}

	/**
	 * Perform the operation on one particular coefficient in place
	 *
	 * @param op
	 */
	public void scalarInPlace(int i, DoubleUnaryOperator op) {
		setCoe(i, op.applyAsDouble(getCoe(i)));
	}

	/**
	 * Perform the operation on each coefficient and return the result
	 *
	 * @param op
	 * @return
	 */
	public DoubleCoefficients scalar(IndexedDoubleUnaryOperator op) {
		var result = new DoubleCoefficients(this, this.getIndexFrom());
		result.scalarInPlace(op);
		return result;
	}

	/**
	 * Perform the operation on each coefficient and return the result
	 *
	 * @param op
	 * @return
	 */
	public DoubleCoefficients scalar(DoubleUnaryOperator op) {
		return scalar((IndexedDoubleUnaryOperator) op);
	}

	/**
	 * Returns a view of this coefficients object indexed from the given value.
	 */
	public DoubleCoefficients reindex(int indexFrom) {
		return new DoubleCoefficients(this.coe, indexFrom);
	}
}
