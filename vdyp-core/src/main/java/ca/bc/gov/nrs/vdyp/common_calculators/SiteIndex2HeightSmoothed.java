package ca.bc.gov.nrs.vdyp.common_calculators;

public class SiteIndex2HeightSmoothed {
    //
// si2hts.c
// 2016 mar 9  - Adjusted default equations for Pli, Sw, Fdc, Hwc
//               to incorporate height smoothing near 1.3m.
//
#include <stdio.h>
#include <math.h>
#include "sindex.h"

#define PPOW(x,y) \
  (((x) <= 0.0) ? 0.0 : pow (x, y))

#define LLOG(x) \
  (((x) <= 0.0) ? log (.00001) : log (x))

double index_to_height_smoothed (
  short int cu_index,
  double iage,
  short int age_type,
  double site_index,
  double y2bh,
  double seedling_age,
  double seedling_ht)
  {
  double height;  // return value
  double k0, k1;
  double itage;   // user's total age
  double tage;    // total age
  double bhage;   // breast-height age
  double pi;      // proportion of height growth between breast height
                  // ages 0 and 1 that occurs below breast height
  

  if (site_index < 1.3)
    return SI_ERR_LT13;
  
  if (y2bh < 0)
    return SI_ERR_NO_ANS;

  itage = iage;
  if (age_type == SI_AT_BREAST)
    itage = iage + y2bh;
  if (itage < 0.0)
    return SI_ERR_NO_ANS;
  if (itage < 0.00001)
    return 0.0;
  
  if (cu_index == SI_PLI_THROWER ||
      cu_index == SI_SW_GOUDIE_PLAAC ||
      cu_index == SI_SW_GOUDIE_NATAC ||
      cu_index == SI_FDC_BRUCEAC ||
      cu_index == SI_HWC_WILEYAC)
    pi = y2bh - (int) y2bh;
  else
    pi = 0.5;

  bhage = 2;
  do
    {
    height = index_to_height (cu_index, bhage, SI_AT_BREAST, site_index, y2bh, pi);
    if (height < 0)
      return height;
    tage = bhage + (int) y2bh;
    k1 = log ((1.3 - seedling_ht) / (height - seedling_ht)) / log ((y2bh - seedling_age) / (tage - seedling_age));
//printf ("%f %f k1\n", tage, height, k1);
    if (k1 >= 1)
      {
      k0 = (1.3 - seedling_ht) / pow (y2bh - seedling_age, k1);
      break;
      }
    bhage++;
    if (bhage >= 25)
      return SI_ERR_NO_ANS;
    } while (1);
  
  if (seedling_age == 0)
    {
    if (itage <= tage)
      height = k0 * pow (itage, k1);
    else
      height = index_to_height (cu_index, itage, SI_AT_TOTAL, site_index, y2bh, pi);
    }
  else
    {
    if (itage < seedling_age)
      height = seedling_ht / seedling_age * itage;
    else if (itage < tage)
      height = seedling_ht + k0 * pow (itage - seedling_age, k1);
    else
      height = index_to_height (cu_index, itage, SI_AT_TOTAL, site_index, y2bh, pi);
    }
  
  return height;
  }  

}
