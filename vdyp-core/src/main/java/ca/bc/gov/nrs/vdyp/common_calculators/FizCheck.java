package ca.bc.gov.nrs.vdyp.common_calculators;

/**
 * fizcheck.c - determines whether a given FIZ code represents the coast or
 * interior.
 */
public class FizCheck {
/* @formatter:off */
 /*
 * 1994 oct 19 - Moved here from FredTab.
 * 1999 jan 8  - Changed int to short int.
 * 2023 jul 7  - Translated like for like from C to Java
 */
/* @formatter:on */

	// From sindex.h
	private static short FIZ_UNKNOWN = 0;
	private static short FIZ_COAST = 1;
	private static short FIZ_INTERIOR = 2;

	public static short fiz_check(char fiz) {
		switch (fiz) {
		case 'A':
		case 'B':
		case 'C':
			return FIZ_COAST;
		case 'D':
		case 'E':
		case 'F':
		case 'G':
		case 'H':
		case 'I':
		case 'J':
		case 'K':
		case 'L':
			return FIZ_INTERIOR;
		default:
			return FIZ_UNKNOWN;
		}
	}
}
