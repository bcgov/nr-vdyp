package ca.bc.gov.nrs.vdyp.model;

/**
 * A read only copy of a {@link DoubleCoefficients} object, for coefficients that are shared between runs. This is the
 * double precision counterpart of {@link ReadOnlyCoefficients}, and the two should be kept functionally equivalent.
 * Reading works as for {@link DoubleCoefficients}; any attempt to change a value throws
 * {@link UnsupportedOperationException}. Operations that return a new object, such as {@link #pairwise} and
 * {@link #scalar}, return ordinary modifiable {@link DoubleCoefficients}.
 */
public class ReadOnlyDoubleCoefficients extends DoubleCoefficients {

	private final double[] values;

	private ReadOnlyDoubleCoefficients(double[] values, int indexFrom) {
		super(values, indexFrom);
		this.values = values;
	}

	/**
	 * @return a read only copy of {@code source}, or {@code source} itself if it is already read only
	 * @throws IllegalArgumentException if {@code source} is a subclass of {@link DoubleCoefficients}, whose type a read
	 *                                  only copy would not keep
	 */
	public static DoubleCoefficients of(DoubleCoefficients source) {
		if (source instanceof ReadOnlyDoubleCoefficients) {
			return source;
		}
		if (source.getClass() != DoubleCoefficients.class) {
			throw new IllegalArgumentException(
					"No read only form is defined for " + source.getClass().getName() + "; add one to "
							+ ReadOnlyDoubleCoefficients.class.getSimpleName()
			);
		}
		var copy = new double[source.size()];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = source.getCoe(source.getIndexFrom() + i);
		}
		return new ReadOnlyDoubleCoefficients(copy, source.getIndexFrom());
	}

	@Override
	public void setCoe(int i, double value) {
		throw new UnsupportedOperationException(
				"These coefficients are shared between runs and are read only; copy them before modifying"
		);
	}

	/**
	 * Returns a read only view of these coefficients indexed from the given value.
	 */
	@Override
	public DoubleCoefficients reindex(int indexFrom) {
		return new ReadOnlyDoubleCoefficients(values, indexFrom);
	}
}
