package ca.bc.gov.nrs.vdyp.common_calculators;

import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.ClassErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.ForestInventoryZoneException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.SpeciesErrorException;

/* @formatter:off */
/**
 * SiteClassCode2SiteIndex.java
 * - translates site class code to site index (height in metres) for a
 *   given species, site class, and FIZ.
 * - the translation is intended to be used where total age is small (under 30 years),
 *   where site index based on height may not be reliable.
 * - primarily used by VDYP and FredTab.
 * - the origin of the values used here is Inventory Branch.
 */
/* @formatter:on */
public class SiteClassCode2SiteIndex {
/* @formatter:off */
/*
 * 1994 oct 19 - Moved here from FredTab.
 * 1996 jun 27 - Changed error return of -1 to -5.
 *             - Changed error return of -2 to -6.
 *             - Changed error return of -3 to -7.
 *      aug 8  - Changed error codes to defined constants.
 *      oct 22 - Changed MB_HARRING to MB_THROWER.
 *      nov 28 - Added SS_NIGH.
 *             - Started adding conditional compilation to curves.
 * 1997 mar 24 - Changed HW_WILEY to HWC_WILEY.
 *             - Added HWI_NIGH.
 *      aug 27 - Added conditional compilation around HWI_NIGH.
 *      nov 17 - Added Ea as At Goudie.
 *             - Added Lt and La as Lw Milner.
 *             - Added Pf as Pli Goudie.
 *             - Added Se as Sw Goudie.
 * 1998 nov 12 - Added Nigh & Courtin's Dr.
 * 1999 jan 8  - Changed int to short int.
 *             - Changed to take species index as parameter.
 * 2000 jul 24 - Split CW into CWI and CWC.
 * 2023 jul 14  - Translated like for like from C to Java
 * 			   -  Renamed from sc2si.c to SiteClassCode2SiteIndex
 */
/* @formatter:on */

	// Taken from sindex.h
	/* define species and equation indices */
	private static final int SI_SPEC_ACT = 5;
	private static final int SI_SPEC_AT = 8;
	private static final int SI_SPEC_BA = 11;
	private static final int SI_SPEC_BL = 16;
	private static final int SI_SPEC_CWC = 23;
	private static final int SI_SPEC_CWI = 24;
	private static final int SI_SPEC_DR = 29;
	private static final int SI_SPEC_EA = 31;
	private static final int SI_SPEC_EP = 34;
	private static final int SI_SPEC_FDC = 39;
	private static final int SI_SPEC_FDI = 40;
	private static final int SI_SPEC_HWC = 47;
	private static final int SI_SPEC_HWI = 48;
	private static final int SI_SPEC_LA = 57;
	private static final int SI_SPEC_LT = 59;
	private static final int SI_SPEC_LW = 60;
	private static final int SI_SPEC_MB = 62;
	private static final int SI_SPEC_PA = 76;
	private static final int SI_SPEC_PF = 77;
	private static final int SI_SPEC_PLI = 81;
	private static final int SI_SPEC_PW = 85;
	private static final int SI_SPEC_PY = 87;
	private static final int SI_SPEC_SB = 95;
	private static final int SI_SPEC_SE = 96;
	private static final int SI_SPEC_SS = 99;
	private static final int SI_SPEC_SW = 100;
	private static final int SI_SPEC_YC = 130;

	/*
	 * codes returned by fiz_check()
	 */
	private static final int FIZ_COAST = 1;
	private static final int FIZ_INTERIOR = 2;

	public static double class_to_index(short sp_index, char sitecl, char fiz) throws IllegalArgumentException {
		if (sitecl != 'G' && sitecl != 'M' && sitecl != 'P' && sitecl != 'L') {
			//return SI_ERR_CLASS;
			throw new ClassErrorException("Unknown site class code: " + sitecl);
			
		}

		switch (sp_index) {
		case SI_SPEC_ACT:
		case SI_SPEC_MB:
			switch (sitecl) {
			case 'G':
				return (double) 26;
			case 'M':
				return (double) 18;
			case 'P':
				return (double) 9;
			case 'L':
				return (double) 3;
			}
			break;
		case SI_SPEC_AT:
		case SI_SPEC_EA:
		case SI_SPEC_EP:
			switch (sitecl) {
			case 'G':
				return (double) 27;
			case 'M':
				return (double) 20;
			case 'P':
				return (double) 12;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_BA:
			switch (sitecl) {
			case 'G':
				return (double) 29;
			case 'M':
				return (double) 23;
			case 'P':
				return (double) 14;
			case 'L':
				return (double) 5;
			}
			break;
		case SI_SPEC_BL:
			switch (sitecl) {
			case 'G':
				return (double) 18;
			case 'M':
				return (double) 15;
			case 'P':
				return (double) 11;
			case 'L':
				return (double) 5;
			}
			break;
		case SI_SPEC_CWC:
		case SI_SPEC_YC:
			switch (sitecl) {
			case 'G':
				return (double) 29;
			case 'M':
				return (double) 23;
			case 'P':
				return (double) 15;
			case 'L':
				return (double) 6;
			}
			break;
		case SI_SPEC_CWI:
			switch (sitecl) {
			case 'G':
				return (double) 22;
			case 'M':
				return (double) 19;
			case 'P':
				return (double) 13;
			case 'L':
				return (double) 5;
			}
			break;
		case SI_SPEC_DR:
			switch (sitecl) {
			case 'G':
				return (double) 33;
			case 'M':
				return (double) 23;
			case 'P':
				return (double) 13;
			case 'L':
				return (double) 6;
			}
			break;
		case SI_SPEC_FDC:
			switch (sitecl) {
			case 'G':
				return (double) 32;
			case 'M':
				return (double) 27;
			case 'P':
				return (double) 18;
			case 'L':
				return (double) 7;
			}
			break;
		case SI_SPEC_FDI:
			switch (sitecl) {
			case 'G':
				return (double) 20;
			case 'M':
				return (double) 17;
			case 'P':
				return (double) 12;
			case 'L':
				return (double) 5;
			}
			break;
		case SI_SPEC_HWC:
			switch (FizCheck.fiz_check(fiz)) {
			case FIZ_COAST:
				switch (sitecl) {
				case 'G':
					return (double) 28;
				case 'M':
					return (double) 22;
				case 'P':
					return (double) 14;
				case 'L':
					return (double) 5;
				}
				break;
			case FIZ_INTERIOR:
				switch (sitecl) {
				case 'G':
					return (double) 21;
				case 'M':
					return (double) 18;
				case 'P':
					return (double) 12;
				case 'L':
					return (double) 4;
				}
				break;
			default:
				throw new ForestInventoryZoneException("Unknown FIZ code: " +fiz);

			}
			break;
		case SI_SPEC_HWI:
			switch (sitecl) {
			case 'G':
				return (double) 21;
			case 'M':
				return (double) 18;
			case 'P':
				return (double) 12;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_LA:
		case SI_SPEC_LT:
		case SI_SPEC_LW:
			switch (sitecl) {
			case 'G':
				return (double) 20;
			case 'M':
				return (double) 16;
			case 'P':
				return (double) 10;
			case 'L':
				return (double) 3;
			}
			break;
		case SI_SPEC_PLI:
		case SI_SPEC_PA:
		case SI_SPEC_PF:
			switch (sitecl) {
			case 'G':
				return (double) 20;
			case 'M':
				return (double) 16;
			case 'P':
				return (double) 11;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_PY:
			switch (sitecl) {
			case 'G':
				return (double) 17;
			case 'M':
				return (double) 14;
			case 'P':
				return (double) 10;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_PW:
			switch (sitecl) {
			case 'G':
				return (double) 28;
			case 'M':
				return (double) 22;
			case 'P':
				return (double) 12;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_SS:
			switch (sitecl) {
			case 'G':
				return (double) 28;
			case 'M':
				return (double) 21;
			case 'P':
				return (double) 11;
			case 'L':
				return (double) 4;
			}
			break;
		case SI_SPEC_SB:
		case SI_SPEC_SW:
		case SI_SPEC_SE:
			switch (sitecl) {
			case 'G':
				return (double) 19;
			case 'M':
				return (double) 15;
			case 'P':
				return (double) 10;
			case 'L':
				return (double) 5;
			}
			break;
		}
		throw new SpeciesErrorException("Unknown species index: " + sitecl);
	}

}
