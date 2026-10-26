package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;

/**
 * A target year for growth which may be relative or absolute. Absolute years are assumed to be proleptic Gregorian year
 * numbers: 2 = 2 CE, 1 = 1 CE, 0 = 1 BCE, -1 = 2 BCE
 */
public class TargetYear {
	final int value;
	final boolean relative;

	/**
	 * There should be no growth
	 */
	public static final TargetYear NO_GROWTH = new TargetYear(0, true) {

		@Override
		public String toString() {
			return "No growth";
		}

		@Override
		public boolean isSpecified() {
			return false;
		}

	};

	/**
	 * Growth should be determined per polygon using a growth file. This will throw an exception if you call relativeTo.
	 */
	public static final TargetYear PER_POLYGON = new TargetYear(0, true) {

		@Override
		public int relativeTo(int baseYear) {
			throw new IllegalStateException("Flag for per polygon growth target can not be made relative.");
		}

		@Override
		public String toString() {
			return "Per polygon growth";
		}

		@Override
		public boolean isSpecified() {
			return false;
		}

	};

	public int getValue() {
		return value;
	}

	public boolean isRelative() {
		return relative;
	}

	public boolean isSpecified() {
		return true;
	}

	private TargetYear(int value, boolean relative) {
		this.value = value;
		this.relative = relative;
	}

	/**
	 * If this is a relative target, return the absolute year from the given base year. If this is an absolute target,
	 * simply return it.
	 * 
	 * @param baseYear Proleptic Gregorian base year.
	 * @return Proleptic Gregorian year number of the target year
	 */
	public int relativeTo(int baseYear) {
		if (relative) {
			return value + baseYear;
		}
		return value;
	}

	@Override
	public String toString() {
		if (relative) {
			return MessageFormat.format("{}{}", value < 0 ? "-" : "+", Math.abs(value));
		}
		int year = value;
		String era = "CE";
		if (value < 1) {
			year = 1 - year;
			era = "BCE";
		}
		return MessageFormat.format("{} {}", year, era);
	}

	/**
	 * Create a relative target year
	 * 
	 * @param offset Offset in years from the base year.
	 */
	public static TargetYear relative(int offset) {
		return new TargetYear(offset, true);
	}

	/**
	 * Create a TargetYear for an absolute year.
	 * 
	 * @param year Proleptic Gregorian year number for the target year.
	 */
	public static TargetYear absolute(int year) {
		return new TargetYear(year, false);
	}

	/**
	 * Assume numbers <=400 are offsets and larger numbers are absolute. Returns NO_GROWTH for 0 and PER_POLYGON for -1.
	 * 
	 * @param offset Offset in years from the base year.
	 */
	public static TargetYear fromControlVariable(int value) {
		if (value == 0) {
			return NO_GROWTH;
		}
		if (value == -1) {
			return PER_POLYGON;
		}
		if (value < 0) {
			throw new IllegalArgumentException("Target year " + value + " may not be negative");
		}
		if (value <= 400) {
			return relative(value);
		}
		if (value < 1920) {
			throw new IllegalArgumentException("Target year " + value + " may not be between 400 and 1920");
		}
		// TODO: might want to relax this
		if (value > 2400) {
			throw new IllegalArgumentException("Target year " + value + " may not be later than 2400");
		}
		return absolute(value);
	}

}
