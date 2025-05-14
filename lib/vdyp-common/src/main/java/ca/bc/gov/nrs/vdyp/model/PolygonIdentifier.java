package ca.bc.gov.nrs.vdyp.model;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

public class PolygonIdentifier {

	public static final int MAPSHEET_LENGTH = 9;
	public static final int POLYGON_NUMBER_LENGTH = 12;
	public static final int YEAR_LENGTH = 4;
	public static final int BASE_LENGTH = MAPSHEET_LENGTH + POLYGON_NUMBER_LENGTH;
	public static final int ID_LENGTH = BASE_LENGTH + YEAR_LENGTH;

	private final String base;
	private final int year;

	public PolygonIdentifier(String base, int year) throws IllegalArgumentException {
		if (base.length() > BASE_LENGTH)
			throw new IllegalArgumentException("Polygon identifier base \"" + base + "\" is too long.");
		if (year < 1)
			throw new IllegalArgumentException("Polygon identifier year " + year + " must be positive.");
		this.base = base;
		this.year = year;
	}

	public PolygonIdentifier(String mapSheet, Long polygonNumber, int measurementYear) {
		this.base = mapSheet + "         ".substring(mapSheet.length()) + polygonNumber;
		this.year = measurementYear;
	}

	public static PolygonIdentifier split(String polygonIdentifier) throws IllegalArgumentException {
		if (polygonIdentifier.length() != ID_LENGTH)
			throw new IllegalArgumentException(
					"Polygon identifier \"" + polygonIdentifier + "\" must be exactly " + ID_LENGTH + " characters."
			);
		String base = polygonIdentifier.substring(0, BASE_LENGTH).trim();
		String year = polygonIdentifier.substring(BASE_LENGTH, ID_LENGTH).trim();

		return new PolygonIdentifier(base, Integer.parseInt(year));
	}

	private static final String FORMAT = "%-" + BASE_LENGTH + "s%" + YEAR_LENGTH + "d";
	private static final String COMPACT_FORMAT = "%s(%" + YEAR_LENGTH + "d)";

	/**
	 * Return the <code>base</code> (a.k.a. <code>name</code>) of the Polygon. These terms are synonomous.
	 *
	 * @return as described
	 */
	public String getBase() {
		return base;
	}

	/**
	 * Return the <code>name</code> (a.k.a. <code>base</code>) of the Polygon. These terms are synonomous.
	 *
	 * @return as described
	 */
	public String getName() {
		return base;
	}

	public int getYear() {
		return year;
	}

	@Override
	public String toString() {
		String result = FORMAT.formatted(base, year);
		assert (result.length() == ID_LENGTH);
		return result;
	}

	public String toStringCompact() {
		return COMPACT_FORMAT.formatted(base, year);
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, year);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof PolygonIdentifier other)
			return Objects.equals(base, other.base) && year == other.year;
		else
			return false;
	}

	public PolygonIdentifier forYear(int year) {
		return new PolygonIdentifier(getBase(), year);
	}

	public PolygonIdentifier forYear(IntUnaryOperator op) {
		return forYear(op.applyAsInt(getYear()));
	}
}
