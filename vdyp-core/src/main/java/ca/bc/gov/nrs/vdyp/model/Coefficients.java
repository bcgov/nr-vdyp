package ca.bc.gov.nrs.vdyp.model;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;

/**
 * Fixed length list of floats that can be accessed using an offset index
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class Coefficients extends AbstractList<Float> implements List<Float> {
	float[] coe;
	int indexFrom;

	public Coefficients(float[] coe, int indexFrom) {
		this.coe = coe;
		this.indexFrom = indexFrom;
	}

	public Coefficients(List<Float> coe, int indexFrom) {

		this(listToArray(coe), indexFrom);
	}

	private static float[] listToArray(List<Float> coe) {
		float[] floatArray = new float[coe.size()];
		int i = 0;

		for (Float f : coe) {
			floatArray[i++] = (f != null ? f : Float.NaN);
		}
		return floatArray;
	}

	public Float get(int i) {
		return coe[i];
	}

	public Float getCoe(int i) {
		return coe[i - indexFrom];
	}

	public void modifyCoe(int i, UnaryOperator<Float> op) {
		setCoe(i, op.apply(getCoe(i)));
	}

	public Float setCoe(int i, float value) {
		return coe[i - indexFrom] = value;
	}

	@Override
	public int size() {
		return coe.length;
	}

	@Override
	public boolean addAll(Collection<? extends Float> c) {
		return false;
	}

	/**
	 * Create a list of all the same float value
	 * 
	 * @param size  number of elements
	 * @param value the value to repeat
	 * @return
	 */
	public static List<Float> sameSize(int size, float value) {
		return DoubleStream.generate(() -> value).limit(size).mapToObj(x -> (float) x).toList();
	}

	/**
	 * Create an empty (all 0.0) Coefficents object
	 * 
	 * @param size
	 * @param indexFrom
	 * @return
	 */
	public static Coefficients empty(int size, int indexFrom) {
		return new Coefficients(sameSize(size, 0f), indexFrom);
	}

	/**
	 * Index of first coefficient
	 */
	public int getIndexFrom() {
		return indexFrom;
	}

}
