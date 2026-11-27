package ca.bc.gov.nrs.vdyp.model;

/**
 * A read only copy of a {@link Coefficients} object, for coefficients that are shared between runs. Reading works as
 * for {@link Coefficients}; any attempt to change a value throws {@link UnsupportedOperationException}. Operations that
 * return a new object, such as {@link #pairwise} and {@link #scalar}, return ordinary modifiable {@link Coefficients}.
 */
public class ReadOnlyCoefficients extends Coefficients {

	private final float[] values;

	private ReadOnlyCoefficients(float[] values, int indexFrom) {
		super(values, indexFrom);
		this.values = values;
	}

	/**
	 * @return a read only copy of {@code source}, or {@code source} itself if it is already read only
	 * @throws IllegalArgumentException if {@code source} is a subclass of {@link Coefficients} other than
	 *                                  {@link NonprimaryHLCoefficients}, whose type a read only copy would not keep
	 */
	public static Coefficients of(Coefficients source) {
		if (source instanceof ReadOnlyCoefficients || source instanceof ReadOnlyNonprimaryHLCoefficients) {
			return source;
		}
		if (source.getClass() == NonprimaryHLCoefficients.class) {
			return new ReadOnlyNonprimaryHLCoefficients((NonprimaryHLCoefficients) source);
		}
		if (source.getClass() != Coefficients.class) {
			throw new IllegalArgumentException(
					"No read only form is defined for " + source.getClass().getName() + "; add one to "
							+ ReadOnlyCoefficients.class.getSimpleName()
			);
		}
		return new ReadOnlyCoefficients(copyValues(source), source.getIndexFrom());
	}

	/**
	 * @return the values of {@code source} as a new array, without boxing or rounding
	 */
	static float[] copyValues(Coefficients source) {
		var copy = new float[source.size()];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = source.getCoe(source.getIndexFrom() + i);
		}
		return copy;
	}

	static UnsupportedOperationException readOnlyException() {
		return new UnsupportedOperationException(
				"These coefficients are shared between runs and are read only; copy them before modifying"
		);
	}

	@Override
	public void setCoe(int i, float value) {
		throw readOnlyException();
	}

	/**
	 * Returns a read only view of these coefficients indexed from the given value.
	 */
	@Override
	public Coefficients reindex(int indexFrom) {
		return new ReadOnlyCoefficients(values, indexFrom);
	}

	/**
	 * Read only form of {@link NonprimaryHLCoefficients}, which keeps the equation index.
	 */
	static class ReadOnlyNonprimaryHLCoefficients extends NonprimaryHLCoefficients {

		private final float[] values;

		private ReadOnlyNonprimaryHLCoefficients(NonprimaryHLCoefficients source) {
			this(copyValues(source), source.getEquationIndex());
		}

		private ReadOnlyNonprimaryHLCoefficients(float[] values, int equationIndex) {
			super(values, equationIndex);
			this.values = values;
		}

		@Override
		public void setCoe(int i, float value) {
			throw readOnlyException();
		}

		@Override
		public Coefficients reindex(int indexFrom) {
			return new ReadOnlyCoefficients(values, indexFrom);
		}
	}
}
