package ca.bc.gov.nrs.vdyp.common_calculators;

public class SiteIndex2Age {
    /*
 * si2age.c
 * - given site index and site height, computes age.
 * - error codes (returned as age value):
 *     SI_ERR_LT13: site index or height < 1.3m
 *     SI_ERR_NO_ANS: iteration could not converge (or projected age > 999)
 *     SI_ERR_CURVE: unknown curve index
 *     SI_ERR_GI_TOT: cannot compute growth intercept when using total age
 *
 * 1990 aug 15 - Created.
 * 1991 jan 15 - Added check for no convergence.
 *      jul 23 - Added code to compute age directly for Bruce's Fdc,
 *               Wiley's Hw, and Goudie's Pli, Ss, and Sw.
 *      dec 2  - Changed to independent Sindex functions.
 * 1992 jan 10 - Added defines for how function prototypes and definitions
 *               are handled.
 *      apr 29 - Noticed that Pli Goudie Dry was included, but Wet was not.
 *               Fixed.
 *               Removed difference between plantations and natural stands
 *               for Pli Goudie.
 * 1994 dec 6  - Added another check for getting stuck when iterating.
 * 1995 dec 19 - Added Hm.
 * 1996 jun 27 - Added error code of -4, instead of 999.
 *      jul 30 - Added check for incoming height or site < 1.3m.
 *      aug 1  - Refined iterating loop when growth intercept curves are used.
 *          8  - Changed error codes to defined constants.
 * 1997 jan 23 - Added special function to handle growth intercept.
 *      feb 5  - Changed check for top height or site index < 1.3 to be
 *               <= 1.3.
 *      mar 21 - Added Nigh's 1997 Hwi GI.
 *             - Changed define names: FDC_NIGH, HW_NIGH, PLI_NIGH, SW_NIGH
 *               all have "GI" added after them.
 *             - Added Nigh's 1997 Pl GI.
 *             - Added Nigh's 1997 Fdi GI.
 *          24 - Split HW into HWI and HWC.
 *      jul 8  - Replaced checking height <= 1.3 and returning error code
 *               with checking and returning 0 if age type is breast-height.
 *      sep 16 - Changed a "log(50)" to "log(50.0)" in Goudie formulation.
 *      nov 17 - Added Pf as Pli Goudie.
 *             - Added Se as Sw Goudie.
 * 1998 apr 7  - Added inclusion of sindex2.h.
 *      may 27 - If site height is <= 1.3 and age type is breast height,
 *               return value is SI_ERR_NO_ANS.
 * 1999 jan 8  - Changed int to short int.
 *      aug 20 - Changed iteration to always be to breast height age.
 *          24 - Added error count to ensure iterate loop doesn't run forever.
 *             - Added additional error checks in iterate loop.
 *          26 - Removed y2bh as parameter to gi_iterate().
 *      sep 24 - If an error occurs in iterating, don't convert age type.
 *             - If age is really tiny and total age, return 0.
 *      oct 1  - D'oh!  The aug 20 change to make iterating always be
 *               by breast height makes it impossible to iterate for
 *               heights below breast-height!  Trying total age now...
 * 2000 jan 27 - Added some missing GI cases.
 *      apr 25 - Added call to age_to_age() in iterate() when converting
 *               from total age to breast height age.
 *      jul 24 - Changed CW to CWI.
 *      oct 10 - Changed check for site <= 1.3 to < 1.3.
 * 2009 may 6  - Forced pure y2bh to be computed for Fdc-Bruce.
 *      apr 16 - Added 2010 Sw Hu and Garcia.
 * 2016 mar 9  - Added parameter to index_to_height().
 */

#include <stdio.h>
#include <math.h>
#include "sindex.h"

/*
#define TEST 1
*/

#define PPOW(x,y) \
  (((x) <= 0.0) ? 0.0 : pow (x, y))

#define LLOG(x) \
  (((x) <= 0.0) ? log (.00001) : log (x))

#define MAX_AGE 999.0

static double iterate (short int, double, short int, double, double);
static double gi_iterate (short int, double, short int, double);
static double hu_garcia_q (double, double);
static double hu_garcia_h (double, double);
static double hu_garcia_bha (double, double);

#ifdef TEST
FILE *testfile;
#endif

double index_to_age (
  short int cu_index,
  double site_height,
  short int age_type,
  double site_index,
  double y2bh)
  {
  double x1, x2, x3, x4;
  double a, b, c;
#ifdef HOOP
  double ht5, ht10;
#endif
  double age;
  
  
#ifdef TEST
testfile = fopen ("si2age.tst", "w");
#endif
  
  if (site_height < 1.3)
    {
    if (age_type == SI_AT_BREAST)
      return SI_ERR_LT13;
    
    if (site_height <= 0.0001)
      return 0;
    }
  
  if (site_index < 1.3)
    return SI_ERR_LT13;
  
  switch (cu_index)
    {
#ifdef SI_FDC_BRUCE
    case SI_FDC_BRUCE:
      // 2009 may 6: force a non-rounded y2bh
      y2bh = 13.25 - site_index / 6.096;
      
      x1 = site_index / 30.48;
      x2 = -0.477762 + x1 * (-0.894427 + x1 * (0.793548 - x1 * 0.171666));
      x3 = PPOW (50.0+y2bh, x2);
      x4 = LLOG (1.372 / site_index) / (PPOW (y2bh, x2) - x3);
      
      x1 = LLOG (site_height / site_index) / x4 + x3;
      if (x1 < 0)
        age = SI_ERR_NO_ANS;
      else
        {
        age = PPOW (x1, 1 / x2);
        
        if (age_type == SI_AT_BREAST)
          age -= y2bh;
        
        if (age < 0.0)
          age = 0.0;
        else if (age > MAX_AGE)
          age = SI_ERR_NO_ANS;
        }
      break;
#endif

#ifdef SI_SW_HU_GARCIA
    case SI_SW_HU_GARCIA:
      {
      double q;
        
      q = hu_garcia_q (site_index, 50.0);
      age = hu_garcia_bha (q, site_height);
      if (age_type == SI_AT_TOTAL)
        age += y2bh;
      }
      break;
#endif

#ifdef SI_HWC_WILEY
#undef WILEY
#define WILEY 1
    case SI_HWC_WILEY:
#endif

#ifdef SI_HM_WILEY
#undef WILEY
#define WILEY 1
    case SI_HM_WILEY:
#endif

#ifdef WILEY
      if (site_height / 0.3048 < 4.5)
        {
        age = y2bh * PPOW (site_height / 1.37, 0.5);
        
        if (age_type == SI_AT_BREAST)
          age -= y2bh;
        
        if (age < 0.0)
          age = 0.0;
        }
      else
        {
        x1 = 2500 / (site_index / 0.3048 - 4.5);
        x2 = -1.7307 + 0.1394 * x1;
        x3 = -0.0616 + 0.0137 * x1;
        x4 = 0.00192428 + 0.00007024 * x1;
        
        x1 = (4.5 - site_height / 0.3048);
        a = 1 + x1 * x4;
        b = x1 * x3;
        c = x1 * x2;
        
        x1 = PPOW (b * b - 4 * a * c, 0.5);
        if (x1 == 0.0)
          age = SI_ERR_NO_ANS;
        else
          {
          age = (-b + x1) / (2 * a);
          
          if (age_type == SI_AT_TOTAL)
            age += y2bh;
          
          if (age < 0)
            age = SI_ERR_NO_ANS;
          else if (age > MAX_AGE)
            age = SI_ERR_NO_ANS;
          }
        }
      
      if (age < 10 && age > 0)
        {
        age = iterate (cu_index, site_height, age_type, site_index, y2bh);
#ifdef HOOP
            ht5 = index_to_height (cu_index, 5.0, SI_AT_BREAST, site_index, y2bh, 0.5); // 0.5 may have to change
            
            if (site_height <= ht5)
              site_height -= (1 - ((ht5 - site_height) / ht5)) * 1.5;
            else
              {
              ht10 = index_to_height (cu_index, 10.0, SI_AT_BREAST, site_index, y2bh, 0.5); // 0.5 may have to change
              site_height -= (((ht10 - site_height) / (ht10 - ht5))) * 1.5;
              }
#endif
        }
      break;
#endif

#ifdef SI_PLI_GOUDIE_DRY
#undef GOUDIE
#define GOUDIE 1
    case SI_PLI_GOUDIE_DRY:
#endif

#ifdef SI_PLI_GOUDIE_WET
#undef GOUDIE
#define GOUDIE 1
    case SI_PLI_GOUDIE_WET:
#endif

#ifdef SI_PF_GOUDIE_DRY
#undef GOUDIE
#define GOUDIE 1
    case SI_PF_GOUDIE_DRY:
#endif

#ifdef SI_PF_GOUDIE_WET
#undef GOUDIE
#define GOUDIE 1
    case SI_PF_GOUDIE_WET:
#endif

#ifdef SI_SS_GOUDIE
#undef GOUDIE
#define GOUDIE 1
    case SI_SS_GOUDIE:
#endif

#ifdef SI_SE_GOUDIE_PLA
#undef GOUDIE
#define GOUDIE 1
    case SI_SE_GOUDIE_PLA:
#endif

#ifdef SI_SE_GOUDIE_NAT
#undef GOUDIE
#define GOUDIE 1
    case SI_SE_GOUDIE_NAT:
#endif

#ifdef SI_SW_GOUDIE_PLA
#undef GOUDIE
#define GOUDIE 1
    case SI_SW_GOUDIE_PLA:
#endif

#ifdef SI_SW_GOUDIE_NAT
#undef GOUDIE
#define GOUDIE 1
    case SI_SW_GOUDIE_NAT:
#endif

#ifdef GOUDIE
      if (site_height < 1.3)
        {
        age = y2bh * PPOW (site_height / 1.3, 0.5);
        
        if (age_type == SI_AT_BREAST)
          age -= y2bh;
        
        if (age < 0.0)
          age = 0.0;
        }
      else
        {
        switch (cu_index)
          {
#ifdef SI_PF_GOUDIE_DRY
          case SI_PF_GOUDIE_DRY:
            x1 = -1.00726;
            x2 = 7.81498;
            x3 = -1.28517;
            break;
#endif
            
#ifdef SI_PF_GOUDIE_WET
          case SI_PF_GOUDIE_WET:
            x1 = -0.935;
            x2 = 7.81498;
            x3 = -1.28517;
            break;
#endif

#ifdef SI_PLI_GOUDIE_DRY
          case SI_PLI_GOUDIE_DRY:
            x1 = -1.00726;
            x2 = 7.81498;
            x3 = -1.28517;
            break;
#endif
            
#ifdef SI_PLI_GOUDIE_WET
          case SI_PLI_GOUDIE_WET:
            x1 = -0.935;
            x2 = 7.81498;
            x3 = -1.28517;
            break;
#endif

#ifdef SI_SS_GOUDIE
          case SI_SS_GOUDIE:
            x1 = -1.5282;
            x2 = 11.0605;
            x3 = -1.5108;
            break;
#endif
        
#ifdef SI_SE_GOUDIE_PLA
          case SI_SE_GOUDIE_PLA:
            x1 = -1.2866;
            x2 = 9.7936;
            x3 = -1.4661;
            break;
#endif
          
#ifdef SI_SE_GOUDIE_NAT
          case SI_SE_GOUDIE_NAT:
            x1 = -1.2866;
            x2 = 9.7936;
            x3 = -1.4661;
            break;
#endif

#ifdef SI_SW_GOUDIE_PLA
          case SI_SW_GOUDIE_PLA:
            x1 = -1.2866;
            x2 = 9.7936;
            x3 = -1.4661;
            break;
#endif
          
#ifdef SI_SW_GOUDIE_NAT
          case SI_SW_GOUDIE_NAT:
            x1 = -1.2866;
            x2 = 9.7936;
            x3 = -1.4661;
            break;
#endif
          }
        a = (site_index - 1.3) *
          (1 + exp (x2 + x1 * LLOG (site_index - 1.3) + x3 * log (50.0)));
        b = x2 + x1 * LLOG (site_index - 1.3);
        
        age = exp ((LLOG (a / (site_height - 1.3) - 1) - b) / x3);
        
        if (age_type == SI_AT_TOTAL)
          age += y2bh;
        
        if (age < 0)
          age = 0;
        else if (age > MAX_AGE)
          age = SI_ERR_NO_ANS;
        }
      break;
#endif
    
#ifdef SI_BL_THROWERGI
    case SI_BL_THROWERGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_CWI_NIGHGI
    case SI_CWI_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_FDC_NIGHGI
    case SI_FDC_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_FDI_NIGHGI
    case SI_FDI_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_HWC_NIGHGI
    case SI_HWC_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_HWC_NIGHGI99
    case SI_HWC_NIGHGI99:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_HWI_NIGHGI
    case SI_HWI_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_LW_NIGHGI
    case SI_LW_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_PLI_NIGHGI
    case SI_PLI_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_PLI_NIGHGI97
    case SI_PLI_NIGHGI97:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_SS_NIGHGI
    case SI_SS_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_SS_NIGHGI99
    case SI_SS_NIGHGI99:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_SW_NIGHGI
    case SI_SW_NIGHGI:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
#ifdef SI_SW_NIGHGI99
    case SI_SW_NIGHGI99:
      age = gi_iterate (cu_index, site_height, age_type, site_index);
      break;
#endif
    
    default:
#ifdef TEST
fprintf (testfile, "before iterate()\n");
#endif
      age = iterate (cu_index, site_height, age_type, site_index, y2bh);
      break;
    }
  
#ifdef TEST
fprintf (testfile, "final age: %f", age);
fclose (testfile);
#endif
  return (age);
  }


static double iterate (
  short int cu_index,
  double site_height,
  short int age_type,
  double site_index,
  double y2bh)
  {
  double si2age;
  double step;
  double test_ht;
  short int err_count;
  
  
  /* initial guess */
  si2age = 25;
  step = si2age / 2;
  err_count = 0;
    
  /* do a preliminary test to catch some obvious errors */
  test_ht = index_to_height (cu_index, si2age, SI_AT_TOTAL, site_index, y2bh, 0.5); // 0.5 may have to change
  
  if (test_ht == SI_ERR_CURVE ||
      test_ht == SI_ERR_LT13 ||
      test_ht == SI_ERR_GI_MIN ||
      test_ht == SI_ERR_GI_MAX ||
      test_ht == SI_ERR_GI_TOT)
    return test_ht;
  
  /* loop until real close, or other end condition */
  do
    {
#ifdef TEST
fprintf (testfile, "before index_to_height(age=%f, age_type=%d, site_index=%f, y2bh=%f)\n",
  si2age, age_type, site_index, y2bh);
#endif
    test_ht = index_to_height (cu_index, si2age, SI_AT_TOTAL, site_index, y2bh, 0.5); // 0.5 may have to change
#ifdef TEST
fprintf (testfile, "index_to_height()=%f\n", test_ht);
#endif
    
/*
printf ("si2age.c: site_height=%f, test_ht=%f, si2age=%f\n", site_height, test_ht, si2age);
*/
      
    if (test_ht == SI_ERR_NO_ANS) /* height > 999 */
      {
      test_ht = 1000; /* should eventualy force an error code */
      err_count++;
      if (err_count == 100)
        {
        si2age = SI_ERR_NO_ANS;
        break;
        }
      }
    
    /* see if we're close enough */
    if ((test_ht - site_height > 0.005) ||
        (test_ht - site_height < -0.005))
      {
      /* not close enough */
      if (test_ht > site_height)
        {
        if (step > 0)
          step = -step/2.0;
        }
      else
        {
        if (step < 0)
          step = -step/2.0;
        }
      si2age += step;
      }
    else
      /* done */
      break;
    
    /* check for lack of convergence, so we're not here forever */
    if (step < 0.00001 && step > -0.00001)
      {
      /* we have a value, but perhaps not too accurate */
      break;
      }
    if (si2age > 999.0)
      {
      si2age = SI_ERR_NO_ANS;
#ifdef TEST
fprintf (testfile, "Failed due to age too high (> 999).\n");
#endif
      break;
      }
    } while (1);
    
  if (si2age >= 0)
    if (age_type == SI_AT_BREAST)
/* was
      si2age -= y2bh;
*/
      si2age = age_to_age (cu_index, si2age, SI_AT_TOTAL, SI_AT_BREAST, y2bh);
  return (si2age);
  }


static double gi_iterate (
  short int cu_index,
  double site_height,
  short int age_type,
  double site_index)
  {
  double age;
  double si2age;
  double test_site;
  double diff;
  double mindiff;
  
  
  if (age_type == SI_AT_TOTAL)
    return SI_ERR_GI_TOT;
  
  diff = 0;
  mindiff = 999;
  si2age = 1;
  for (age = 1; age < 100; age += 1)
    {
#ifdef TEST
fprintf (testfile, "before height_to_index(age=%f, site_height=%f)\n",
  age, site_height);
#endif
    test_site = height_to_index (cu_index, age, SI_AT_BREAST, site_height, SI_EST_DIRECT);
#ifdef TEST
fprintf (testfile, "height_to_index()=%f\n", test_site);
#endif
    if (test_site == SI_ERR_GI_MAX)
      break;
    
    if (test_site > site_index)
      diff = test_site - site_index;
    else
      diff = site_index - test_site;
    
    if (diff < mindiff)
      {
      mindiff = diff;
      si2age = age;
      }
    }
  
  if (si2age == 1)
    {
    /* right answer, or not low enough */
    if (diff > 1)
      {
      /* outside tolerance of 1m */
      return SI_ERR_NO_ANS;
      }
    }
  
  else if (si2age == age-1)
    {
    /* right answer, or not high enough */
    if (diff > 1)
      {
      /* outside tolerance of 1m */
      return SI_ERR_NO_ANS;
      }
    }
  
  return si2age;
  }


static double hu_garcia_q (double site_index, double bhage)
  {
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
    if (diff > 0.0000001)
      {
      if (lastdiff < 0)
        step = step / 2.0;
      q += step;
      }
    else if (diff < -0.0000001)
      {
      if (lastdiff > 0)
        step = step / 2.0;
      q -= step;
      if (q <= 0)
        q = 0.0000001;
      }
    else
      break;
    if (step < 0.0000001)
      break;
    } while (1);
  
  return q;
  }


static double hu_garcia_h (double q, double bhage)
  {
  double a, height;


  a = 283.9 * pow (q, 0.5137);
  height = a * pow (1 - (1 - pow (1.3 / a, 0.5829)) * exp (-q * (bhage - 0.5)), 1.71556);
  return height;
  }


static double hu_garcia_bha (double q, double height)
  {
  double a, bhage;


  a = 283.9 * pow (q, 0.5137);
  bhage = 0.5 - 1 / q * log ((1 - pow (height / a, 0.5829)) / (1 - pow (1.3 / a, 0.5829)));
  return bhage;
  }

}
