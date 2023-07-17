package ca.bc.gov.nrs.vdyp.common_calculators;

public class SpecRMap {
    /*
 * specrmap.c
 * - determines the default species/curve index for a given species code.
 * - initial species code remappings provided by Inventory Branch.
 * - species codes can be 1-3 letters, in upper or lower case.
 * - error codes:
 *     SI_ERR_FIZ:  unknown FIZ code
 *     SI_ERR_CODE: unknown species code
 *
 * 1994 oct 19 - Moved here from FredTab.
 * 1996 jun 3  - Changed remap of YC to CW.
 *             - Changed remap of EP to AT.
 *             - Changed remap of MB to DR.
 *             - Changed remap of PA to PLI.
 *             - Changed SS_GOUDIE to SS_NIGH as default curve for Ss.
 *          27 - Changed error code of -1 to -8.
 *             - Changed error code of -2 to -7.
 *      aug 9  - Changed error codes to defined constants.
 * 1997 mar 24 - Split HW into HWC and HWI.
 *             - Added Nigh's Hwi.
 *      aug 27 - Added conditional compilation around HWI_NIGH.
 *      nov 17 - Changed mapping of PJ from PLI_GOUDIE_DRY to PJ_HUANG_NAT.
 *          21 - Changed mapping of PJ back to PLI_GOUDIE_DRY.
 *          26 - Changed remapping of MB from DR to ACT.
 * 1998 sep 17 - Added some ifdefs.
 *             - Changed code to allow checking 3-letter codes, and allow
 *               lower case.
 * 1999 jan 8  - Changed int to short int.
 *             - Changed to return species index, not curve index.
 *      may 31 - Split Ac into Acb and Act.
 *      sep 24 - Added Bp.
 * 2000 jul 24 - Split Cw into Cwc and Cwi.
 *      oct 10 - Implemented Cwc/Cwi.
 *      nov 3  - Changed remap of Hm -> Hwc to stay Hm.
 * 2001 jan 4  - If "CW" was entered, a bug made it always return "ACT". Fixed.
 *             - If "C" was entered, "CWC" was always returned. Fixed.
 *          17 - Changed mapping of SE.
 *      mar 14 - Bug fix in 'S'. A 'break;' was missing, causing any
 *               single letter 'S' to fall through to the SB case.
 * 2002 jan 30 - Added vdyp_species_remap().
 *      feb 8  - Added a few more cases to vdyp_species_remap().
 *          21 - Bug fixes and additions to vdyp_species_remap().
 *      mar 7  - Bug fixes and additions to vdyp_species_remap().
 *          25 - Removed vdyp_species_remap().
 *             - Expanded and limited species_remap().
 *      jun 21 - Changed dynamic sc2[] to static array.
 *      nov 29 - Added species_map().
 * 2003 jan 16 - Added Hwc, Hwi, Cwc, Cwi, Pli to species_map().
 *      aug 7  - Added 40 more species.
 *      sep 11 - Added Fd, Pl, Hw, Cw.
 * 2005 oct 20 - Changed PJ mapping from PLI to PJ.
 * 2009 aug 18 - Changed E* remaps from At to Ep.
 * 2015 apr 9  - Removed species code "Bv".
 */

#include <string.h>
#include <ctype.h>
#include "sindex.h"

short int species_map (char *sc)
  {
  short int i, i2;
  char sc2[10];
  
  
  i2 = 0;
  for (i = 0; i < strlen (sc) && i < 10; i++)
    {
    if (sc[i] != ' ')
      {
      sc2[i2] = toupper (sc[i]);
      i2++;
      }
    }
  sc2[i2] = '\0';
  
  if (strcmp (sc2, "A"   ) == 0) return SI_SPEC_A;
  if (strcmp (sc2, "ABAL") == 0) return SI_SPEC_ABAL;
  if (strcmp (sc2, "ABCO") == 0) return SI_SPEC_ABCO;
  if (strcmp (sc2, "AC"  ) == 0) return SI_SPEC_AC;
  if (strcmp (sc2, "ACB" ) == 0) return SI_SPEC_ACB;
  if (strcmp (sc2, "ACT" ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "AD"  ) == 0) return SI_SPEC_AD;
  if (strcmp (sc2, "AH"  ) == 0) return SI_SPEC_AH;
  if (strcmp (sc2, "AT"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "AX"  ) == 0) return SI_SPEC_AX;
  if (strcmp (sc2, "B"   ) == 0) return SI_SPEC_B;
  if (strcmp (sc2, "BA"  ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BB"  ) == 0) return SI_SPEC_BB;
  if (strcmp (sc2, "BC"  ) == 0) return SI_SPEC_BC;
  if (strcmp (sc2, "BG"  ) == 0) return SI_SPEC_BG;
  if (strcmp (sc2, "BI"  ) == 0) return SI_SPEC_BI;
  if (strcmp (sc2, "BL"  ) == 0) return SI_SPEC_BL;
  if (strcmp (sc2, "BM"  ) == 0) return SI_SPEC_BM;
  if (strcmp (sc2, "BP"  ) == 0) return SI_SPEC_BP;
//  if (strcmp (sc2, "BV"  ) == 0) return SI_SPEC_BV;
  if (strcmp (sc2, "C"   ) == 0) return SI_SPEC_C;
  if (strcmp (sc2, "CI"  ) == 0) return SI_SPEC_CI;
  if (strcmp (sc2, "CP"  ) == 0) return SI_SPEC_CP;
  if (strcmp (sc2, "CW"  ) == 0) return SI_SPEC_CW;
  if (strcmp (sc2, "CWC" ) == 0) return SI_SPEC_CWC;
  if (strcmp (sc2, "CWI" ) == 0) return SI_SPEC_CWI;
  if (strcmp (sc2, "CY"  ) == 0) return SI_SPEC_CY;
  if (strcmp (sc2, "D"   ) == 0) return SI_SPEC_D;
  if (strcmp (sc2, "DG"  ) == 0) return SI_SPEC_DG;
  if (strcmp (sc2, "DM"  ) == 0) return SI_SPEC_DM;
  if (strcmp (sc2, "DR"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "E"   ) == 0) return SI_SPEC_E;
  if (strcmp (sc2, "EA"  ) == 0) return SI_SPEC_EA;
  if (strcmp (sc2, "EB"  ) == 0) return SI_SPEC_EB;
  if (strcmp (sc2, "EE"  ) == 0) return SI_SPEC_EE;
  if (strcmp (sc2, "EP"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "ES"  ) == 0) return SI_SPEC_ES;
  if (strcmp (sc2, "EW"  ) == 0) return SI_SPEC_EW;
  if (strcmp (sc2, "EXP" ) == 0) return SI_SPEC_EXP;
  if (strcmp (sc2, "FD"  ) == 0) return SI_SPEC_FD;
  if (strcmp (sc2, "FDC" ) == 0) return SI_SPEC_FDC;
  if (strcmp (sc2, "FDI" ) == 0) return SI_SPEC_FDI;
  if (strcmp (sc2, "G"   ) == 0) return SI_SPEC_G;
  if (strcmp (sc2, "GP"  ) == 0) return SI_SPEC_GP;
  if (strcmp (sc2, "GR"  ) == 0) return SI_SPEC_GR;
  if (strcmp (sc2, "H"   ) == 0) return SI_SPEC_H;
  if (strcmp (sc2, "HM"  ) == 0) return SI_SPEC_HM;
  if (strcmp (sc2, "HW"  ) == 0) return SI_SPEC_HW;
  if (strcmp (sc2, "HWC" ) == 0) return SI_SPEC_HWC;
  if (strcmp (sc2, "HWI" ) == 0) return SI_SPEC_HWI;
  if (strcmp (sc2, "HXM" ) == 0) return SI_SPEC_HXM;
  if (strcmp (sc2, "IG"  ) == 0) return SI_SPEC_IG;
  if (strcmp (sc2, "IS"  ) == 0) return SI_SPEC_IS;
  if (strcmp (sc2, "J"   ) == 0) return SI_SPEC_J;
  if (strcmp (sc2, "JR"  ) == 0) return SI_SPEC_JR;
  if (strcmp (sc2, "K"   ) == 0) return SI_SPEC_K;
  if (strcmp (sc2, "KC"  ) == 0) return SI_SPEC_KC;
  if (strcmp (sc2, "L"   ) == 0) return SI_SPEC_L;
  if (strcmp (sc2, "LA"  ) == 0) return SI_SPEC_LA;
  if (strcmp (sc2, "LE"  ) == 0) return SI_SPEC_LE;
  if (strcmp (sc2, "LT"  ) == 0) return SI_SPEC_LT;
  if (strcmp (sc2, "LW"  ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "M"   ) == 0) return SI_SPEC_M;
  if (strcmp (sc2, "MB"  ) == 0) return SI_SPEC_MB;
  if (strcmp (sc2, "ME"  ) == 0) return SI_SPEC_ME;
  if (strcmp (sc2, "MN"  ) == 0) return SI_SPEC_MN;
  if (strcmp (sc2, "MR"  ) == 0) return SI_SPEC_MR;
  if (strcmp (sc2, "MS"  ) == 0) return SI_SPEC_MS;
  if (strcmp (sc2, "MV"  ) == 0) return SI_SPEC_MV;
  if (strcmp (sc2, "OA"  ) == 0) return SI_SPEC_OA;
  if (strcmp (sc2, "OB"  ) == 0) return SI_SPEC_OB;
  if (strcmp (sc2, "OC"  ) == 0) return SI_SPEC_OC;
  if (strcmp (sc2, "OD"  ) == 0) return SI_SPEC_OD;
  if (strcmp (sc2, "OE"  ) == 0) return SI_SPEC_OE;
  if (strcmp (sc2, "OF"  ) == 0) return SI_SPEC_OF;
  if (strcmp (sc2, "OG"  ) == 0) return SI_SPEC_OG;
  if (strcmp (sc2, "P"   ) == 0) return SI_SPEC_P;
  if (strcmp (sc2, "PA"  ) == 0) return SI_SPEC_PA;
  if (strcmp (sc2, "PF"  ) == 0) return SI_SPEC_PF;
  if (strcmp (sc2, "PJ"  ) == 0) return SI_SPEC_PJ;
  if (strcmp (sc2, "PL"  ) == 0) return SI_SPEC_PL;
  if (strcmp (sc2, "PLC" ) == 0) return SI_SPEC_PLC;
  if (strcmp (sc2, "PLI" ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PM"  ) == 0) return SI_SPEC_PM;
  if (strcmp (sc2, "PR"  ) == 0) return SI_SPEC_PR;
  if (strcmp (sc2, "PS"  ) == 0) return SI_SPEC_PS;
  if (strcmp (sc2, "PW"  ) == 0) return SI_SPEC_PW;
  if (strcmp (sc2, "PXJ" ) == 0) return SI_SPEC_PXJ;
  if (strcmp (sc2, "PY"  ) == 0) return SI_SPEC_PY;
  if (strcmp (sc2, "Q"   ) == 0) return SI_SPEC_Q;
  if (strcmp (sc2, "QE"  ) == 0) return SI_SPEC_QE;
  if (strcmp (sc2, "QG"  ) == 0) return SI_SPEC_QG;
  if (strcmp (sc2, "R"   ) == 0) return SI_SPEC_R;
  if (strcmp (sc2, "RA"  ) == 0) return SI_SPEC_RA;
  if (strcmp (sc2, "S"   ) == 0) return SI_SPEC_S;
  if (strcmp (sc2, "SA"  ) == 0) return SI_SPEC_SA;
  if (strcmp (sc2, "SB"  ) == 0) return SI_SPEC_SB;
  if (strcmp (sc2, "SE"  ) == 0) return SI_SPEC_SE;
  if (strcmp (sc2, "SI"  ) == 0) return SI_SPEC_SI;
  if (strcmp (sc2, "SN"  ) == 0) return SI_SPEC_SN;
  if (strcmp (sc2, "SS"  ) == 0) return SI_SPEC_SS;
  if (strcmp (sc2, "SW"  ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SX"  ) == 0) return SI_SPEC_SX;
  if (strcmp (sc2, "SXB" ) == 0) return SI_SPEC_SXB;
  if (strcmp (sc2, "SXE" ) == 0) return SI_SPEC_SXE;
  if (strcmp (sc2, "SXL" ) == 0) return SI_SPEC_SXL;
  if (strcmp (sc2, "SXS" ) == 0) return SI_SPEC_SXS;
  if (strcmp (sc2, "SXW" ) == 0) return SI_SPEC_SXW;
  if (strcmp (sc2, "SXX" ) == 0) return SI_SPEC_SXX;
  if (strcmp (sc2, "T"   ) == 0) return SI_SPEC_T;
  if (strcmp (sc2, "TW"  ) == 0) return SI_SPEC_TW;
  if (strcmp (sc2, "U"   ) == 0) return SI_SPEC_U;
  if (strcmp (sc2, "UA"  ) == 0) return SI_SPEC_UA;
  if (strcmp (sc2, "UP"  ) == 0) return SI_SPEC_UP;
  if (strcmp (sc2, "V"   ) == 0) return SI_SPEC_V;
  if (strcmp (sc2, "VB"  ) == 0) return SI_SPEC_VB;
  if (strcmp (sc2, "VP"  ) == 0) return SI_SPEC_VP;
  if (strcmp (sc2, "VS"  ) == 0) return SI_SPEC_VS;
  if (strcmp (sc2, "VV"  ) == 0) return SI_SPEC_VV;
  if (strcmp (sc2, "W"   ) == 0) return SI_SPEC_W;
  if (strcmp (sc2, "WA"  ) == 0) return SI_SPEC_WA;
  if (strcmp (sc2, "WB"  ) == 0) return SI_SPEC_WB;
  if (strcmp (sc2, "WD"  ) == 0) return SI_SPEC_WD;
  if (strcmp (sc2, "WI"  ) == 0) return SI_SPEC_WI;
  if (strcmp (sc2, "WP"  ) == 0) return SI_SPEC_WP;
  if (strcmp (sc2, "WS"  ) == 0) return SI_SPEC_WS;
  if (strcmp (sc2, "WT"  ) == 0) return SI_SPEC_WT;
  if (strcmp (sc2, "X"   ) == 0) return SI_SPEC_X;
  if (strcmp (sc2, "XC"  ) == 0) return SI_SPEC_XC;
  if (strcmp (sc2, "XH"  ) == 0) return SI_SPEC_XH;
  if (strcmp (sc2, "Y"   ) == 0) return SI_SPEC_Y;
  if (strcmp (sc2, "YC"  ) == 0) return SI_SPEC_YC;
  if (strcmp (sc2, "YP"  ) == 0) return SI_SPEC_YP;
  if (strcmp (sc2, "Z"   ) == 0) return SI_SPEC_Z;
  if (strcmp (sc2, "ZC"  ) == 0) return SI_SPEC_ZC;
  if (strcmp (sc2, "ZH"  ) == 0) return SI_SPEC_ZH;
  
  return SI_ERR_CODE;
  }


short int species_remap (char *sc, char fiz)
  {
  short int i, i2;
  char sc2[10];
  
  
  i2 = 0;
  for (i = 0; i < strlen (sc) && i < 10; i++)
    {
    if (sc[i] != ' ')
      {
      sc2[i2] = toupper (sc[i]);
      i2++;
      }
    }
  sc2[i2] = '\0';
  
  if (strcmp (sc2, "A"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "ABAL") == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "ABCO") == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "AC"  ) == 0) return SI_SPEC_ACB;
  if (strcmp (sc2, "ACB" ) == 0) return SI_SPEC_ACB;
  if (strcmp (sc2, "ACT" ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "AD"  ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "AH"  ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "AT"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "AX"  ) == 0) return SI_SPEC_ACB;
  if (strcmp (sc2, "B"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_BA;
      case FIZ_INTERIOR: return SI_SPEC_BL;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "BA"  ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BAC" ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BAI" ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BB"  ) == 0) return SI_SPEC_BL;
  if (strcmp (sc2, "BC"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_BA;
      case FIZ_INTERIOR: return SI_SPEC_BL;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "BG"  ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BI"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "BL"  ) == 0) return SI_SPEC_BL;
  if (strcmp (sc2, "BM"  ) == 0) return SI_SPEC_BA;
  if (strcmp (sc2, "BN"  ) == 0) return SI_SPEC_BP;
  if (strcmp (sc2, "BP"  ) == 0) return SI_SPEC_BP;
//  if (strcmp (sc2, "BV"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "C"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "CI"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "COT" ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "CP"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "CT"  ) == 0) return SI_SPEC_ACT;
  if (strcmp (sc2, "CW"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "CWC" ) == 0) return SI_SPEC_CWC;
  if (strcmp (sc2, "CWI" ) == 0) return SI_SPEC_CWI;
  if (strcmp (sc2, "CY"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "D"   ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "DF"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "DG"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "DM"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "DR"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "E"   ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EA"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EB"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EE"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EP"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "ES"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EW"  ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "EXP" ) == 0) return SI_SPEC_EP;
  if (strcmp (sc2, "F"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "FD"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "FDC" ) == 0) return SI_SPEC_FDC;
  if (strcmp (sc2, "FDI" ) == 0) return SI_SPEC_FDI;
  if (strcmp (sc2, "G"   ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "GP"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "GR"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "H"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_HWC;
      case FIZ_INTERIOR: return SI_SPEC_HWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "HM"  ) == 0) return SI_SPEC_HM;
  if (strcmp (sc2, "HW"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_HWC;
      case FIZ_INTERIOR: return SI_SPEC_HWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "HWC" ) == 0) return SI_SPEC_HWC;
  if (strcmp (sc2, "HWI" ) == 0) return SI_SPEC_HWI;
  if (strcmp (sc2, "HXM" ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_HWC;
      case FIZ_INTERIOR: return SI_SPEC_HWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "IG"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "IS"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "J"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "JR"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "K"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "KC"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "L"   ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "LA"  ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "LE"  ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "LT"  ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "LW"  ) == 0) return SI_SPEC_LW;
  if (strcmp (sc2, "M"   ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "MB"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "ME"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "MN"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "MR"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "MS"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "MV"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "OA"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "OB"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "OC"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "OD"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "OE"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "OF"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "OG"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "P"   ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PA"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PF"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PJ"  ) == 0) return SI_SPEC_PJ;
  if (strcmp (sc2, "PL"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PLC" ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PLI" ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PM"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PR"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PS"  ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PV"  ) == 0) return SI_SPEC_PY;
  if (strcmp (sc2, "PW"  ) == 0) return SI_SPEC_PW;
  if (strcmp (sc2, "PXJ" ) == 0) return SI_SPEC_PLI;
  if (strcmp (sc2, "PY"  ) == 0) return SI_SPEC_PY;
  if (strcmp (sc2, "Q"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "QE"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "QG"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "R"   ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "RA"  ) == 0) return SI_SPEC_DR;
  if (strcmp (sc2, "S"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SW;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "SA"  ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SB"  ) == 0) return SI_SPEC_SB;
  if (strcmp (sc2, "SE"  ) == 0) return SI_SPEC_SE;
  if (strcmp (sc2, "SI"  ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SN"  ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SS"  ) == 0) return SI_SPEC_SS;
  if (strcmp (sc2, "SW"  ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SX"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SW;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "SXB" ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SXE" ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SE;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "SXL" ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SW;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "SXS" ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SW;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "SXW" ) == 0) return SI_SPEC_SW;
  if (strcmp (sc2, "SXX" ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_SS;
      case FIZ_INTERIOR: return SI_SPEC_SW;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "T"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_HWC;
      case FIZ_INTERIOR: return SI_SPEC_HWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "TW"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_HWC;
      case FIZ_INTERIOR: return SI_SPEC_HWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "U"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "UA"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "UP"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "V"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "VB"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "VP"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "VS"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "VV"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "W"   ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WA"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WB"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WD"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WI"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WP"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WS"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "WT"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "X"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "XC"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "XH"  ) == 0) return SI_SPEC_AT;
  if (strcmp (sc2, "Y"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "YC"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "YP"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_CWC;
      case FIZ_INTERIOR: return SI_SPEC_CWI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "Z"   ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "ZC"  ) == 0)
    switch (fiz_check (fiz))
      {
      case FIZ_COAST:    return SI_SPEC_FDC;
      case FIZ_INTERIOR: return SI_SPEC_FDI;
      default:           return SI_ERR_CODE;
      }
  if (strcmp (sc2, "ZH"  ) == 0) return SI_SPEC_AT;
  
  return SI_ERR_CODE;
  }

}
