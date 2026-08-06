package ca.bc.gov.nrs.vdyp.processing_state;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;

public class ProcessingStateTestUtils {

	public static Bank mockBank(BecDefinition bec, int nSpecies) {
		return Bank.mockBank(bec, nSpecies);
	}

	static public void fill(float[] array, float... values) {
		if (values.length != array.length) {
			throw new IllegalArgumentException(
					"Tried to fill array of size" + array.length + " with " + values.length + " values"
			);
		}
		System.arraycopy(values, 0, array, 0, values.length);
	}

	@SafeVarargs
	static public <T> void fill(T[] array, T... values) {
		if (values.length != array.length) {
			throw new IllegalArgumentException(
					"Tried to fill array of size" + array.length + " with " + values.length + " values"
			);
		}
		System.arraycopy(values, 0, array, 0, values.length);
	}
}
