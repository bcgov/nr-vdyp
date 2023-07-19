package ca.bc.gov.nrs.vdyp.common_calculators;

import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.GrowthInterceptMinimumException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.LessThan13Exception;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;

/* @formatter:off */
/**
 * SiteIndex2Height.java
 * - given site index and age, computes site height
 * - age can be given as total age or breast height age
 * - if total age is given, y2bh must be the number of years to breast height
 * - all heights input/output are in metres.
 * - site index must be based on breast height age 50
 * - where breast height age is less than 0, a quadratic function is used
 * - error codes (returned as height value):
 *     SI_ERR_LT13: site index < 1.3m
 *     SI_ERR_GI_MIN: variable height growth intercept formulation; bhage < 0.5 years
 *     SI_ERR_GI_MAX: variable height growth intercept formulation; bhage > range
 *     SI_ERR_NO_ANS: iteration could not converge (projected height > 999)
 *     SI_ERR_CURVE: unknown curve index
 *     SI_ERR_GI_TOT: cannot compute growth intercept when using total age
 */
/* @formatter:on */
public class SiteIndex2Height {
/* @formatter:off */
/*
 *
 * 1990 may 31
 *      jun 8  - Added Fdi Monserud's equations.
 *               Added At Goudie's equation.
 *          11 - Added Dr Harrington & Curtis equation.
 *               Added Bg Cochran's equation.
 *               Added Bc Cochran's equation.
 *          15 - Changed most variable names, and file name, to include
 *               "si" at start.
 *      aug 29 - Added Pp Hann & Scrivani's equation.
 *               Added Lw Milner's equation.
 *      sep 25 - Changed approximation of height below breast height from
 *               linear to quadratic.
 *      oct 3  - Added programming checks for negative numbers to the
 *               pow() and log() math functions.
 *          11 - Modified computation of log function for values <= 0.
 *          22 - Switched Pli Goudie wet and dry to correct error.
 *      nov 9  - Bug fix in Fdi Vander Ploeg curves.
 *      dec 13 - Added Dempster's Sw, Sb, and Pli.
 *          17 - If incoming site index <= 0, make it a bit bigger.
 * 1991 jan 14 - Added Mb, Ea, Pw, Pa, and Yc, using other existing equations.
 *          16 - Added Fdc, Cwi and Fdi, by Hegyi.
 *          17 - Added Hw, Cw, and Ss Barker.
 *               Bug fix in Dr Harrington.  Age was not being properly
 *               converted from breast height basis to total age.
 *               Minor bug fix to Ba Kurucz (1982).  Added missing (extra)
 *               condition to applying correction to height, and added the
 *               correction code for ages less than years-to-breast-height.
 *          23 - Split Goudie's Pli, Sw, Pw, and Pa into natural and
 *               plantation versions.
 *          24 - Added Hegyi's black cottonwood.
 *      feb 4  - Changed EA to EP.
 *          12 - Added Curtis' Pw.
 *      mar 15 - Removed Goudie's Pw.
 *      jun 19 - Split Balsam into coast and interior.
 *          21 - Added Ker & Bowling's Bac, Sb, and Sw.
 *      jul 24 - Kurucz' Cw curves started bending down after about 400
 *               years.  The height correction was adjusted beyond 200 years.
 *      oct 21 - Added Cieszewski & Bella's Pl.
 *      nov 6  - Added Cieszewski & Bella's Sw, Sb, and At, and updated Pl.
 *      dec 2  - Changed to independent Sindex functions.
 * 1992 jan 10 - Added defines for how function prototypes and definitions
 *               are handled.
 *      feb 11 - Added Milner's Pp, Fdi, and Pli for 1992.
 *      apr 29 - Removed difference between plantations and natural stands
 *               for Pli Goudie and Pa Goudie.
 *      dec 2  - Added Mario Dilucca's coastal balsam fir.
 * 1993 feb 4  - Added Thrower's draft height-age curve for lodgepole pine.
 *      mar 22 - Added Thrower's draft height-age curve for black cottonwood.
 *          26 - Corrected coefficients for black cottonwood.
 *      sep 16 - Added Thrower's height-age curve for white spruce.
 * 1994 sep 27 - Copied Kurucz' 1982 Ba equation to Bg and Bc.
 *      oct 14 - Changed log(50) to log(50.0) in two places.  This had not
 *               been compiling correctly on Sun unix machines.
 * 1995 jun 12 - Added Goudie's convoluded Teak.
 *      sep 20 - Added Huang, Titus, & Lakusta equations for Sw, Pli, At,
 *               Sb, Pj, Acb, Bb, Fdi.
 *          21 - Modified check for incoming site index, so that it is at
 *               least 1.31m.
 *             - Added initial check to ensure y2bh at least 1.
 *      oct 13 - Updated Thrower's Pli and Sw.
 *      nov 3  - Added Nigh's 1995 draft Ss.
 *          20 - Removed white fir.
 *      dec 19 - Added Hm.
 * 1996 feb 8  - Changed coefficients of Nigh's draft Ss.
 *          12 - Tiny fix in Thrower's Pli, so that equation is used for
 *               age > 0.5 (not age >= 0.0), and that the y2bh has 0.5
 *               added to it before used below breast height age.
 *      jun 11 - Added gi_si2ht() function to iterate using growth
 *               intercept formulas.
 *          27 - Added proper error codes, instead of just returning 999.
 *      aug 2  - Amalgamated SI_SW_NIGH_PLA and SI_SW_NIGH_NAT into
 *               SI_SW_NIGH.
 *          8  - Changed error codes to defined constants.
 *             - Added check for incoming y2bh being > 0 before making sure
 *               it is at least 1.
 *             - Added check for valid incoming y2bh before computing
 *               height via growth intercept.
 *      oct 22 - Changed MB_HARRING to MB_THROWER.
 *      dec 11 - Eliminated some divide by zero problems when age is 0.
 * 1997 jan 6  - Changed Kurucz formulations to handle site index = 1.3.
 *      feb 5  - Changed check for top height or site index < 1.3 to be
 *               <= 1.3.
 *          11 - Adjusted Fdc Bruce so that if total age is <= 0, height
 *               is 0.
 *      mar 21 - Added Nigh's 1997 Hwi GI.
 *             - Changed define names: FDC_NIGH, HW_NIGH, PLI_NIGH, SW_NIGH
 *               all have "GI" added after them.
 *             - Added Nigh's 1997 Hwi.
 *             - Added Nigh's 1997 Pl GI.
 *             - Added Nigh's 1997 Fdi GI.
 *          24 - Split HW into HWI and HWC.
 *      oct 28 - Added Thrower's Bl GI.
 *      nov 17 - Added Ea as At Goudie.
 *             - Added Lt and La as Lw Milner.
 *             - Added Pf as Pli Goudie.
 *             - Added Se as Sw Goudie.
 * 1998 mar 13 - Added checks for high site for Wiley's Hwc, Kurucz' Cw,
 *               Kurucz' Bac, Harrington's Dr.
 *      apr 7  - Added inclusion of sindex2.h.
 *      nov 12 - Added Nigh & Courtin's 1998 Dr.
 *             - Added Nigh & Love's 1998 Pli.
 *          13 - Added hybrid Pli Thrower 1994 with Pli Nigh & Love 1998.
 *      dec 8  - Added Chen's Se, Bl, Pl.
 * 1999 jan 8  - Changed int to short int.
 *      feb 15 - Added Chen's Ep, Dr.
 *      apr 8  - Restricted Pli Nigh/Love to 0-15 years.
 *          14 - Added Chen's At.
 *          15 - Bug fix in Chen Ep and Dr.
 *      may 28 - Added Cameron's Ep.
 *      jun  9 - Added preliminary Cwi Nigh.
 *      aug  4 - Added "age > 0.5" constraint to Nigh's Dr.
 *          24 - Refined the 1998 Mar 13 check on those species/curves.
 *      sep 22 - Altered Pli Nigh 1999 to NOT correct by 0.5 years.
 *          24 - Added Curtis' Noble fir.
 *          30 - If age type is total, and age is 0, return error for
 *               certain curves.
 *      oct 1  - For those curves that "go nuts" at high site and low
 *               age, the return value is now a linear interpolation
 *               between two known good values.
 *          18 - Added Nigh's Hwc GI, SS GI, Sw GI, Lw GI.
 *             - Added Nigh/Love's Sw total age curve.
 * 2000 jan 27 - Added Nigh's Cw GI.
 *      mar 15 - Bug fix in Nigh/Thrower's spliced Pli curve.
 *      jul 24 - Split CW into CWC and CWI.
 *          25 - Added Nigh/Goudie's spliced Sw curve.
 *      oct 10 - Changed check for site <= 1.3 to < 1.3.
 *      nov 3  - Added Hm by Means/Campbell/Johnson.
 *      dec 12 - Changed BAC_KERR to BB_KER.
 * 2001 apr 9  - Added Fdc Nigh total age curve, and spliced with Bruce.
 *      may 3  - Added Lw curve by Brisco, Klinka, Nigh.
 *          10 - Bug fix in Monserud Fdi curve, 4.5 should have been
 *               subtracted from site_index before use.
 *      aug 27 - Added constraint of min si 14.2 to Nigh's Sw.
 *      sep 5  - For Fdc Bruce-Nigh, added in the missing part for
 *               use with breast-height age.
 * 2002 feb 5  - Modified equation for LW_NIGH.
 *          12 - Added Sb Nigh.
 *      jun 27 - Changed incoming y2bh to ensure steps of 0.5, 1.5, etc.
 *             - Simplified age by using tage and bhage.
 *      aug 21 - Changed Bruce-Nigh Fdc so that join point is tage 50, not 60.
 *      oct 9  - Added At Nigh.
 *      nov 19 - Bug fix in At Nigh.
 *          21 - Bug fix in BruceNigh Fdc.
 * 2003 jun 13 - Copied several curves and "corrected" the origin from
 *               bhage=0 ht=1.3 to bhage=0.5 ht=1.3.
 *               Added "AC" to the end of the define.
 * 2004 mar 26 - Copied sw_goudie_nat from 0,1.3 to 0.5,1.3.
 *      apr 28 - Added Nigh's 2002 Py.
 *             - Added Nigh's 2004 Pl/Sw/Se total age curves.
 *      jun 15 - Substituted Nigh's total age curves for the 0-1.3m area of
 *               Pl Thrower, Sw Goudie Nat AC,
 *               Sw Goudie Pla AC, and Se Chen AC.
 *      sep 14 - Fixed age check in Sw Thrower (was bha>=0, change to bha>0.5).
 *             - Fixed age check in Ba Di Lucca, Fdi VDP Mont & Wash,
 *               Py Hann, and Py Hann AC (was bha>=0, change to bha>0).
 * 2005 feb 18 - Fix to Fdc Bruce and Bruce AC, using total age if that was
 *               passed in, rather than using the converted breast-height age.
 *             - Fixed age check (was bha>=0, change to bha>0)
 *               in Ba/Bl/Bg Kurucz82, Fdi/Py/Lw/La/Lt/Pli Milner,
 *               Hwc/Hm Wiley, Sb/Sw/Bb Ker, Cwc/Yc Kurucz.
 *      aug 5  - Changed At Nigh age-height curve below 1.3m.
 *      oct 20 - Added Huang Pj.
 * 2008 feb 28 - Added 2004 Sw Nigh GI.
 * 2009 may 6  - Forced pure y2bh to be computed for Fdc-Bruce.
 *      aug 28 - Added Nigh's 2009 Ep.
 * 2010 mar 4  - Added 2009 Ba Nigh GI.
 *             - Added 2009 Ba Nigh.
 *      apr 14 - Added 2010 Sw Hu and Garcia.
 * 2015 may 13 - Added 2015 Se Nigh.
 * 2016 mar 9  - Added parameter for proportion of growth in year before 1.3m.
 *             - Adjusted default equations for Pli, Sw, Fdc, Hwc
 *               to incorporate height smoothing near 1.3m.
 * 2017 feb 2  - Added Nigh's 2016 Cwc equation.
 * 2023 jul 7  - Translated like for like from C to Java
 *             - Rename si2ht to SiteIndex2Height
 */
/* @formatter:off */
    //Taken from sindex.h
    /*
    * age types
    */
    private static final short SI_AT_TOTAL   = 0;
    private static final short SI_AT_BREAST  = 1;

    /*
    * site index estimation (from height and age) types
    */
    private static final int SI_EST_DIRECT  = 1;

    /*
    * error codes as return values from functions
    */
    private static final int SI_ERR_NO_ANS    = -4;

    /* define species and equation indices */
    private static final int SI_ACB_HUANGAC        = 97;
    private static final int SI_ACB_HUANG          = 0;
    private static final int SI_ACT_THROWERAC      = 103;
    private static final int SI_ACT_THROWER        = 1;
    private static final int SI_AT_CHEN            = 74;
    private static final int SI_AT_CIESZEWSKI      =  3;
    private static final int SI_AT_GOUDIE          =  4;
    private static final int SI_AT_HUANG           =  2;
    private static final int SI_AT_NIGH            = 92;
    private static final int SI_BA_DILUCCA         =   5;
    private static final int SI_BA_KURUCZ82AC      = 102;
    private static final int SI_BA_KURUCZ82        =  8;
    private static final int SI_BA_KURUCZ86        =  7;
    private static final int SI_BA_NIGHGI          = 117;
    private static final int SI_BA_NIGH            = 118;
    private static final int SI_BL_CHENAC          = 93;
    private static final int SI_BL_CHEN            = 73;
    private static final int SI_BL_KURUCZ82        = 10;
    private static final int SI_BL_THROWERGI       =  9;
    private static final int SI_BP_CURTISAC        = 94;
    private static final int SI_BP_CURTIS          = 78;
    private static final int SI_CWC_BARKER         = 12;
    private static final int SI_CWC_KURUCZAC       = 101;
    private static final int SI_CWC_KURUCZ         = 11;
    private static final int SI_CWC_NIGH           = 122;
    private static final int SI_CWI_NIGH           = 77;
    private static final int SI_CWI_NIGHGI         = 84;
    private static final int SI_DR_HARRING         = 14;
    private static final int SI_DR_NIGH            = 13;
    private static final int SI_EP_NIGH             = 116;
    private static final int SI_FDC_BRUCEAC         = 100;
    private static final int SI_FDC_BRUCE          = 16;
    private static final int SI_FDC_BRUCENIGH      = 89;
    private static final int SI_FDC_COCHRAN        = 17;
    private static final int SI_FDC_KING           = 18;
    private static final int SI_FDC_NIGHGI         = 15;
    private static final int SI_FDC_NIGHTA         = 88;
    private static final int SI_FDI_HUANG_NAT      = 21;
    private static final int SI_FDI_HUANG_PLA      = 20;
    private static final int SI_FDI_MILNER         = 22;
    private static final int SI_FDI_MONS_DF        = 26;
    private static final int SI_FDI_MONS_GF        = 27;
    private static final int SI_FDI_MONS_SAF       = 30;
    private static final int SI_FDI_MONS_WH        = 29;
    private static final int SI_FDI_MONS_WRC       = 28;
    private static final int SI_FDI_NIGHGI         = 19;
    private static final int SI_FDI_THROWERAC      = 96;
    private static final int SI_FDI_THROWER        = 23;
    private static final int SI_FDI_VDP_MONT       = 24;
    private static final int SI_FDI_VDP_WASH       = 25;
    private static final int SI_HM_MEANSAC         = 95;
    private static final int SI_HM_MEANS           = 86;
    private static final int SI_HWC_BARKER         = 33;
    private static final int SI_HWC_FARR           = 32;
    private static final int SI_HWC_NIGHGI         = 31;
    private static final int SI_HWC_NIGHGI99       = 79;
    private static final int SI_HWC_WILEYAC        = 99;
    private static final int SI_HWC_WILEY          = 34;
    private static final int SI_HWC_WILEY_BC       = 35;
    private static final int SI_HWC_WILEY_MB       = 36;
    private static final int SI_HWI_NIGH           = 37;
    private static final int SI_HWI_NIGHGI         = 38;
    private static final int SI_LW_MILNER          = 39;
    private static final int SI_LW_NIGH            = 90;
    private static final int SI_LW_NIGHGI          = 82;
    private static final int SI_PJ_HUANG           = 113;
    private static final int SI_PJ_HUANGAC         = 114;
    private static final int SI_PLI_CIESZEWSKI     = 47;
    private static final int SI_PLI_DEMPSTER       = 50;
    private static final int SI_PLI_GOUDIE_DRY     = 48;
    private static final int SI_PLI_GOUDIE_WET     = 49;
    private static final int SI_PLI_HUANG_NAT      = 44;
    private static final int SI_PLI_HUANG_PLA      = 43;
    private static final int SI_PLI_MILNER         = 46;
    private static final int SI_PLI_NIGHGI97       = 42;
    private static final int SI_PLI_NIGHTA98       = 41;
    private static final int SI_PLI_THROWER        = 45;
    private static final int SI_PLI_THROWNIGH      = 40;
    private static final int SI_PL_CHEN            = 76;
    private static final int SI_PW_CURTISAC        = 98;
    private static final int SI_PW_CURTIS          = 51;
    private static final int SI_PY_HANNAC          = 104;
    private static final int SI_PY_HANN            = 53;
    private static final int SI_PY_MILNER          = 52;
    private static final int SI_PY_NIGH            = 107;
    private static final int SI_PY_NIGHGI          = 108;
    private static final int SI_SB_CIESZEWSKI      = 55;
    private static final int SI_SB_DEMPSTER        = 57;
    private static final int SI_SB_HUANG           = 54;
    private static final int SI_SB_KER             = 56;
    private static final int SI_SB_NIGH            = 91;
    private static final int SI_SE_CHENAC          = 105;
    private static final int SI_SE_CHEN            = 87;
    private static final int SI_SE_NIGHGI          = 120;
    private static final int SI_SE_NIGH            = 121;
    private static final int SI_SS_BARKER          = 62;
    private static final int SI_SS_FARR            = 61;
    private static final int SI_SS_GOUDIE          = 60;
    private static final int SI_SS_NIGH            = 59;
    private static final int SI_SS_NIGHGI          = 58;
    private static final int SI_SS_NIGHGI99        = 80;
    private static final int SI_SW_CIESZEWSKI      = 67;
    private static final int SI_SW_DEMPSTER        = 72;
    private static final int SI_SW_GOUDIE_NAT      = 71;
    private static final int SI_SW_GOUDIE_NATAC    = 106;
    private static final int SI_SW_GOUDIE_PLA      = 70;
    private static final int SI_SW_GOUDIE_PLAAC    = 112;
    private static final int SI_SW_GOUDNIGH        = 85;
    private static final int SI_SW_HU_GARCIA       = 119;
    private static final int SI_SW_HUANG_NAT       = 65;
    private static final int SI_SW_HUANG_PLA       = 64;
    private static final int SI_SW_KER_NAT         = 69;
    private static final int SI_SW_KER_PLA         = 68;
    private static final int SI_SW_NIGHGI          = 63;
    private static final int SI_SW_NIGHGI99        = 81;
    private static final int SI_SW_NIGHGI2004      = 115;
    private static final int SI_SW_NIGHTA          = 83;
    private static final int SI_SW_THROWER         = 66;

    /* not used, but must be defined for array positioning */
    private static final int SI_BB_KER              = 6;
    private static final int SI_DR_CHEN             = 75;
    private static final int SI_PLI_NIGHTA2004      = 109;
    private static final int SI_SE_NIGHTA           = 110;
    private static final int SI_SW_NIGHTA2004       = 111;

    public static double ppow(double x, double y) {
        return (x <= 0) ? 0.0 : Math.pow(x, y);
    }

    public static double llog(double x) {
        return ( (x) <= 0.0) ? Math.log(.00001) : Math.log(x);
    }


public static double index_to_height (
  short  cu_index,
  double iage,
  short  age_type,
  double site_index,
  double y2bh,
  double pi)      // proportion of height growth between breast height
                  // ages 0 and 1 that occurs below breast height
  {
  double height;  // return value
  double x1, x2, x3, x4, x5;  // equation coefficients
  double tage;    // total age
  double bhage;   // breast-height age

  if (site_index < 1.3){
    throw new LessThan13Exception("Site index < 1.3m: " + site_index);
  }

  // should this line be removed?
  y2bh = ((int) y2bh) + 0.5;

  if (age_type == SI_AT_TOTAL){
    tage = iage;
    bhage = Age2Age.age_to_age(cu_index, tage, SI_AT_TOTAL, SI_AT_BREAST, y2bh);
    }
  else{
    bhage = iage;
    tage = Age2Age.age_to_age(cu_index, bhage, SI_AT_BREAST, SI_AT_TOTAL, y2bh);
    }
  if (tage < 0.0){
    throw new NoAnswerException("Iteration could not converge (projected height > 999), age: " + tage);
  }
  if (tage < 0.00001){
    return 0.0;
  }

  switch (cu_index){
    case SI_FDC_COCHRAN:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = Math.log(bhage);
        x1 = Math.exp (-0.37496 + 1.36164 * x1 - 0.00243434 * ppow(x1, 4));
        x2 = -0.2828 + 1.87947 * ppow(1 - Math.exp (-0.022399 * bhage), 0.966998);

        height = 4.5 + x1 - x2 * (79.97 - (site_index - 4.5));

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
      break;
    case SI_FDC_KING:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 2500 / (site_index - 4.5);

        x2 = -0.954038    + 0.109757    * x1;
        x3 =  0.0558178   + 0.00792236  * x1;
        x4 = -0.000733819 + 0.000197693 * x1;

        height = 4.5 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

        if (bhage < 5){
          height += (0.22 * bhage);
        }

        if (bhage >= 5 && bhage < 10){
          height += (2.2 - 0.22 * bhage);
        }

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
      break;
    case SI_HWC_FARR:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = Math.log(bhage);

        x2 = 0.3621734 +
             1.149181 * x1 -
             0.005617852 * ppow(x1, 3.0) -
             7.267547E-6 * ppow(x1, 7.0) +
             1.708195E-16 * ppow(x1, 22.0) -
             2.482794E-22 * ppow(x1, 30.0);

        x3 = -2.146617 -
             0.109007 * x1 +
             0.0994030 * ppow(x1, 3.0) -
             0.003853396 * ppow(x1, 5.0) +
             1.193933E-8 * ppow(x1, 12.0) -
             9.486544E-20 * ppow(x1, 27.0) +
             1.431925E-26 * ppow(x1, 36.0);

        height = 4.5 + Math.exp(x2) - Math.exp(x3) * (83.20 - (site_index - 4.5));

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_HWC_BARKER:{
      double si50t;

      /*
       * convert from SI 50b to SI 50t
       */
      si50t = -10.45 + 1.30049 * site_index - 0.0022 * site_index * site_index;

      height = Math.exp(4.35753) * ppow(si50t / Math.exp(4.35753), ppow(50.0 / tage, 0.756313));
      }
      break;
    case SI_HM_MEANS:
      if (bhage > 0.0){
        /* convert to base 100 */
        site_index = -1.73 + 3.149 * ppow(site_index, 0.8279);

        height = 1.37 + (22.87 + 0.9502 * (site_index - 1.37)) *
          ppow(1 - Math.exp(-0.0020647 * ppow(site_index - 1.37, 0.5) * bhage),
          1.3656 + 2.046 / (site_index - 1.37));
        }
      else
        height = tage * tage * 1.37 / y2bh / y2bh;
      break;
    case SI_HM_MEANSAC:
      if (bhage > 0.5){
        /* convert to base 100 */
        site_index = -1.73 + 3.149 * ppow(site_index, 0.8279);

        height = 1.37 + (22.87 + 0.9502 * (site_index - 1.37)) *
          ppow(1 - Math.exp (-0.0020647 * ppow(site_index - 1.37, 0.5) * (bhage-0.5)),
          1.3656 + 2.046 / (site_index - 1.37));
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    //Couldn't find the constant
    /* case SI_HM_WILEY:
       if (bhage > 0.0){
        if (site_index > 60 + 1.667 * bhage){
          // function starts going nuts at high sites and low ages
          // evaluate at a safe age, and interpolate
          x1 = (site_index - 60) / 1.667 + 0.1;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.37 + (x2-1.37) * bhage / x1;
          break;
          }

        // convert to imperial
        site_index /= 0.3048;

        x1 = 2500 / (site_index - 4.5);

        x2 = -1.7307 + 0.1394 * x1;
        x3 = -0.0616 + 0.0137 * x1;
        x4 = 0.00192428 + 0.00007024 * x1;

        height = 4.5 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

        if (bhage < 5){
          height += (0.3 * bhage);
        }
        else if (bhage < 10){
          height += (3.0 - 0.3 * bhage);
        }

        // convert back to metric
        height *= 0.3048;

    }else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
      break;
      */

    case SI_HWC_WILEY:
        if (bhage > 0.0){
            if (site_index > 60 + 1.667 * bhage){
            // function starts going nuts at high sites and low ages
            // evaluate at a safe age, and interpolate
            x1 = (site_index - 60) / 1.667 + 0.1;
            x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
            height = 1.37 + (x2-1.37) * bhage / x1;
            break;
            }

            // convert to imperial
            site_index /= 0.3048;

            x1 = 2500 / (site_index - 4.5);

            x2 = -1.7307 + 0.1394 * x1;
            x3 = -0.0616 + 0.0137 * x1;
            x4 = 0.00192428 + 0.00007024 * x1;

            height = 4.5 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

            if (bhage < 5){
            height += (0.3 * bhage);
            }
            else if (bhage < 10){
            height += (3.0 - 0.3 * bhage);
            }

            // convert back to metric
            height *= 0.3048;
        } else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
      break;
    case SI_HWC_WILEY_BC:
        if (bhage > 0.0){
            if (site_index > 60 + 1.667 * bhage){
            // function starts going nuts at high sites and low ages
            // evaluate at a safe age, and interpolate
            x1 = (site_index - 60) / 1.667 + 0.1;
            x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
            height = 1.37 + (x2-1.37) * bhage / x1;
            break;
            }

            // convert to imperial
            site_index /= 0.3048;

            x1 = 2500 / (site_index - 4.5);

            x2 = -1.7307 + 0.1394 * x1;
            x3 = -0.0616 + 0.0137 * x1;
            x4 = 0.00192428 + 0.00007024 * x1;

            height = 4.5 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

            if (bhage < 5){
            height += (0.3 * bhage);
            }
            else if (bhage < 10){
            height += (3.0 - 0.3 * bhage);
            }

            // convert back to metric
            height *= 0.3048;

            if (cu_index == SI_HWC_WILEY_BC){
                x1 = -1.34105 + 0.0009 * bhage * height;
                if (x1 > 0.0){
                    height -= x1;
                }
            }
        } else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
      break;

    case SI_HWC_WILEY_MB:
        if (bhage > 0.0){
            if (site_index > 60 + 1.667 * bhage){
            // function starts going nuts at high sites and low ages
            // evaluate at a safe age, and interpolate
            x1 = (site_index - 60) / 1.667 + 0.1;
            x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
            height = 1.37 + (x2-1.37) * bhage / x1;
            break;
            }

            // convert to imperial
            site_index /= 0.3048;

            x1 = 2500 / (site_index - 4.5);

            x2 = -1.7307 + 0.1394 * x1;
            x3 = -0.0616 + 0.0137 * x1;
            x4 = 0.00192428 + 0.00007024 * x1;

            height = 4.5 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

            if (bhage < 5){
            height += (0.3 * bhage);
            }
            else if (bhage < 10){
            height += (3.0 - 0.3 * bhage);
            }

            // convert back to metric
            height *= 0.3048;

            if (cu_index == SI_HWC_WILEY_MB){
                x1 = 0.0972129 + 0.000419315 * bhage * height;
                height -= x1;
            }
        } else{
        height = tage * tage * 1.37 / y2bh / y2bh;
        }
        break;
    case SI_HWC_WILEYAC:
      if (bhage >= pi){
        if (site_index > 60 + 1.667 * (bhage-pi)){
          /* function starts going nuts at high sites and low ages */
          /* evaluate at a safe age, and interpolate */
          x1 = (site_index - 60) / 1.667 + 0.1 + pi;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.37 + (x2-1.37) * (bhage-pi) / x1;
          break;
        }

        /* convert to imperial */
        site_index /= 0.3048;

        x1 = Math.pow (49 + (1 - pi), 2.0) / (site_index - 4.5);

        x2 = -1.7307 + 0.1394 * x1;
        x3 = -0.0616 + 0.0137 * x1;
        x4 = 0.00195078 + 0.00007446 * x1;
        x5 = bhage - pi;
        height = 4.5 + x5 * x5 / (x2 + x3 * x5 + x4 * x5 * x5);

        if (x5 < 5){
          height += (0.3 * x5);
        }
        else if (x5 < 10){
          height += (3.0 - 0.3 * x5);
        }

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }

      break;
    case SI_BP_CURTIS:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = Math.log (site_index - 4.5) + 1.649871 * (Math.log (bhage) - Math.log (50))
           + 0.147245 * Math.pow (Math.log (bhage) - Math.log(50), 2.0);
        x2 = 1.0 + 0.164927 * (Math.log(bhage) - Math.log (50))
           + 0.052467 * Math.pow (Math.log (bhage) - Math.log (50), 2.0);
        height = 4.5 + Math.exp (x1 / x2);

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_BP_CURTISAC:
      if (bhage > 0.5){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = Math.log (site_index - 4.5) + 1.649871 * (Math.log (bhage-0.5) - Math.log (49.5))
           + 0.147245 * Math.pow (Math.log (bhage-0.5) - Math.log (49.5), 2.0);
        x2 = 1.0 + 0.164927 * (Math.log (bhage-0.5) - Math.log (49.5))
           + 0.052467 * Math.pow (Math.log (bhage-0.5) - Math.log (49.5), 2.0);
        height = 4.5 + Math.exp (x1 / x2);

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_SW_GOUDNIGH:
      if (site_index < 19.5){
        if (bhage > 0.5){
          /* Goudie */
          x1 = (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3) -1.4661 * Math.log (49.5))) /
               (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3) -1.4661 * Math.log (bhage-0.5)));

          height = 1.3 + (site_index - 1.3) * x1;
        }
        else{
          height = tage * tage * 1.3 / y2bh / y2bh;
        }
      }
      else{
        if (tage < y2bh - 0.5){
          /* use Nigh's total age curve */
          height = (-0.01666 + 0.001722 * site_index) * ppow(tage, 1.858) *
            ppow(0.9982, tage);
        }
        else if (tage > y2bh + 2 - 0.5){
          /* use Goudie's breast-height age curve */
          x1 = (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3)
                           - 1.4661 * Math.log (49.5))) /
               (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3)
                           - 1.4661 * Math.log (bhage-0.5)));

          height = 1.3 + (site_index - 1.3) * x1;
        }
        else{
          /* use Nigh's total age curve */
          x4 = (-0.01666 + 0.001722 * site_index) * ppow(y2bh-0.5, 1.858) *
            ppow(0.9982, y2bh-0.5);

          /* use Goudie's breast-height age curve */
          x1 = (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3)
                           - 1.4661 * Math.log (49.5))) /
               (1.0 + Math.exp (9.7936 -1.2866 * llog(site_index - 1.3)
                           - 1.4661 * Math.log (2-0.5)));

          x5 = 1.3 + (site_index - 1.3) * x1;

          height = x4 + (x5 - x4) * bhage / 2.0;
          }
        }
      break;
    case SI_PLI_THROWNIGH:
      if (site_index < 18.5){
        if (bhage > 0.5){
          x1 = (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (49.5))) /
               (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (bhage - 0.5)));

          height = 1.3 + (site_index - 1.3) * x1;
        }
        else{
          height = 1.3 * Math.pow (tage / y2bh, 1.8);
        }
      }
      else{
        if (tage < y2bh - 0.5){
          /* use Nigh's total age curve */
          height = (-0.03993 + 0.004828 * site_index) * ppow(tage, 1.902) *
            ppow(0.9645, tage);
        }
        else if (tage > y2bh + 2 - 0.5){
          /* use Thrower's breast-height age curve */
          x1 = (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (49.5))) /
               (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (bhage-0.5)));

          height = 1.3 + (site_index - 1.3) * x1;
        }
        else{
          /* use Nigh's total age curve */
          x4 = (-0.03993 + 0.004828 * site_index) * ppow(y2bh-0.5, 1.902) *
            ppow(0.9645, y2bh-0.5);

          /* use Thrower's breast-height age curve */
          x1 = (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (49.5))) /
               (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                           - 1.3563 * Math.log (2 - 0.5)));

          x5 = 1.3 + (site_index - 1.3) * x1;

          height = x4 + (x5 - x4) * bhage / 2.0;
          }
        }
      break;
    case SI_PLI_THROWER:
      if (bhage > pi){
        x1 = (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                         - 1.3563 * Math.log (50 - pi))) /
             (1.0 + Math.exp (7.6298 - 0.8940 * llog(site_index - 1.3)
                         - 1.3563 * Math.log (bhage - pi)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
/*
        height = tage * tage * 1.3 / y2bh / y2bh;
*/
        height = 1.3 * Math.pow (tage/y2bh, 1.77 - 0.1028 * y2bh) * Math.pow (1.179, tage - y2bh);
      }
      break;
    case SI_PLI_NIGHTA2004:
      if (tage <= 15){
        height = 1.3 * Math.pow (tage/y2bh, 1.77 - 0.1028 * y2bh) * Math.pow (1.179, tage - y2bh);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_PLI_NIGHTA98:
      if (tage <= 15){
        height = (-0.03993 + 0.004828 * site_index) * ppow(tage, 1.902) *
          ppow(0.9645, tage);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_SW_NIGHTA2004:
      if (tage <= 20){
        height = 1.3 * Math.pow (tage/y2bh, 1.628 - 0.05991 * y2bh) * Math.pow (1.127, tage - y2bh);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_SW_NIGHTA:
      if (tage <= 20 && site_index >= 14.2){
        height = (-0.01666 + 0.001722 * site_index) * ppow(tage, 1.858) *
          ppow(0.9982, tage);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_FDC_NIGHTA:
      if (tage <= 25){
        height = (-0.002355 + 0.0003156 * site_index) * ppow(tage, 2.861) *
          ppow(0.9337, tage);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_SE_NIGH:
      if (bhage > 0.5){
        // -1.71635 = 1.758 * log (1 - exp (-0.00955 * 49.5))
        // 45.3824 = -4 * 11.6209 * log (1 - exp (-0.00955 * 49.5))
        x1 = 0.5 * ((Math.log (site_index - 1.3) - 1.71635) + Math.sqrt (Math.pow (Math.log (site_index - 1.3) - 1.71635, 2.0) + 45.3824));
        height = 1.3 + Math.exp (x1) * Math.pow (1 - Math.exp (-0.00955 * (bhage-0.5)), -1.758 + 11.6209 / x1);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SE_NIGHTA:
      if (tage <= 20){
        height = 1.3 * Math.pow (tage/y2bh, 1.628 - 0.05991 * y2bh) * Math.pow (1.127, tage - y2bh);
      }
      else{
        height = SI_ERR_NO_ANS;
      }
      break;
    case SI_FDC_BRUCE:
      // 2009 may 6: force a non-rounded y2bh
      y2bh = 13.25 - site_index / 6.096;

      x1 = site_index / 30.48;

      x2 = -0.477762 + x1 * (-0.894427 + x1 * (0.793548 - x1 * 0.171666));

      x3 = ppow(50.0+y2bh, x2);

      x4 = Math.log (1.372 / site_index) / (ppow(y2bh, x2) - x3);

      if (age_type == SI_AT_TOTAL){
        height = site_index * Math.exp (x4 * (ppow(tage, x2) - x3));
      }
      else{
        height = site_index * Math.exp (x4 * (ppow(bhage+y2bh, x2) - x3));
      }
      break;
    case SI_FDC_BRUCEAC:
      // 2009 may 6: force a non-rounded y2bh
      y2bh = 13.25 - site_index / 6.096;

      x1 = site_index / 30.48;

      x2 = -0.477762 + x1 * (-0.894427 + x1 * (0.793548 - x1 * 0.171666));

      x3 = ppow(49 + (1 - pi) + y2bh, x2);

      x4 = Math.log (1.372 / site_index) / (ppow(y2bh, x2) - x3);

      if (age_type == SI_AT_TOTAL){
        height = site_index * Math.exp (x4 * (ppow(tage, x2) - x3));
      }
      else{
        height = site_index * Math.exp (x4 * (ppow(bhage + y2bh - pi, x2) - x3));
      }
      break;
    case SI_FDC_BRUCENIGH:
      // 2009 may 6: force a non-rounded y2bh
      y2bh = 13.25 - site_index / 6.096;

      if (tage < 50){
        /* compute Bruce at age 50 */
        x1 = site_index / 30.48;
        x2 = -0.477762 + x1 * (-0.894427 + x1 * (0.793548 - x1 * 0.171666));
        x3 = ppow(50.0+y2bh-0.5, x2);
        x4 = Math.log (1.372 / site_index) / (ppow(y2bh-0.5, x2) - x3);
        height = site_index * Math.exp (x4 * (ppow(50, x2) - x3));

        /* now smooth it into the Nigh curve */
        x4 = ppow(height * ppow(50, -2.037) / (-0.0123  + 0.00158 * site_index), 1.0 / 50);

        height = (-0.0123  + 0.00158 * site_index) * ppow(tage, 2.037) * ppow(x4, tage);
      }
      else{
        /* compute Bruce */
        x1 = site_index / 30.48;
        x2 = -0.477762 + x1 * (-0.894427 + x1 * (0.793548 - x1 * 0.171666));
        x3 = ppow(50.0+y2bh-0.5, x2);
        x4 = Math.log (1.372 / site_index) / (ppow(y2bh-0.5, x2) - x3);
        height = site_index * Math.exp (x4 * (ppow(tage, x2) - x3));
      }
      break;
    case SI_PLI_MILNER:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 96.93 * ppow(1 - Math.exp (-0.01955 * bhage), 1.216);
        x2 = 1.41  * ppow(1 - Math.exp (-0.02656 * bhage), 1.297);
        height = 4.5 + x1 + x2 * (site_index - 59.6);

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_PLI_CIESZEWSKI:
      if (bhage > 0.0){
        x1 = 0.20372424;
        x2 = 97.37473618;
        x3 = 20 * x2 / (ppow(50.0, 1+x1));
        x4 = site_index-1.3 +
          Math.sqrt ((site_index-1.3 - x3)*(site_index-1.3 - x3) +
          80*x2*(site_index-1.3) * ppow(50.0, -(1+x1)));

        height = 1.3 + (x4 + x3) /
          (2 + 80*x2*ppow(bhage, -(1+x1)) / (x4 - x3));
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_CIESZEWSKI:
      if (bhage > 0.0){
        x1 = 0.3235139;
        x2 = 260.9162652;
        x3 = 20 * x2 / (ppow(50.0, 1+x1));
        x4 = site_index-1.3 +
          Math.sqrt ((site_index-1.3 - x3)*(site_index-1.3 - x3) +
          80*x2*(site_index-1.3) * ppow(50.0, -(1+x1)));

        height = 1.3 + (x4 + x3) /
          (2 + 80*x2*ppow(bhage, -(1+x1)) / (x4 - x3));
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SB_CIESZEWSKI:
      if (bhage > 0.0){
        x1 = 0.1992266;
        x2 = 114.8730018;
        x3 = 20 * x2 / (ppow(50.0, 1+x1));
        x4 = site_index-1.3 +
          Math.sqrt ((site_index-1.3 - x3)*(site_index-1.3 - x3) +
          80*x2*(site_index-1.3) * ppow(50.0, -(1+x1)));

        height = 1.3 + (x4 + x3) /
          (2 + 80*x2*ppow(bhage, -(1+x1)) / (x4 - x3));
      }else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_AT_CIESZEWSKI:
      if (bhage > 0.0){
        x1 = 0.2644606;
        x2 = 117.3695371;
        x3 = 20 * x2 / (ppow(50.0, 1+x1));
        x4 = site_index-1.3 +
          Math.sqrt ((site_index-1.3 - x3)*(site_index-1.3 - x3) +
          80*x2*(site_index-1.3) * ppow(50.0, -(1+x1)));

        height = 1.3 + (x4 + x3) /
          (2 + 80*x2*ppow(bhage, -(1+x1)) / (x4 - x3));
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Couldn't find constant
    /*case SI_PF_GOUDIE_WET:
      if (bhage > 0.0){
        x1 = -0.935;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    // Couldn't find constant
    /*
    case SI_PF_GOUDIE_DRY:
      if (bhage > 0.0){
        x1 = -1.00726;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    case SI_PLI_GOUDIE_WET:
      if (bhage > 0.0){
        x1 = -0.935;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_PLI_GOUDIE_DRY:
      if (bhage > 0.0){
        x1 = -1.00726;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    //Couldn't find constant
    /*case SI_PA_GOUDIE_WET:
      if (bhage > 0.0){
        x1 = -0.935;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    // Couldn't find constant
    /*
    case SI_PA_GOUDIE_DRY:
      if (bhage > 0.0){
        x1 = -1.00726;
        x2 = 7.81498;
        x3 = -1.28517;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    */
    case SI_PLI_DEMPSTER:
      if (bhage > 0.0){
        x1 = -0.9576;
        x2 = 7.4871;
        x3 = -1.2036;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Couldn't find constant
    /*case SI_SE_GOUDIE_PLA:
      if (bhage > 0.0){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    // Couldn't find constant
    /* case SI_SE_GOUDIE_NAT:
      if (bhage > 0.0){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    case SI_SW_GOUDIE_PLA:
      if (bhage > 0.0){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_GOUDIE_NAT:
       if (bhage > 0.0){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_DEMPSTER:
      if (bhage > 0.0){
        x1 = -1.2240;
        x2 = 9.6183;
        x3 = -1.4627;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SB_DEMPSTER:
       if (bhage > 0.0){
          x1 = -1.3154;
          x2 = 8.5594;
          x3 = -1.1484;

          x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SS_GOUDIE:
      if (bhage > 0.0){
        x1 = -1.5282;
        x2 = 11.0605;
        x3 = -1.5108;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_FDI_THROWER:
      if (bhage > 0.0){
        x1 = -0.237724692;
        x2 = 5.780089777;
        x3 = -1.150039266;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_AT_GOUDIE:
      if (bhage > 0.0){
        x1 = -0.618;
        x2 = 6.879;
        x3 = -1.32;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    //Couldn't find constant
    /*
    case SI_EP_GOUDIE:
      if (bhage > 0.0){
        x1 = -0.618;
        x2 = 6.879;
        x3 = -1.32;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    // Couldn't find constant
    /*case SI_EA_GOUDIE:
      if (bhage > 0.0){
            x1 = -0.618;
            x2 = 6.879;
            x3 = -1.32;

            x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    case SI_SW_GOUDIE_NATAC:
      if (bhage > pi){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log(50 - pi))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log(bhage - pi)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = 1.3 * Math.pow (tage/y2bh, 1.628 - 0.05991 * y2bh) * Math.pow (1.127, tage - y2bh);
      }
      break;

    case SI_SW_GOUDIE_PLAAC:
      if (bhage > pi){
        x1 = -1.2866;
        x2 = 9.7936;
        x3 = -1.4661;

        x1 = (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (50 - pi))) /
             (1.0 + Math.exp (x2 + x1 * llog(site_index - 1.3) + x3 * Math.log (bhage - pi)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = 1.3 * Math.pow (tage/y2bh, 1.628 - 0.05991 * y2bh) * Math.pow (1.127, tage - y2bh);
      }
      break;
    case SI_FDI_THROWERAC:
      if (bhage > 0.5){
        x1 = -0.237724692;
        x2 = 5.780089777;
        x3 = -1.150039266;
        x1 = (1.0 + Math.exp(x2 + x1 * llog(site_index - 1.3) + x3 * Math.log(49.5))) /
             (1.0 + Math.exp(x2 + x1 * llog(site_index - 1.3) + x3 * Math.log(bhage-0.5)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SS_NIGH:
      if (bhage > 0.5){
        x1 = 8.947;
        x2 = -1.357;
        x3 = -1.013;

        x1 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5)      + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5) + x3 * llog(site_index - 1.3)));
        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_BA_NIGH:
      if (bhage > 0.5){
        x5 = Math.pow (site_index - 1.3, 3.0) / 49.5;
        x4 = x5 + Math.pow (x5 * x5 + 16692000.0 * Math.pow (site_index - 1.3, 3.0) / 299891.0, 0.5);
        x2 = (8346000.0 + x4 * 6058.412) * Math.pow (bhage-0.5, 3.232);
        x3 = (8346000.0 + x4 * Math.pow (bhage-0.5, 2.232)) * 299891.0;
        height = 1.3 + (site_index - 1.3) * Math.pow (x2 / x3, 1/3.0);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_EP_NIGH:
      if (bhage > 0.5){
        x1 = 9.604;
        x2 = -1.113;
        x3 = -1.849;

        x1 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5)      + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5) + x3 * llog(site_index - 1.3)));
        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_CWI_NIGH:
      if (bhage > 0.5){
        x1 = 9.474;
        x2 = -1.340;
        x3 = -1.244;

        x1 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5)      + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp(x1 + x2 * Math.log (bhage-0.5) + x3 * llog(site_index - 1.3)));
        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_HWI_NIGH:
      if (bhage > 0.5){
        x1 = 8.998;
        x2 = -1.434;
        x3 = -1.051;

        x1 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5)      + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5) + x3 * llog(site_index - 1.3)));
        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_PY_NIGH:
      if (bhage > 0.5){
        x1 = 8.519;
        x2 = -1.385;
        x3 = -0.8498;

        x1 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5)      + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5) + x3 * llog(site_index - 1.3)));
        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = (1.3 * Math.pow (tage, 1.137) * Math.pow (1.016, tage)) /
                       (Math.pow (y2bh, 1.137) * Math.pow (1.016, y2bh));
      }
      break;
    case SI_ACT_THROWER:
    // case SI_MB_THROWER: Cannot find constant
      if (bhage > 0.0){
        x1 = -1.3481;
        x2 = 10.3861;
        x3 = -1.6555;

        x1 = (1.0 + Math.exp (x2 + x3 * llog(site_index - 1.3) + x1 * Math.log (50.0))) /
             (1.0 + Math.exp (x2 + x3 * llog(site_index - 1.3) + x1 * Math.log (bhage)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_ACT_THROWERAC:
      if (bhage > 0.5){
        x1 = -1.3481;
        x2 = 10.3861;
        x3 = -1.6555;

        x1 = (1.0 + Math.exp (x2 + x3 * llog(site_index - 1.3) + x1 * Math.log (49.5))) /
             (1.0 + Math.exp (x2 + x3 * Math.log (site_index - 1.3) + x1 * Math.log (bhage-0.5)));

        height = 1.3 + (site_index - 1.3) * x1;
        }
      else
        height = tage * tage * 1.3 / y2bh / y2bh;
      break;
    case SI_SB_KER:
      if (bhage > 0.0){
        x2 = 0.01741;
        x3 = 8.7428;
        x4 = -0.7346;
        x1 = ppow(1 - Math.exp (-x2 * bhage), x3 * ppow(site_index, x4));
        x2 = ppow(1 - Math.exp (-x2 *  50),   x3 * ppow(site_index, x4));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_KER_PLA:
    case SI_SW_KER_NAT:
      if (bhage > 0.0){
        x2 = 0.02081;
        x3 = 11.1515;
        x4 = -0.7518;
        x1 = ppow(1 - Math.exp (-x2 * bhage), x3 * ppow(site_index, x4));
        x2 = ppow(1 - Math.exp (-x2 *    50), x3 * ppow(site_index, x4));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_THROWER:
      if (bhage > 0.5){
        x1 = (1.0 + Math.exp (10.1654 - 1.4002 * llog(site_index - 1.3)
                         - 1.4482 * Math.log (50.0 - 0.5))) /
             (1.0 + Math.exp (10.1654 - 1.4002 * llog(site_index - 1.3)
                         - 1.4482 * Math.log (bhage - 0.5)));

        height = 1.3 + (site_index - 1.3) * x1;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SW_HU_GARCIA:
      if (bhage > 0.5){
        double q;

        q = hu_garcia_q (site_index, 50.0);
        height = hu_garcia_h (q, bhage);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SS_FARR:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x3 = llog(bhage);

        x1 = -0.20505 +
             1.449615 * x3 -
             0.01780992 * ppow(x3, 3.0) +
             6.519748E-5 * ppow(x3, 5.0) -
             1.095593E-23 * ppow(x3, 30.0);

        x2 = -5.61188 +
             2.418604 * x3 -
             0.259311 * ppow(x3, 2.0) +
             1.351445E-4 * ppow(x3, 5.0) -
             1.701139E-12 * ppow(x3, 16.0) +
             7.964197E-27 * ppow(x3, 36.0);

        height = 4.5 + Math.exp (x1) - Math.exp (x2) * (86.43 - (site_index - 4.5));

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
     break;
    case SI_PW_CURTIS:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 1.0 - Math.exp (-Math.exp (-9.975053 + (1.747353 - 0.38583) * Math.log (bhage) +
          1.119438 * Math.log (site_index)));

        x2 = 1.0 - Math.exp (-Math.exp (-9.975053 + 1.747353 * Math.log (50.0) -
          0.38583 * Math.log (bhage) + 1.119438 * Math.log (site_index)));

        height = 4.5 + (site_index - 4.5) * x1 / x2;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
     break;
    case SI_PW_CURTISAC:
      if (bhage > 0.5){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 1.0 - Math.exp (-Math.exp (-9.975053 + (1.747353 - 0.38583) * Math.log (bhage-0.5) +
          1.119438 * Math.log (site_index)));

        x2 = 1.0 - Math.exp (-Math.exp (-9.975053 + 1.747353 * Math.log (49.5) -
          0.38583 * Math.log (bhage-0.5) + 1.119438 * Math.log (site_index)));

        height = 4.5 + (site_index - 4.5) * x1 / x2;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
     break;
    case SI_SS_BARKER:
    {
      double si50t;

      /*
       * convert from SI 50b to SI 50t
       */
      si50t = -10.59 + 1.24 * site_index - 0.001 * site_index * site_index;

      height = Math.exp (4.39751) * ppow(si50t / Math.exp (4.39751), ppow(50.0 / tage, 0.792329));
    }
      break;
    case SI_CWC_BARKER:
    {
      double si50t;

      /*
       * convert from SI 50b to SI 50t
       */
      si50t = -5.85 + 1.12 * site_index;

      height = Math.exp (4.56128) * ppow(si50t / Math.exp (4.56128), ppow(50.0 / tage, 0.584627));
    }
      break;
    case SI_CWC_KURUCZ:
    //case SI_YC_KURUCZ: Cannot find constant
      if (bhage > 0.0){
        if (site_index > 43 + 1.667 * bhage){
          /* function starts going nuts at high sites and low ages */
          /* evaluate at a safe age, and interpolate */
          x1 = (site_index - 43) / 1.667 + 0.1;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.3 + (x2-1.3) * bhage / x1;
          break;
        }

        if (site_index <= 1.3){
          x1 = 99999.0;
        }
        else{
          x1 = 2500.0 / (site_index - 1.3);
        }

        x2 = -3.11785 + 0.05027     * x1;
        x3 = -0.02465 + 0.01411     * x1;
        x4 =  0.00174 + 0.000097667 * x1;

        height = 1.3 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

        if (bhage > 50.0){
          if (bhage > 200){
            /*
             * The "standard" correction applied above 50 years would
             * overpower the uncorrected curve at around 400 years.
             * So, after consultation with Robert Macdonald and Ian
             * Cameron, it was decided to use a correction beyond 200
             * years with the same ratio as at age 200.
             */
            bhage = 200;
          }
          height = height - (-0.02379545 * height +
            0.000475909 * bhage * height);
        }
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_CWC_KURUCZAC:
      if (bhage >= 0.5){
        if (site_index > 43 + 1.667 * (bhage-0.5)){
          /* function starts going nuts at high sites and low ages */
          /* evaluate at a safe age, and interpolate */
          x1 = (site_index - 43) / 1.667 + 0.1 + 0.5;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.3 + (x2-1.3) * (bhage-0.5) / x1;
          break;
        }

        if (site_index <= 1.3){
          x1 = 99999.0;
        }
        else{
          x1 = 2450.25 / (site_index - 1.3);
        }

        x2 = -3.11785 + 0.05027     * x1;
        x3 = -0.02465 + 0.01411     * x1;
        x4 =  0.00177044 + 0.000102554 * x1;
        x5 = bhage-0.5;
        height = 1.3 + x5 * x5 / (x2 + x3 * x5 + x4 * x5 * x5);

        if (bhage > 50.0){
          if (bhage > 200){
            /*
             * The "standard" correction applied above 50 years would
             * overpower the uncorrected curve at around 400 years.
             * So, after consultation with Robert Macdonald and Ian
             * Cameron, it was decided to use a correction beyond 200
             * years with the same ratio as at age 200.
             */
            bhage = 200;
          }
          height = height - (-0.02379545 * height +
            0.000475909 * bhage * height);
        }
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_CWC_NIGH:
      if (bhage > 0.5){
        x1 = -3.004284755 + 2.5332489439 * site_index - 0.019027688 * site_index * site_index + 0.0000992968 * Math.pow (site_index, 3.0);
        height = 1.3 + x1 * Math.pow (1 - Math.exp (-0.01449 * (bhage-0.5)), 1.4026 - 0.005781 * x1);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_BA_DILUCCA:
      if (bhage > 0.0){
        x1 = 1 + Math.exp (8.377148582 - 1.27351813 * Math.log (50.0) -
                      0.975226632 * Math.log (site_index));
        x2 = 1 + Math.exp (8.377148582 - 1.27351813 * Math.log (bhage) -
                      0.975226632 * Math.log (site_index));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_BB_KER:
      if (bhage > 0.0){
        x2 = 0.01373;
        x3 = 6.1299;
        x4 = -0.6157;
        x1 = ppow(1 - Math.exp (-x2 * bhage), x3 * ppow(site_index, x4));
        x2 = ppow(1 - Math.exp (-x2 *    50), x3 * ppow(site_index, x4));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_BA_KURUCZ86:
      if (bhage > 0.0){
        x1 = (site_index - 1.3) * ppow(1.0 - Math.exp (-0.01303 * bhage), 1.024971);

        height = 1.3 + x1 / 0.470011;

        if (bhage <= 50.0){
          height -= (4 * 0.4 * bhage * (50 - bhage) / 2500);
        }
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_BA_KURUCZ82:
    case SI_BL_KURUCZ82:
    //case SI_BG_KURUCZ82: Cannot find constants
      if (bhage > 0.0){
        if (site_index > 60 + 1.667 * bhage){
          /* function starts going nuts at high sites and low ages */
          /* evaluate at a safe age, and interpolate */
          x1 = (site_index - 60) / 1.667 + 0.1;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.3 + (x2-1.3) * bhage / x1;
          break;
        }

        if (site_index <= 1.3){
          x1 = 99999.0;
        }
        else{
          x1 = 2500.0 / (site_index - 1.3);
        }

        x2 = -2.34655 + 0.0565  * x1;
        x3 = -0.42007 + 0.01687 * x1;
        x4 =  0.00934 + 0.00004 * x1;

        height = 1.3 + bhage * bhage / (x2 + x3 * bhage + x4 * bhage * bhage);

        if (bhage < 50.0 && bhage * height < 1695.3){
          x1 = 0.45773 - 0.00027 * bhage * height;
          if (x1 > 0.0){
            height -= x1;
          }
        }
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;

        x1 = 0.45773 - 0.00027 * tage * height;   /* flaw? total vs bh-age */
        if (x1 > 0.0)
          height -= x1;
      }
      break;

      case SI_BA_KURUCZ82AC:
      if (bhage >= 0.5){
        if (site_index > 60 + 1.667 * (bhage-0.5)){
          /* function starts going nuts at high sites and low ages */
          /* evaluate at a safe age, and interpolate */
          x1 = (site_index - 60) / 1.667 + 0.1 + 0.5;
          x2 = index_to_height (cu_index, x1, SI_AT_BREAST, site_index, y2bh, pi);
          height = 1.3 + (x2-1.3) * (bhage-0.5) / x1;
          break;
        }

        if (site_index <= 1.3){
          x1 = 99999.0;
        }
        else{
          x1 = 2450.25 / (site_index - 1.3);
        }

        x2 = -2.09187 + 0.066925  * x1;
        x3 = -0.42007 + 0.01687 * x1;
        x4 =  0.00934 + 0.00004 * x1;
        x5 = bhage-0.5;
        height = 1.3 + x5 * x5 / (x2 + x3 * x5 + x4 * x5 * x5);

        if (bhage < 50.0 && bhage * height < 1695.3){
          x1 = 0.45773 - 0.00027 * bhage * height;
          if (x1 > 0.0){
            height -= x1;
          }
        }
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;

        x1 = 0.45773 - 0.00027 * tage * height;   /* flaw? total vs bh-age */
        if (x1 > 0.0){
          height -= x1;
        }
      }
      break;
    case SI_FDI_MILNER:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 114.6 * ppow(1 - Math.exp (-0.01462 * bhage), 1.179);
        x2 = 1.703 * ppow(1 - Math.exp (-0.02214 * bhage), 1.321);
        height = 4.5 + x1 + x2 * (site_index - 57.3);

        /* convert back to metric */
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_FDI_VDP_MONT:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        height = 4.5 + (1.9965 * (site_index - 4.5) /
                        (1 + Math.exp (5.479 - 1.4016 * Math.log (bhage))));

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_FDI_VDP_WASH:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        height = 4.5 + (1.79897 * (site_index - 4.5) /
                        (1 + Math.exp (6.0678 - 1.6085 * Math.log (bhage))));

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_FDI_MONS_DF:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;
        x1 = 0.3197;
        x2 = 1.0232;

        x3 = 1.0 + Math.exp (9.7278 - 1.2934 * Math.log (bhage) - x2 * llog(site_index-4.5));

        height = 4.5 + 42.397 * ppow(site_index-4.5, x1) / x3;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;

    case SI_FDI_MONS_GF:
    if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;
        x1 = 0.3488;
        x2 = 0.9779;

        x3 = 1.0 + Math.exp (9.7278 - 1.2934 * Math.log (bhage) - x2 * llog(site_index-4.5));

        height = 4.5 + 42.397 * ppow(site_index-4.5, x1) / x3;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;


    case SI_FDI_MONS_WRC:
        if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;
        x1 = 0.3488;
        x2 = 0.9779;

        x3 = 1.0 + Math.exp (9.7278 - 1.2934 * Math.log (bhage) - x2 * llog(site_index-4.5));

        height = 4.5 + 42.397 * ppow(site_index-4.5, x1) / x3;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;

    case SI_FDI_MONS_WH:
        if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;
        x1 = 0.3656;
        x2 = 0.9527;

        x3 = 1.0 + Math.exp (9.7278 - 1.2934 * Math.log (bhage) - x2 * llog(site_index-4.5));

        height = 4.5 + 42.397 * ppow(site_index-4.5, x1) / x3;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_FDI_MONS_SAF:
        if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;
        x1 = 0.3656;
        x2 = 0.9527;

        x3 = 1.0 + Math.exp (9.7278 - 1.2934 * Math.log (bhage) - x2 * llog(site_index-4.5));

        height = 4.5 + 42.397 * ppow(site_index-4.5, x1) / x3;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_DR_HARRING:
      if (site_index > 45 + 2.5 * tage){
        /* function starts going nuts at high sites and low ages */
        /* evaluate at a safe age, and interpolate */
        x1 = (site_index - 45) / 2.5 + 0.1;
        x2 = index_to_height (cu_index, x1, SI_AT_TOTAL, site_index, y2bh, pi);
        height = x2 * tage / x1;
      }
      else{
        double si20;

        si20 = ppow(site_index, 1.5) / 8.0;
        x1 = 18.1622 + 0.7953 * si20;
        x2 = 0.00194 - 0.002441 * si20;
        x3 = si20 + x1 * ppow(1.0 - Math.exp (x2 * tage), 0.9198);
        height = x3 - x1 * ppow(1.0 - Math.exp (x2 * 20), 0.9198);
      }
      break;
    case SI_DR_NIGH:
      if (bhage > 0.5){
        double si25;

        si25 = 0.3094 + 0.7616 * site_index;
        height = 1.3 + (1.693 * (si25 - 1.3)) /
          (1 + Math.exp (3.6 - 1.24 * Math.log (bhage - 0.5)));
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Cannot Find Constant
    /*   case SI_BG_COCHRAN:
      if (bhage > 0.0){
        // convert to imperial
        site_index /= 0.3048;

        x1 = Math.log (bhage);
        x2 = -0.30935 +
             1.2383 * x1 +
             0.001762 * ppow(x1, 4.0) -
             5.4e-6 * ppow(x1, 9.0) +
             2.046e-7 * ppow(x1, 11.0) -
             4.04e-13 * ppow(x1, 18.0);
        x3 = -6.2056 +
             2.097 * x1 -
             0.09411 * ppow(x1, 2) -
             4.382e-5 * ppow(x1, 7) +
             2.007e-11 * ppow(x1, 16) -
             2.054e-17 * ppow(x1, 24);
        height = 4.5 +
                 Math.exp (x2) -
                 84.93 * Math.exp (x3) +
                 (site_index - 4.5) * Math.exp (x3);

        // convert back to metric
        height *= 0.3048;
        }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
      */
    case SI_PY_MILNER:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 121.4 * ppow(1 - Math.exp (-0.01756 * bhage), 1.483);
        x2 = 1.189 * ppow(1 - Math.exp (-0.05799 * bhage), 2.63);
        height = 4.5 + x1 + x2 * (site_index - 59.6);

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_PY_HANN:
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 1 - Math.exp (-Math.exp (-6.54707 + 0.288169 * llog(site_index - 4.5) + 1.21297 * Math.log (bhage)));
        x2 = 1 - Math.exp (-Math.exp (-6.54707 + 0.288169 * llog(site_index - 4.5) + 1.21297 * Math.log (50.0)));
        height = 4.5 + (site_index - 4.5) * x1 / x2;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_PY_HANNAC:
      if (bhage > 0.5){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 1 - Math.exp (-Math.exp (-6.54707 + 0.288169 * llog(site_index - 4.5) + 1.21297 * Math.log (bhage-0.5)));
        x2 = 1 - Math.exp (-Math.exp (-6.54707 + 0.288169 * llog(site_index - 4.5) + 1.21297 * Math.log (49.5)));
        height = 4.5 + (site_index - 4.5) * x1 / x2;

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;

    //case SI_LT_MILNER: Couldn't find constant
    case SI_LW_MILNER:
    // case SI_LA_MILNER: Couldn't find constant
      if (bhage > 0.0){
        /* convert to imperial */
        site_index /= 0.3048;

        x1 = 127.8 * ppow(1 - Math.exp (-0.01655 * bhage), 1.196);
        x2 = 1.289 * ppow(1 - Math.exp (-0.03211 * bhage), 1.047);
        height = 4.5 + x1 + x2 * (site_index - 69.0);

        /* convert back to metric */
        height *= 0.3048;
      }
      else{
        height = tage * tage * 1.37 / y2bh / y2bh;
      }
      break;
    case SI_LW_NIGH:
      if (bhage > 0.5){
        x1 = Math.log (Math.pow (site_index - 1.3, 1 - 0.8566) / 3.027) / Math.log (1 - Math.exp (-0.01588 * 49.5));
        height = 1.3 + 3.027 * Math.pow (site_index - 1.3, 0.8566) *
          Math.pow (1 - Math.exp (-0.01588 * (bhage - 0.5)), x1);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_SB_NIGH:
      if (bhage > 0.5){
        x1 = 1 + Math.exp (9.086 - 1.052 * Math.log (     49.5) - 1.55 * Math.log (site_index - 1.3));
        x2 = 1 + Math.exp (9.086 - 1.052 * Math.log (bhage-0.5) - 1.55 * Math.log (site_index - 1.3));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_AT_NIGH:
      if (bhage > 0.5){
        x1 = 1 + Math.exp (7.423 - 1.15 * Math.log (     49.5) - 0.9614 * Math.log (site_index - 1.3));
        x2 = 1 + Math.exp (7.423 - 1.15 * Math.log (bhage-0.5) - 0.9614 * Math.log (site_index - 1.3));
        height = 1.3 + (site_index - 1.3) * x1 / x2;
      }
      else{
/* was
        height = tage * tage * 1.3 / y2bh / y2bh;
*/
        height = Math.pow (tage / y2bh, 1.5) * 1.3;
      }
      break;
    // Cannot find constant
    /* case SI_TE_GOUDIE:
      if (bhage > 0.0){
        x1 = (1-Math.exp(-0.0227 * bhage)) / (1-Math.exp(-0.0227 * 50));
        x2 = 6.525 * ppow(site_index, -0.7606);
        height = site_index * ppow(x1, x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    case SI_SW_HUANG_PLA:
      double x0;
      double age_huang; /* used in HUANG's equations */

      if (bhage > 0.0){
        x0 =  0.010168;
        x1 =  0.004801;
        x2 =  4.997735;
        x3 =  0.802776;
        x4 = -0.243297;
        x5 =  0.325438;
        age_huang = 50.0;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_SW_HUANG_NAT:
      if (bhage > 0.0){
        x0 =  0.010168;
        x1 =  0.004801;
        x2 =  4.997735;
        x3 =  0.802776;
        x4 = -0.243297;
        x5 =  0.325438;
        age_huang = 50.0;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_PLI_HUANG_PLA:
      if (bhage > 0.0){
          x0 =  0.026714;
          x1 = -0.314562;
          x2 =  1.033165;
          x3 =  0.799658;
          x4 = -0.439270;
          x5 =  0.401374;
          age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_PLI_HUANG_NAT:
      if (bhage > 0.0){
        x0 =  0.026714;
        x1 = -0.314562;
        x2 =  1.033165;
        x3 =  0.799658;
        x4 = -0.439270;
        x5 =  0.401374;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Cannot Find Constant
    /* case SI_PJ_HUANG_PLA:
      double x0;
      double age_huang; // used in HUANG's equations

      if (bhage > 0.0){
        x0 =  0.023405;
        x1 = -0.371557;
        x2 =  1.048011;
        x3 =  0.715449;
        x4 = -0.503105;
        x5 =  0.444505;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */
    // Cannot Find Constant
    /* case SI_PJ_HUANG_NAT:
      double x0;
      double age_huang; // used in HUANG's equations

      if (bhage > 0.0){
        x0 =  0.023405;
        x1 = -0.371557;
        x2 =  1.048011;
        x3 =  0.715449;
        x4 = -0.503105;
        x5 =  0.444505;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */


    case SI_FDI_HUANG_PLA:
      if (bhage > 0.0){
        x0 =  0.007932;
        x1 =  0.011994;
        x2 =  7.053999;
        x3 =  0.617157;
        x4 = -0.365916;
        x5 =  0.405321;
        age_huang = 50.0;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;


    case SI_FDI_HUANG_NAT:
      if (bhage > 0.0){
        x0 =  0.007932;
        x1 =  0.011994;
        x2 =  7.053999;
        x3 =  0.617157;
        x4 = -0.365916;
        x5 =  0.405321;
        age_huang = 50.0;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;


    case SI_AT_HUANG:
      if (bhage > 0.0){
        x0 =  0.035930;
        x1 = -0.486239;
        x2 =  1.041916;
        x3 =  0.818283;
        x4 = -0.594641;
        x5 =  0.522558;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_SB_HUANG:
      if (bhage > 0.0){
        x0 =  0.011117;
        x1 =  0.030221;
        x2 =  1.010399;
        x3 =  0.573793;
        x4 = -0.328092;
        x5 =  0.387445;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_ACB_HUANG:
      if (bhage > 0.0){
        x0 =  0.041208;
        x1 = -0.559626;
        x2 =  1.038923;
        x3 =  0.832609;
        x4 = -0.627227;
        x5 =  0.526901;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;

      }
      break;

    // Cannot Find Constant
    /* case SI_BB_HUANG:
      double x0;
      double age_huang; // used in HUANG's equations

      if (bhage > 0.0){
        x0 =  0.010190;
        x1 =  0.013957;
        x2 =  3.876735;
        x3 =  0.647527;
        x4 = -0.274343;
        x5 =  0.378078;
        age_huang = 50.0;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
              Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * bhage)) / (1 - Math.exp (x0 * 50.0));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (50.0, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */

    case SI_ACB_HUANGAC:{
      if (bhage > 0.5)
        {
        x0 =  0.041208;
        x1 = -0.559626;
        x2 =  1.038923;
        x3 =  0.832609;
        x4 = -0.627227;
        x5 =  0.526901;
        age_huang = 1;

        x0 = -x0 * ppow(site_index - 1.3, x1) *
          Math.pow (x2, (site_index - 1.3) / age_huang);
        x0 = (1.0 - Math.exp (x0 * (bhage-0.5))) / (1 - Math.exp (x0 * 49.5));
        x1 = ppow(site_index - 1.3, x4);
        x2 = Math.pow (49.5, x5);

        height = 1.3 + (site_index - 1.3) * ppow(x0, x3 * x1 * x2);
        }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      }
      break;
    case SI_BL_CHEN:
      if (bhage > 0.0){
        x1 =  9.523;
        x2 = -1.4945;
        x3 = -1.2159;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (50.0) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
        }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_SE_CHEN:
      if (bhage > 0.0){
        x1 =  8.6126;
        x2 = -1.5269;
        x3 = -0.7805;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (50.0) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;


    case SI_PL_CHEN:
      if (bhage > 0.0){
        x1 =  6.9603;
        x2 = -1.2875;
        x3 = -0.5904;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (50.0) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Couldn't Find Constant
    /*
    case SI_EP_CHEN:
      if (bhage > 0.0){
        x1 =  9.9045;
        x2 = -1.1736;
        x3 = -1.8361;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (50.0) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    */
    case SI_DR_CHEN:
      if (bhage > 0.0){
        x1 =  6.6133;
        x2 = -1.0807;
        x3 = -1.0176;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (50.0) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_BL_CHENAC:
      if (bhage > 0.5){
        x1 =  9.523;
        x2 = -1.4945;
        x3 = -1.2159;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_SE_CHENAC:
      if (bhage > 0.5){
        x1 =  8.6126;
        x2 = -1.5269;
        x3 = -0.7805;

        x4 = (1.0 + Math.exp (x1 + x2 * Math.log (49.5) + x3 * llog(site_index - 1.3))) /
             (1.0 + Math.exp (x1 + x2 * Math.log (bhage-0.5)+ x3 * llog(site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x4;
      }
      else{
/*
        height = tage * tage * 1.3 / y2bh / y2bh;
*/
        height = 1.3 * Math.pow (tage/y2bh, 1.628 - 0.05991 * y2bh) * Math.pow (1.127, tage - y2bh);
      }
      break;

    case SI_AT_CHEN:
      if (bhage > 0.0){
        x1 = llog(ppow(site_index - 1.3, -0.076) / 1.418) /
          llog(1 - Math.exp (-0.017 * 50));
        height = 1.3 + 1.418 * (ppow(site_index - 1.3, 1.076) *
          ppow(1 - Math.exp (-0.017 * bhage), x1));
        }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    case SI_PJ_HUANG:
      if (bhage > 0){
        x1 = 0.073456;
        x2 = 8.770517;
        x3 = -1.334706;
        x4 = 1.719841;

        x5 = (1.0 + x1 * (site_index - 1.3) + Math.exp (x2 + x3 * Math.log (   50+x4) - Math.log (site_index - 1.3))) /
             (1.0 + x1 * (site_index - 1.3) + Math.exp (x2 + x3 * Math.log (bhage+x4) - Math.log (site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x5;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;

    case SI_PJ_HUANGAC:
      if (bhage > 0.5){
        x1 = 0.073456;
        x2 = 8.770517;
        x3 = -1.334706;
        x4 = 1.719841;

        x5 = (1.0 + x1 * (site_index - 1.3) + Math.exp (x2 + x3 * Math.log (     49.5+x4) - Math.log (site_index - 1.3))) /
             (1.0 + x1 * (site_index - 1.3) + Math.exp (x2 + x3 * Math.log (bhage-0.5+x4) - Math.log (site_index - 1.3)));

        height = 1.3 + (site_index - 1.3) * x5;
      }
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
    // Couldn't Find Constant
    /*
    case SI_EP_CAMERON:
      if (bhage > 0.0)
        height = 1.76928 * (site_index - 1.3) * ppow(1 - Math.exp (-0.01558 * bhage), 0.92908);
      else{
        height = tage * tage * 1.3 / y2bh / y2bh;
      }
      break;
      */


    case SI_BA_NIGHGI:
    case SI_BL_THROWERGI:
    case SI_PY_NIGHGI:
    case SI_CWI_NIGHGI:
    case SI_FDC_NIGHGI:
    case SI_FDI_NIGHGI:
    case SI_HWC_NIGHGI:
    case SI_HWC_NIGHGI99:
    case SI_HWI_NIGHGI:
    case SI_LW_NIGHGI:
    //case SI_PLI_NIGHGI: Couldnt Find constant
    case SI_PLI_NIGHGI97:
    case SI_SE_NIGHGI:
    case SI_SS_NIGHGI:
    case SI_SS_NIGHGI99:
    case SI_SW_NIGHGI:
    case SI_SW_NIGHGI99:
    case SI_SW_NIGHGI2004:


      height = gi_si2ht (cu_index, bhage, site_index);
      break;


    default:
      throw new CurveErrorException("Unknown curve index");
    }

    if(height == SI_ERR_NO_ANS){
      throw new NoAnswerException("Iteration could not converge (projected height > 999)");
    }
    
  return height;
  }


public static double gi_si2ht (
  short  cu_index,
  double age,
  double site_index){
  double si2ht;
  double step;
  double test_site;


  /* breast height age must be at least 1/2 a year */
  if (age < 0.5){
    throw new GrowthInterceptMinimumException("Variable height growth intercept formulation; bhage < 0.5 years. Age: " + age);
  }

  /* initial guess */
  si2ht = site_index;
  if (si2ht < 1.3){
    si2ht = 1.3;
  }
  step = si2ht / 2;

  /* loop until real close */
  do
    { 
    test_site = Height2SiteIndex.height_to_index(cu_index, age, (short)SI_AT_BREAST, si2ht, (short) SI_EST_DIRECT);
/*
printf ("age=%3.0f, site=%5.2f, test_site=%5.2f, si2ht=%5.2f, step=%9.7f\n",
  age, site_index, test_site, si2ht, step);
*/

    //This code could probably be removed
    if (test_site < 0) /* error */
      {
      si2ht = test_site;
      break;
      }

    if ((test_site - site_index > 0.01) ||
        (test_site - site_index < -0.01)){
      /* not close enough */
      if (test_site > site_index){
        if (step > 0){
          step = -step/2.0;
        }
      }
      else{
        if (step < 0){
          step = -step/2.0;
        }
      }
      si2ht += step;
    }
    else{
      /* done */
      break;
    }

    /* check for lack of convergence, so we're not here forever */
    if (step < 0.00001 && step > -0.00001){
      /* we have a value, but perhaps not too accurate */
      break;
    }
    if (si2ht > 999.0){
      si2ht = SI_ERR_NO_ANS;
      break;
    }
    /* site index must be at least 1.3 */
    if (si2ht < 1.3){
      if (step > 0){
        si2ht += step;
      }
      else{
        si2ht -= step;
      }
      step = step / 2.0;
      }
    } while (true);

    if(si2ht == SI_ERR_NO_ANS){
      throw new NoAnswerException("Iteration could not converge (projected height > 999)");
    }
  return si2ht;
  }



public static double hu_garcia_q (double site_index, double bhage){
  double h, q, step, diff, lastdiff;


  q = 0.02;
  step = 0.01;
  lastdiff = 0;
  diff = 0;

  do
    {
    h = hu_garcia_h (q, bhage);
    lastdiff = diff;
    diff = site_index - h;
    if (diff > 0.0000001){
      if (lastdiff < 0){
        step = step / 2.0;
      }
      q += step;
    }
    else if (diff < -0.0000001){
      if (lastdiff > 0){
        step = step / 2.0;
      }
      q -= step;
      if (q <= 0){
        q = 0.0000001;
      }
    }
    else{
      break;
    }
    if (step < 0.0000001){
      break;
    }
  } while (true);

  return q;
  }


public static double hu_garcia_h (double q, double bhage)
  {
  double a, height;


  a = 283.9 * Math.pow (q, 0.5137);
  height = a * Math.pow (1 - (1 - Math.pow (1.3 / a, 0.5829)) * Math.exp (-q * (bhage - 0.5)), 1.71556);
  return height;
  }
}
