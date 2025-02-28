package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.List;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

public class GrowthFiatDetails {
	private static final int N_AGES = 4;
	private static final int N_MIXED_COEFFICIENTS = 3;
	private static final int N_ENTRIES = N_AGES * 2 + N_MIXED_COEFFICIENTS;

	private final Region region;

	private int nAgesSupplied;
	private final Float[] ages;
	private final Float[] coefficients;
	private final Float[] mixedCoefficients;

	/**
	 * Create a GrowthFiat model. The first 8 values is a list of four pairs of (age, coefficient.) The list may be
	 * terminated by supplying 0 as the age value in entries that are not to be considered part of the list. The ages in
	 * all previous entries must be monatonically increasing. The coefficients of all pairs whose age is 0 must be 0,
	 * too.
	 *
	 * @param regionId     either 1 (coast) or 2 (interior)
	 * @param coefficients (11: 0-7 N_AGES pairs of (age, coefficient), 8-10 mixed coefficients)
	 *
	 * @throws ResourceParseException
	 */
	public GrowthFiatDetails(int regionId, List<Float> numbers) throws ResourceParseException {

		if (numbers == null || numbers.size() != N_ENTRIES) {
			throw new ResourceParseException("numbers is null or does not contain " + N_ENTRIES + " entries");
		}

		if (regionId == 1) {
			region = Region.COASTAL;
		} else if (regionId == 2) {
			region = Region.INTERIOR;
		} else {
			throw new ResourceParseException(
					"region must have the value \"1\" or \"2\"; instead, saw \"" + regionId + "\""
			);
		}

		nAgesSupplied = N_AGES;
		for (int i = 0; i < N_AGES; i++) {
			if (nAgesSupplied < N_AGES) {
				if (numbers.get(i * 2) != 0.0) {
					throw new ResourceParseException(
							MessageFormat.format(
									"All ages after the first 0 must be 0, too. Instead, saw \"{0}\"",
									numbers.get(i * 2)
							)
					);
				}
			} else if (numbers.get(i * 2) == 0.0) {
				nAgesSupplied = i;
			}
		}

		ages = new Float[N_AGES];
		ages[0] = numbers.get(0);
		for (int i = 1; i < N_AGES; i++) {
			ages[i] = numbers.get(i * 2);
			if (i < nAgesSupplied && ages[i] < ages[i - 1]) {
				throw new ResourceParseException(
						MessageFormat.format(
								"Non-zero age values must be monatonically increasing"
										+ " Instead, saw \"{0}\" followed by \"{1}\"",
								ages[i - 1], ages[i]
						)
				);
			}
		}

		coefficients = new Float[N_AGES];
		coefficients[0] = numbers.get(1);
		for (int i = 1; i < N_AGES; i++) {
			coefficients[i] = numbers.get(i * 2 + 1);
			if (i >= nAgesSupplied && coefficients[i] != 0.0f) {
				throw new ResourceParseException(
						MessageFormat.format(
								"Coefficients of ages with value 0 must also have value 0" + " Instead, saw \"{0}\"",
								coefficients[i]
						)
				);
			}
		}

		mixedCoefficients = new Float[N_MIXED_COEFFICIENTS];
		for (int i = 0; i < N_MIXED_COEFFICIENTS; i++) {
			mixedCoefficients[i] = numbers.get(2 * N_AGES + i);
		}
	}

	public Region getRegion() {
		return region;
	}

	public int getNAgesSupplied() {
		return nAgesSupplied;
	}

	public Float getAge(int index) {
		if (index < 0 || index >= N_AGES) {
			throw new IllegalArgumentException("GrowthFiatDetails.getAge: index");
		}
		return ages[index];
	}

	public Float getCoefficient(int index) {
		if (index < 0 || index >= N_AGES) {
			throw new IllegalArgumentException("GrowthFiatDetails.getCoefficient: index");
		}
		return coefficients[index];
	}

	public Float getMixedCoefficient(int index) {
		if (index < 0 || index >= N_MIXED_COEFFICIENTS) {
			throw new IllegalArgumentException("GrowthFiatDetails.getMixedCoefficient: index");
		}
		return mixedCoefficients[index];
	}

	public Float[] getAges() {
		return ages;
	}

	public Float[] getCoefficients() {
		return coefficients;
	}

	public Float[] getMixedCoefficients() {
		return mixedCoefficients;
	}

	public float calculateCoefficient(float age) {

		if (nAgesSupplied == 0 || age <= ages[0]) {
			return getCoefficient(0);
		} else if (age >= ages[nAgesSupplied - 1]) {
			return getCoefficient(nAgesSupplied - 1);
		} else {
			for (int j = nAgesSupplied - 1, i = j - 1; i >= 0; i--, j--) {
				if (age >= ages[i] && age < ages[j]) {

					return getCoefficient(i)
							+ (getCoefficient(j) - getCoefficient(i)) * (age - ages[i]) / (ages[j] - ages[i]);
				}
			}
		}

		throw new IllegalStateException(MessageFormat.format("age {0} was not found to be in range ", age));
	}
}
