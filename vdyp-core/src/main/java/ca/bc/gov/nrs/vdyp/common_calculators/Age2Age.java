package ca.bc.gov.nrs.vdyp.common_calculators;

/* @formatter:off */
/**
 * Age2Age.java
 * given age and type, converts to other type of age.
 *
 * @throws IllegalArgumentException if nnkown age type is provided
 */
/* @formatter:on */
public class Age2Age {
/* @formatter:off */
/*
 * 1999 sep 23 - Created.
 * 2000 mar 15 - Added specific case for Nigh's 1999 Pli.
 *      jul 25 - Added Goudie/Nigh Sw.
 * 2001 sep 5  - Added Cwi Nigh, Dr Nigh, Fdc Bruce-Nigh, Fdc Nigh TA,
 *               Hwi Nigh, Lw Nigh, Pli Nigh TA98, SW Nigh TA, SS Nigh.
 * 2002 feb 12 - Added Sb Nigh.
 *      oct 9  - Added At Nigh.
 * 2003 jun 13 - Copied several curves and "corrected" the origin from
 *               bhage=0 ht=1.3 to bhage=0.5 ht=1.3.
 *               Added "AC" to the end of the define.
 * 2004 mar 26 - Added SI_SW_GOUDIE_NATAC.
 *      apr 28 - Added Nigh's 2002 Py.
 *             - Added Nigh's 2004 Pl/Sw/Se total age curves.
 *      may 4  - Added SI_SW_GOUDIE_PLAAC.
 *      jul 12 - Added PLI_THROWER which should have been in here since 1999.
 *             - Added check for return value going negative.
 * 2005 oct 20 - Added Huang's Pj.
 * 2009 aug 28 - Added Nigh's 2009 Ep.
 * 2010 mar 4  - Added Nigh's 2009 Ba.
 * 2023 jul 7  - Translated like for like from C to Java
 *             - Renamed from age2age to Age2Age
 */
/* @formatter:on */

//Taken from sindex.h
	/*
	 * age types
	 */
	private static final int SI_AT_TOTAL = 0;
	private static final int SI_AT_BREAST = 1;

	/* define species and equation indices */
	private static final int SI_ACB_HUANGAC = 97;
	private static final int SI_ACT_THROWERAC = 103;
	private static final int SI_AT_NIGH = 92;
	private static final int SI_BA_KURUCZ82AC = 102;
	private static final int SI_BA_NIGH = 118;
	private static final int SI_BL_CHENAC = 93;
	private static final int SI_BP_CURTISAC = 94;
	private static final int SI_CWC_KURUCZAC = 101;
	private static final int SI_CWI_NIGH = 77;
	private static final int SI_DR_NIGH = 13;
	private static final int SI_EP_NIGH = 116;
	private static final int SI_FDC_BRUCEAC = 100;
	private static final int SI_FDC_NIGHTA = 88;
	private static final int SI_FDI_THROWERAC = 96;
	private static final int SI_HM_MEANSAC = 95;
	private static final int SI_HWC_WILEYAC = 99;
	private static final int SI_HWI_NIGH = 37;
	private static final int SI_LW_NIGH = 90;
	private static final int SI_PJ_HUANG = 113;
	private static final int SI_PJ_HUANGAC = 114;
	private static final int SI_PLI_NIGHTA98 = 41;
	private static final int SI_PLI_THROWER = 45;
	private static final int SI_PLI_THROWNIGH = 40;
	private static final int SI_PW_CURTISAC = 98;
	private static final int SI_PY_HANNAC = 104;
	private static final int SI_PY_NIGH = 107;
	private static final int SI_SB_NIGH = 91;
	private static final int SI_SE_CHENAC = 105;
	private static final int SI_SS_NIGH = 59;
	private static final int SI_FDC_BRUCENIGH = 89;
	private static final int SI_SW_GOUDIE_NATAC = 106;
	private static final int SI_SW_GOUDIE_PLAAC = 112;
	private static final int SI_SW_GOUDNIGH = 85;
	private static final int SI_SW_NIGHTA = 83;
	/* not used, but must be defined for array positioning */
	private static final int SI_SE_NIGHTA = 110;
	private static final int SI_SW_NIGHTA2004 = 111;
	private static final int SI_PLI_NIGHTA2004 = 109;

	public static double age_to_age(short cu_index, double age1, short age1_type, short age2_type, double y2bh) throws IllegalArgumentException{
		double returnValue;

		boolean shouldBranch;
		switch (cu_index) {
		case SI_ACB_HUANGAC:
			shouldBranch = true;
		case SI_ACT_THROWERAC:
			shouldBranch = true;
		case SI_AT_NIGH:
			shouldBranch = true;
		case SI_BA_KURUCZ82AC:
			shouldBranch = true;
		case SI_BA_NIGH:
			shouldBranch = true;
		case SI_BL_CHENAC:
			shouldBranch = true;
		case SI_BP_CURTISAC:
			shouldBranch = true;
		case SI_CWC_KURUCZAC:
			shouldBranch = true;
		case SI_CWI_NIGH:
			shouldBranch = true;
		case SI_DR_NIGH:
			shouldBranch = true;
		case SI_EP_NIGH:
			shouldBranch = true;
		case SI_FDC_BRUCENIGH:
			shouldBranch = true;
		case SI_FDC_BRUCEAC:
			shouldBranch = true;
		case SI_FDC_NIGHTA:
			shouldBranch = true;
		case SI_FDI_THROWERAC:
			shouldBranch = true;
		case SI_HM_MEANSAC:
			shouldBranch = true;
		case SI_HWC_WILEYAC:
			shouldBranch = true;
		case SI_HWI_NIGH:
			shouldBranch = true;
		case SI_LW_NIGH:
			shouldBranch = true;
		case SI_PJ_HUANG:
			shouldBranch = true;
		case SI_PJ_HUANGAC:
			shouldBranch = true;
		case SI_PLI_NIGHTA2004:
			shouldBranch = true;
		case SI_PLI_NIGHTA98:
			shouldBranch = true;
		case SI_PLI_THROWNIGH:
			shouldBranch = true;
		case SI_PLI_THROWER:
			shouldBranch = true;
		case SI_PW_CURTISAC:
			shouldBranch = true;
		case SI_PY_HANNAC:
			shouldBranch = true;
		case SI_PY_NIGH:
			shouldBranch = true;
		case SI_SB_NIGH:
			shouldBranch = true;
		case SI_SE_CHENAC:
			shouldBranch = true;
		case SI_SE_NIGHTA:
			shouldBranch = true;
		case SI_SW_GOUDIE_NATAC:
			shouldBranch = true;
		case SI_SW_GOUDIE_PLAAC:
			shouldBranch = true;
		case SI_SW_GOUDNIGH:
			shouldBranch = true;
		case SI_SW_NIGHTA2004:
			shouldBranch = true;
		case SI_SW_NIGHTA:
			shouldBranch = true;
		case SI_SS_NIGH:
			shouldBranch = true;
		default:
			shouldBranch = false;
		}

		if (shouldBranch) {
			if (age1_type == SI_AT_BREAST) {
				if (age2_type == SI_AT_TOTAL) {
					/* convert to total age */
					returnValue = age1 + y2bh - 0.5;
					if (returnValue < 0) {
						returnValue = 0;
					}
					return returnValue;
				}
				throw new IllegalArgumentException("Unkown age type provided");	
			}

			if (age1_type == SI_AT_TOTAL) {
				if (age2_type == SI_AT_BREAST) {
					/* convert to breast-height age */
					returnValue = age1 - y2bh + 0.5;
					if (returnValue < 0) {
						returnValue = 0;
					}
					return returnValue;
				}
				throw new IllegalArgumentException("Unkown age type provided");	
			}
		} else {
			if (age1_type == SI_AT_BREAST) {
				if (age2_type == SI_AT_TOTAL) {
					/* convert to total age */
					returnValue = age1 + y2bh;
					if (returnValue < 0) {
						returnValue = 0;
					}
					return returnValue;
				}
				throw new IllegalArgumentException("Unkown age type provided");	
			}

			if (age1_type == SI_AT_TOTAL) {
				if (age2_type == SI_AT_BREAST) {
					/* convert to breast-height age */
					returnValue = age1 - y2bh;
					if (returnValue < 0) {
						returnValue = 0;
					}
					return returnValue;
				}
				throw new IllegalArgumentException("Unkown age type provided");	
			}
		}
		throw new IllegalArgumentException("Unkown age type provided");	
	}

}
