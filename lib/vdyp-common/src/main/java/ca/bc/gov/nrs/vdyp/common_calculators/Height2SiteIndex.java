package ca.bc.gov.nrs.vdyp.common_calculators;

import static ca.bc.gov.nrs.vdyp.common_calculators.SiteIndexUtilities.llog;
import static ca.bc.gov.nrs.vdyp.common_calculators.SiteIndexUtilities.ppow;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType.SI_AT_BREAST;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType.SI_AT_TOTAL;
import static ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType.*;

import java.util.function.DoubleUnaryOperator;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.GrowthInterceptMaximumException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.GrowthInterceptMinimumException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.LessThan13Exception;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType;

/**
 * Height2SiteIndex.java - given age and height, computes site index. - if age is total, site index and years to breast
 * height are iterated until stable.
 */
public class Height2SiteIndex {
	// Taken from sindex.h

	/* error codes */
	private static final int SI_ERR_NO_ANS = -4;

	public static double heightToIndex(
			SiteIndexEquation cuIndex, double age, SiteIndexAgeType ageType, double height,
			SiteIndexEstimationType siEstType
	) throws CommonCalculatorException {
		double index;
		double x1, x2;

		if (siEstType == null) {
			// This is the default value.
			siEstType = SI_EST_ITERATE;
		}

		if (ageType == null) {
			ageType = SI_AT_TOTAL;
		}

		/* handle simple cases */
		if (ageType == SI_AT_BREAST) {
			if (height < 1.3) {
				throw new LessThan13Exception("Height < 1.3 for breast height age: " + height);
			}
		} else {
			if (height <= 0) {
				throw new NoAnswerException(
						"Iteration could not converge (projected site index > 999), Height: " + height
				);
			}
		}

		if (age <= 0) {
			throw new NoAnswerException("Iteration could not converge (projected site index > 999), Age: " + age);
		}

		if (ageType == SI_AT_BREAST) {
			index = baHeightToIndex(cuIndex, age, height, siEstType);
		} else {
			if (siEstType == SI_EST_DIRECT) {
				switch (cuIndex) {
				case SI_FDI_THROWER:
					if (age <= 4) {
						/* means less than 1.3m, so can't generate site index */
						/* supposedly this should never happen anyway */
						index = 1.3;
					} else {
						x1 = (age - 4) * (0.39 + 0.3104 * height);
						x2 = 33.3828 * height + x1 + 99;

						index = (x2 + Math.sqrt(x2 * x2 - 4 * 99 * x1)) / (2 * (age - 4));
					}
					break;
				default:
					index = siteIterate(cuIndex, age, SI_AT_TOTAL, height);
					break;
				}
			} else
				index = siteIterate(cuIndex, age, SI_AT_TOTAL, height);
		}

		return index;
	}

	public static double
			baHeightToIndex(SiteIndexEquation cuIndex, double bhage, double height, SiteIndexEstimationType siEstType)
					throws CommonCalculatorException {
		double index;
		double x1, x2;
		double logBhAge;
		double ht13;

		if (siEstType == null) {
			// This is the default value.
			siEstType = SI_EST_ITERATE;
		}

		if (bhage <= 0.5) {
			/* indicator that it can't be done */
			throw new GrowthInterceptMinimumException(
					"Bhage < 0.5 years which indicates that it can't be done, bhage: " + bhage
			);
		} else {
			if (siEstType == SI_EST_DIRECT) {
				switch (cuIndex) {
				case SI_BA_DILUCCA:
					index = height * (1 + Math.exp(6.300852572 + 0.85314673 * Math.log(50.0) - 2.533284275 * (height)))
							/ (1 + Math.exp(6.300852572 + 0.8314673 * Math.log(bhage) - 2.533284275 * llog(height)));
					break;
				case SI_DR_NIGH:
					/* first compute si@bhage25 */
					index = 1.3 + (height - 1.3) * (0.6906 + 21.61 * Math.exp(-1.24 * Math.log(bhage - 0.5)));
					/* now compute si@bhage50 */
					index = -0.4063 + 1.313 * index;
					break;
				case SI_HM_MEANS:
					/* computer index at base age 100 */
					index = 1.37 + 17.22 + (0.58322 + 99.127 * ppow(bhage, -1.18989))
							* (height - 1.37 - 47.926 * ppow(1 - Math.exp(-0.00574787 * bhage), 1.2416));

					/* convert to base age 50 */
					index = ppow( (index + 1.73) / 3.149, 1.2079);
					break;
				case SI_FDI_MILNER:
					index = Utils.computeInFeet(
							height,
							heightFt -> 57.3
									+ (7.06 + 0.02275 * bhage - 1.858 * Math.log(bhage) + 5.496 / (bhage * bhage))
											* (heightFt - 4.5 - 114.6 * Math.pow(1 - Math.exp(-0.01462 * bhage), 1.179))
					);
					break;
				case SI_FDI_THROWER:
					index = 0.39 + 0.3104 * height + 33.3828 * height / bhage;
					break;
				case SI_PLI_THROWER:
					x1 = 1 + Math.exp(6.0925 + 0.7979 * Math.log(50.0) - 2.7338 * Math.log(height));
					x2 = 1 + Math.exp(6.0925 + 0.7979 * Math.log(bhage) - 2.7338 * Math.log(height));

					index = height * x1 / x2;
					break;
				// #undef LA_MILNER Removed since never used again?
				// #define LA_MILNER 1 Removed since never used again?
				// case SI_LA_MILNER: Couldn't find constant so removed
				// #undef LT_MILNER Removed since never used again?
				// #define LT_MILNER 1 Removed since never used again?
				// case SI_LT_MILNER: Couldn't find constant so removed
				// #undef LW_MILNER Removed since never used again?
				// #define LW_MILNER 1 Removed since never used again?
				case SI_LW_MILNER:
					index = Utils.computeInFeet(
							height,
							heightFt -> 69.0
									+ (-0.8019 + 17.06 / bhage + 0.4268 * Math.log(bhage) - 0.00009635 * bhage * bhage)
											* (heightFt - 4.5 - 127.8 * Math.pow(1 - Math.exp(-0.01655 * bhage), 1.196))
					);
					break;
				case SI_PLI_DEMPSTER:
					logBhAge = Math.log(bhage);

					ht13 = height - 1.3;

					index = 1.3 + 10.9408 + 1.6753 * ht13 - 0.9322 * logBhAge * logBhAge + 0.0054 * bhage * logBhAge
							+ 8.2281 * ht13 / bhage - 0.2569 * ht13 * llog(ht13);
					break;
				case SI_PLI_MILNER:
					index = Utils.computeInFeet(
							height,
							heightFt -> 59.6 + (1.055 - 0.006344 * bhage + 14.82 / bhage - 5.212 / (bhage * bhage))
									* (heightFt - 4.5 - 96.93 * Math.pow(1 - Math.exp(-0.01955 * bhage), 1.216))
					);
					break;
				case SI_PY_MILNER:
					index = Utils.computeInFeet(
							height,
							heightFt -> 59.6
									+ (4.787 + 0.012544 * bhage - 1.141 * Math.log(bhage) + 11.44 / (bhage * bhage))
											* (heightFt - 4.5 - 121.4 * Math.pow(1 - Math.exp(-0.01756 * bhage), 1.483))
					);
					break;
				case SI_PW_CURTIS:
					x1 = Math.log(bhage) - Math.log(50.0);
					x2 = x1 * x1;

					index = Utils.computeInFeet(
							height,
							heightFt -> Math.exp(-2.608801 * x1 - 0.715601 * x2)
									* Math.pow(heightFt, 1.0 + 0.408404 * x1 + 0.138199 * x2)
					);
					break;
				case SI_SW_HU_GARCIA: {
					double q;

					q = huGarciaQ(height, bhage);
					index = huGarciaH(q, 50.0);
				}
					break;
				case SI_SW_DEMPSTER:
					logBhAge = Math.log(bhage);

					ht13 = height - 1.3;

					index = 1.3 + 10.3981 + 0.3244 * ht13 + 0.006 * bhage * logBhAge - 0.838 * logBhAge * logBhAge
							+ 27.4874 * ht13 / bhage + 1.1914 * llog(ht13);
					break;
				case SI_SB_DEMPSTER:
					logBhAge = Math.log(bhage);

					ht13 = height - 1.3;

					index = 1.3 + 4.9038 + 0.8118 * ht13 - 0.3638 * logBhAge * logBhAge + 24.0308 * ht13 / bhage
							- 0.1021 * ht13 * llog(ht13);
					break;

				// #ifdef SI_EA_GOUDIE Couldn't find constant so removed
				// #undef SI_AT_GOUDIE
				// #define SI_AT_GOUDIE 1
				// case SI_EA_GOUDIE:
				// #endif

				// #ifdef SI_EP_GOUDIE Couldn't find constant so removed
				// #undef SI_AT_GOUDIE
				// #define SI_AT_GOUDIE 1
				// case SI_EP_GOUDIE:
				// #endif

				// #ifdef SI_AT_GOUDIE
				// #undef SI_AT_GOUDIE
				// #define SI_AT_GOUDIE 1
				case SI_AT_GOUDIE:
					// endif

					// #ifdef SI_AT_GOUDIE
					logBhAge = Math.log(bhage);

					index = 1.3 + 17.0101 + 0.8784 * (height - 1.3) + 1.8364 * logBhAge - 1.4018 * logBhAge * logBhAge
							+ 0.4374 * llog(height - 1.3) / bhage;
					break;
				case SI_FDI_VDP_MONT:
					index = Utils.computeInFeet(
							height,
							heightFt -> 4.5 + 111.832 + 0.721 * (heightFt - 4.5) - 28.2175 * Math.log(bhage)
									- 731.551 / (bhage * bhage) + 13.164 * (heightFt - 4.5) / bhage
					);
					break;
				case SI_FDI_VDP_WASH:
					index = Utils.computeInFeet(
							height,
							heightFt -> 4.5 + 146.274 + 0.809 * (heightFt - 4.5) - 37.218 * Math.log(bhage)
									- 1064.4055 / (bhage * bhage) + 9.511 * (heightFt - 4.5) / bhage
					);
					break;
				case SI_FDI_MONS_DF:
					x1 = 0.4948;
					x2 = 25.315;

					logBhAge = Math.log(bhage);

					index = Utils.computeInFeet(
							height,
							heightFt -> 4.5 + 38.787 - 2.805 * logBhAge * logBhAge + 0.0216 * bhage * logBhAge
									+ x1 * heightFt + x2 * heightFt / bhage
					);
					break;
				case SI_FDI_MONS_GF, SI_FDI_MONS_WRC:
					x1 = 0.4305;
					x2 = 28.415;

					index = findSiteIndex1(bhage, height, x1, x2);
					break;
				case SI_FDI_MONS_WH, SI_FDI_MONS_SAF:
					x1 = 0.3964;
					x2 = 30.008;

					index = findSiteIndex1(bhage, height, x1, x2);
					break;
				case SI_FDI_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 4.114;
						x2 = 0.4540;
						break;
					case 2:
						x1 = 3.312;
						x2 = 0.5139;
						break;
					case 3:
						x1 = 2.365;
						x2 = 0.6037;
						break;
					case 4:
						x1 = 1.830;
						x2 = 0.6683;
						break;
					case 5:
						x1 = 1.589;
						x2 = 0.7005;
						break;
					case 6:
						x1 = 1.461;
						x2 = 0.7186;
						break;
					case 7:
						x1 = 1.327;
						x2 = 0.7395;
						break;
					case 8:
						x1 = 1.237;
						x2 = 0.7545;
						break;
					case 9:
						x1 = 1.203;
						x2 = 0.7575;
						break;
					case 10:
						x1 = 1.127;
						x2 = 0.7717;
						break;
					case 11:
						x1 = 1.071;
						x2 = 0.7819;
						break;
					case 12:
						x1 = 0.9716;
						x2 = 0.8049;
						break;
					case 13:
						x1 = 0.9143;
						x2 = 0.8188;
						break;
					case 14:
						x1 = 0.8701;
						x2 = 0.8300;
						break;
					case 15:
						x1 = 0.8495;
						x2 = 0.8347;
						break;
					case 16:
						x1 = 0.8215;
						x2 = 0.8419;
						break;
					case 17:
						x1 = 0.8013;
						x2 = 0.8472;
						break;
					case 18:
						x1 = 0.7880;
						x2 = 0.8508;
						break;
					case 19:
						x1 = 0.7722;
						x2 = 0.8553;
						break;
					case 20:
						x1 = 0.7532;
						x2 = 0.8613;
						break;
					case 21:
						x1 = 0.7274;
						x2 = 0.8703;
						break;
					case 22:
						x1 = 0.7204;
						x2 = 0.8728;
						break;
					case 23:
						x1 = 0.6862;
						x2 = 0.8858;
						break;
					case 24:
						x1 = 0.6790;
						x2 = 0.8886;
						break;
					case 25:
						x1 = 0.6583;
						x2 = 0.8972;
						break;
					case 26:
						x1 = 0.6355;
						x2 = 0.9066;
						break;
					case 27:
						x1 = 0.6273;
						x2 = 0.9105;
						break;
					case 28:
						x1 = 0.6182;
						x2 = 0.9148;
						break;
					case 29:
						x1 = 0.6067;
						x2 = 0.9204;
						break;
					case 30:
						x1 = 0.5957;
						x2 = 0.9261;
						break;
					case 31:
						x1 = 0.5826;
						x2 = 0.9326;
						break;
					case 32:
						x1 = 0.5714;
						x2 = 0.9385;
						break;
					case 33:
						x1 = 0.5665;
						x2 = 0.9417;
						break;
					case 34:
						x1 = 0.5509;
						x2 = 0.9503;
						break;
					case 35:
						x1 = 0.5422;
						x2 = 0.9556;
						break;
					case 36:
						x1 = 0.5342;
						x2 = 0.9607;
						break;
					case 37:
						x1 = 0.5290;
						x2 = 0.9646;
						break;
					case 38:
						x1 = 0.5225;
						x2 = 0.9691;
						break;
					case 39:
						x1 = 0.5179;
						x2 = 0.9728;
						break;
					case 40:
						x1 = 0.5061;
						x2 = 0.9802;
						break;
					case 41:
						x1 = 0.5003;
						x2 = 0.9847;
						break;
					case 42:
						x1 = 0.4957;
						x2 = 0.9887;
						break;
					case 43:
						x1 = 0.4936;
						x2 = 0.9912;
						break;
					case 44:
						x1 = 0.4931;
						x2 = 0.9930;
						break;
					case 45:
						x1 = 0.4927;
						x2 = 0.9946;
						break;
					case 46:
						x1 = 0.4875;
						x2 = 0.9988;
						break;
					case 47:
						x1 = 0.4866;
						x2 = 1.001;
						break;
					case 48:
						x1 = 0.4857;
						x2 = 1.002;
						break;
					case 49:
						x1 = 0.4899;
						x2 = 1.002;
						break;
					case 50:
						x1 = 0.4950;
						x2 = 1.000;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_PLI_NIGHGI97:
					switch ((int) bhage) {
					case 1:
						x1 = 3.229;
						x2 = 0.4774;
						break;
					case 2:
						x1 = 2.726;
						x2 = 0.5081;
						break;
					case 3:
						x1 = 2.671;
						x2 = 0.5095;
						break;
					case 4:
						x1 = 2.472;
						x2 = 0.5272;
						break;
					case 5:
						x1 = 2.353;
						x2 = 0.5376;
						break;
					case 6:
						x1 = 2.369;
						x2 = 0.5340;
						break;
					case 7:
						x1 = 2.287;
						x2 = 0.5419;
						break;
					case 8:
						x1 = 2.130;
						x2 = 0.5598;
						break;
					case 9:
						x1 = 2.022;
						x2 = 0.5736;
						break;
					case 10:
						x1 = 1.923;
						x2 = 0.5865;
						break;
					case 11:
						x1 = 1.797;
						x2 = 0.6042;
						break;
					case 12:
						x1 = 1.724;
						x2 = 0.6154;
						break;
					case 13:
						x1 = 1.663;
						x2 = 0.6253;
						break;
					case 14:
						x1 = 1.582;
						x2 = 0.6390;
						break;
					case 15:
						x1 = 1.530;
						x2 = 0.6485;
						break;
					case 16:
						x1 = 1.466;
						x2 = 0.6602;
						break;
					case 17:
						x1 = 1.393;
						x2 = 0.6744;
						break;
					case 18:
						x1 = 1.327;
						x2 = 0.6881;
						break;
					case 19:
						x1 = 1.271;
						x2 = 0.6998;
						break;
					case 20:
						x1 = 1.216;
						x2 = 0.7123;
						break;
					case 21:
						x1 = 1.167;
						x2 = 0.7240;
						break;
					case 22:
						x1 = 1.122;
						x2 = 0.7355;
						break;
					case 23:
						x1 = 1.079;
						x2 = 0.7469;
						break;
					case 24:
						x1 = 1.045;
						x2 = 0.7567;
						break;
					case 25:
						x1 = 1.002;
						x2 = 0.7687;
						break;
					case 26:
						x1 = 0.9590;
						x2 = 0.7817;
						break;
					case 27:
						x1 = 0.9167;
						x2 = 0.7950;
						break;
					case 28:
						x1 = 0.8712;
						x2 = 0.8099;
						break;
					case 29:
						x1 = 0.8356;
						x2 = 0.8226;
						break;
					case 30:
						x1 = 0.8005;
						x2 = 0.8354;
						break;
					case 31:
						x1 = 0.7801;
						x2 = 0.8437;
						break;
					case 32:
						x1 = 0.7557;
						x2 = 0.8536;
						break;
					case 33:
						x1 = 0.7238;
						x2 = 0.8666;
						break;
					case 34:
						x1 = 0.7019;
						x2 = 0.8764;
						break;
					case 35:
						x1 = 0.6859;
						x2 = 0.8842;
						break;
					case 36:
						x1 = 0.6667;
						x2 = 0.8935;
						break;
					case 37:
						x1 = 0.6467;
						x2 = 0.9033;
						break;
					case 38:
						x1 = 0.6289;
						x2 = 0.9125;
						break;
					case 39:
						x1 = 0.6147;
						x2 = 0.9205;
						break;
					case 40:
						x1 = 0.6009;
						x2 = 0.9283;
						break;
					case 41:
						x1 = 0.5852;
						x2 = 0.9373;
						break;
					case 42:
						x1 = 0.5731;
						x2 = 0.9448;
						break;
					case 43:
						x1 = 0.5592;
						x2 = 0.9534;
						break;
					case 44:
						x1 = 0.5455;
						x2 = 0.9621;
						break;
					case 45:
						x1 = 0.5350;
						x2 = 0.9693;
						break;
					case 46:
						x1 = 0.5236;
						x2 = 0.9769;
						break;
					case 47:
						x1 = 0.5152;
						x2 = 0.9833;
						break;
					case 48:
						x1 = 0.5075;
						x2 = 0.9895;
						break;
					case 49:
						x1 = 0.4986;
						x2 = 0.9963;
						break;
					case 50:
						x1 = 0.4924;
						x2 = 1.002;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				/*
				 * This section has been commented out since I cannot find the constant anywhere and this was surronded
				 * by an ifdef statement I have thus assumed this code should not trigger
				 */
				/*
				 * case SI_PLI_NIGHGI: //later, we divide by age-0.5, so check it now
				 */
				/*
				 * if (bhage < 0.5) { return SI_ERR_GI_MIN; }
				 *
				 * switch ((int) bhage) { case 1: x1 = 3.791; x2 = 0.4338; break; case 2: x1 = 3.460; x2 = 0.4592;
				 * break; case 3: x1 = 3.440; x2 = 0.4615; break; case 4: x1 = 3.083; x2 = 0.4898; break; case 5: x1 =
				 * 2.902; x2 = 0.5054; break; case 6: x1 = 2.962; x2 = 0.4989; break; case 7: x1 = 2.802; x2 = 0.5127;
				 * break; case 8: x1 = 2.663; x2 = 0.5245; break; case 9: x1 = 2.608; x2 = 0.5288; break; case 10: x1 =
				 * 2.534; x2 = 0.5357; break; case 11: x1 = 2.438; x2 = 0.5455; break; case 12: x1 = 2.338; x2 = 0.5563;
				 * break; case 13: x1 = 2.232; x2 = 0.5686; break; case 14: x1 = 2.133; x2 = 0.5808; break; case 15: x1
				 * = 2.053; x2 = 0.5912; break; case 16: x1 = 2.093; x2 = 0.5861; break; case 17: x1 = 1.973; x2 =
				 * 0.6020; break; case 18: x1 = 1.867; x2 = 0.6170; break; case 19: x1 = 1.769; x2 = 0.6317; break; case
				 * 20: x1 = 1.675; x2 = 0.6467; break; case 21: x1 = 1.597; x2 = 0.6601; break; case 22: x1 = 1.522; x2
				 * = 0.6738; break; case 23: x1 = 1.460; x2 = 0.6859; break; case 24: x1 = 1.407; x2 = 0.6968; break;
				 * case 25: x1 = 1.353; x2 = 0.7083; break; case 26: x1 = 1.299; x2 = 0.7203; break; case 27: x1 =
				 * 1.242; x2 = 0.7334; break; case 28: x1 = 1.195; x2 = 0.7450; break; case 29: x1 = 1.152; x2 = 0.7560;
				 * break; case 30: x1 = 1.106; x2 = 0.7682; break; default: x1 = 0; x2 = 0; break; break; } if (x1 == 0)
				 * { index = SI_ERR_GI_MAX; } else { index = (height - 1.3) * 100 / (bhage - 0.5); index = x1 *
				 * ppow(index, x2); } break;
				 */
				case SI_SW_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 7.867;
						x2 = 0.3516;
						break;
					case 2:
						x1 = 8.125;
						x2 = 0.3437;
						break;
					case 3:
						x1 = 8.155;
						x2 = 0.3448;
						break;
					case 4:
						x1 = 8.197;
						x2 = 0.3431;
						break;
					case 5:
						x1 = 8.270;
						x2 = 0.3369;
						break;
					case 6:
						x1 = 7.917;
						x2 = 0.3413;
						break;
					case 7:
						x1 = 7.414;
						x2 = 0.3496;
						break;
					case 8:
						x1 = 7.022;
						x2 = 0.3557;
						break;
					case 9:
						x1 = 6.700;
						x2 = 0.3599;
						break;
					case 10:
						x1 = 6.427;
						x2 = 0.3626;
						break;
					case 11:
						x1 = 6.125;
						x2 = 0.3664;
						break;
					case 12:
						x1 = 5.831;
						x2 = 0.3703;
						break;
					case 13:
						x1 = 5.595;
						x2 = 0.3732;
						break;
					case 14:
						x1 = 5.369;
						x2 = 0.3759;
						break;
					case 15:
						x1 = 5.125;
						x2 = 0.3792;
						break;
					case 16:
						x1 = 4.921;
						x2 = 0.3817;
						break;
					case 17:
						x1 = 4.750;
						x2 = 0.3837;
						break;
					case 18:
						x1 = 4.512;
						x2 = 0.3878;
						break;
					case 19:
						x1 = 4.307;
						x2 = 0.3911;
						break;
					case 20:
						x1 = 4.180;
						x2 = 0.3929;
						break;
					case 21:
						x1 = 4.045;
						x2 = 0.3954;
						break;
					case 22:
						x1 = 3.909;
						x2 = 0.3981;
						break;
					case 23:
						x1 = 3.731;
						x2 = 0.4022;
						break;
					case 24:
						x1 = 3.472;
						x2 = 0.4083;
						break;
					case 25:
						x1 = 3.210;
						x2 = 0.4147;
						break;
					case 26:
						x1 = 2.984;
						x2 = 0.4203;
						break;
					case 27:
						x1 = 2.782;
						x2 = 0.4254;
						break;
					case 28:
						x1 = 2.633;
						x2 = 0.4293;
						break;
					case 29:
						x1 = 2.519;
						x2 = 0.4323;
						break;
					case 30:
						x1 = 2.434;
						x2 = 0.4349;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndexCommon(bhage, height, x1, 100, index2 -> x1 + x2 * index2);
					break;
				case SI_SW_NIGHGI99:
					switch ((int) bhage) {
					case 1:
						x1 = 4.050;
						x2 = 0.4630;
						break;
					case 2:
						x1 = 3.215;
						x2 = 0.5222;
						break;
					case 3:
						x1 = 2.917;
						x2 = 0.5509;
						break;
					case 4:
						x1 = 2.768;
						x2 = 0.5674;
						break;
					case 5:
						x1 = 2.749;
						x2 = 0.5683;
						break;
					case 6:
						x1 = 2.724;
						x2 = 0.5671;
						break;
					case 7:
						x1 = 2.685;
						x2 = 0.5672;
						break;
					case 8:
						x1 = 2.646;
						x2 = 0.5675;
						break;
					case 9:
						x1 = 2.572;
						x2 = 0.5724;
						break;
					case 10:
						x1 = 2.503;
						x2 = 0.5772;
						break;
					case 11:
						x1 = 2.469;
						x2 = 0.5780;
						break;
					case 12:
						x1 = 2.419;
						x2 = 0.5812;
						break;
					case 13:
						x1 = 2.334;
						x2 = 0.5892;
						break;
					case 14:
						x1 = 2.259;
						x2 = 0.5963;
						break;
					case 15:
						x1 = 2.178;
						x2 = 0.6044;
						break;
					case 16:
						x1 = 2.079;
						x2 = 0.6153;
						break;
					case 17:
						x1 = 1.965;
						x2 = 0.6294;
						break;
					case 18:
						x1 = 1.857;
						x2 = 0.6442;
						break;
					case 19:
						x1 = 1.772;
						x2 = 0.6562;
						break;
					case 20:
						x1 = 1.673;
						x2 = 0.6711;
						break;
					case 21:
						x1 = 1.578;
						x2 = 0.6861;
						break;
					case 22:
						x1 = 1.486;
						x2 = 0.7016;
						break;
					case 23:
						x1 = 1.394;
						x2 = 0.7186;
						break;
					case 24:
						x1 = 1.301;
						x2 = 0.7371;
						break;
					case 25:
						x1 = 1.215;
						x2 = 0.7551;
						break;
					case 26:
						x1 = 1.139;
						x2 = 0.7723;
						break;
					case 27:
						x1 = 1.069;
						x2 = 0.7891;
						break;
					case 28:
						x1 = 1.008;
						x2 = 0.8045;
						break;
					case 29:
						x1 = 0.9554;
						x2 = 0.8188;
						break;
					case 30:
						x1 = 0.9067;
						x2 = 0.8327;
						break;
					case 31:
						x1 = 0.8666;
						x2 = 0.8448;
						break;
					case 32:
						x1 = 0.8366;
						x2 = 0.8541;
						break;
					case 33:
						x1 = 0.8074;
						x2 = 0.8636;
						break;
					case 34:
						x1 = 0.7745;
						x2 = 0.8749;
						break;
					case 35:
						x1 = 0.7386;
						x2 = 0.8877;
						break;
					case 36:
						x1 = 0.7095;
						x2 = 0.8985;
						break;
					case 37:
						x1 = 0.6861;
						x2 = 0.9075;
						break;
					case 38:
						x1 = 0.6651;
						x2 = 0.9159;
						break;
					case 39:
						x1 = 0.6409;
						x2 = 0.9261;
						break;
					case 40:
						x1 = 0.6157;
						x2 = 0.9372;
						break;
					case 41:
						x1 = 0.5945;
						x2 = 0.9469;
						break;
					case 42:
						x1 = 0.5721;
						x2 = 0.9575;
						break;
					case 43:
						x1 = 0.5508;
						x2 = 0.9681;
						break;
					case 44:
						x1 = 0.5347;
						x2 = 0.9765;
						break;
					case 45:
						x1 = 0.5208;
						x2 = 0.9841;
						break;
					case 46:
						x1 = 0.5101;
						x2 = 0.9902;
						break;
					case 47:
						x1 = 0.5047;
						x2 = 0.9935;
						break;
					case 48:
						x1 = 0.5006;
						x2 = 0.9963;
						break;
					case 49:
						x1 = 0.4970;
						x2 = 0.9988;
						break;
					case 50:
						x1 = 0.4934;
						x2 = 1.001;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_SW_NIGHGI2004:
					switch ((int) bhage) {
					case 1:
						x1 = 4.7650;
						x2 = 0.4102;
						break;
					case 2:
						x1 = 4.2040;
						x2 = 0.4406;
						break;
					case 3:
						x1 = 3.8680;
						x2 = 0.4639;
						break;
					case 4:
						x1 = 3.5580;
						x2 = 0.4900;
						break;
					case 5:
						x1 = 3.3280;
						x2 = 0.5093;
						break;
					case 6:
						x1 = 3.2480;
						x2 = 0.5137;
						break;
					case 7:
						x1 = 3.0380;
						x2 = 0.5305;
						break;
					case 8:
						x1 = 2.9050;
						x2 = 0.5403;
						break;
					case 9:
						x1 = 2.8040;
						x2 = 0.5476;
						break;
					case 10:
						x1 = 2.7250;
						x2 = 0.5532;
						break;
					case 11:
						x1 = 2.7000;
						x2 = 0.5528;
						break;
					case 12:
						x1 = 2.6150;
						x2 = 0.5592;
						break;
					case 13:
						x1 = 2.5070;
						x2 = 0.5687;
						break;
					case 14:
						x1 = 2.4340;
						x2 = 0.5749;
						break;
					case 15:
						x1 = 2.3610;
						x2 = 0.5813;
						break;
					case 16:
						x1 = 2.2510;
						x2 = 0.5930;
						break;
					case 17:
						x1 = 2.1430;
						x2 = 0.6051;
						break;
					case 18:
						x1 = 2.0320;
						x2 = 0.6188;
						break;
					case 19:
						x1 = 1.9210;
						x2 = 0.6333;
						break;
					case 20:
						x1 = 1.8280;
						x2 = 0.6460;
						break;
					case 21:
						x1 = 1.7240;
						x2 = 0.6613;
						break;
					case 22:
						x1 = 1.6180;
						x2 = 0.6777;
						break;
					case 23:
						x1 = 1.5080;
						x2 = 0.6966;
						break;
					case 24:
						x1 = 1.3910;
						x2 = 0.7183;
						break;
					case 25:
						x1 = 1.3080;
						x2 = 0.7346;
						break;
					case 26:
						x1 = 1.2290;
						x2 = 0.7510;
						break;
					case 27:
						x1 = 1.1600;
						x2 = 0.7659;
						break;
					case 28:
						x1 = 1.1050;
						x2 = 0.7787;
						break;
					case 29:
						x1 = 1.0520;
						x2 = 0.7919;
						break;
					case 30:
						x1 = 0.9917;
						x2 = 0.8076;
						break;
					case 31:
						x1 = 0.9453;
						x2 = 0.8203;
						break;
					case 32:
						x1 = 0.9035;
						x2 = 0.8324;
						break;
					case 33:
						x1 = 0.8589;
						x2 = 0.8460;
						break;
					case 34:
						x1 = 0.8206;
						x2 = 0.8584;
						break;
					case 35:
						x1 = 0.7821;
						x2 = 0.8715;
						break;
					case 36:
						x1 = 0.7510;
						x2 = 0.8825;
						break;
					case 37:
						x1 = 0.7181;
						x2 = 0.8946;
						break;
					case 38:
						x1 = 0.6966;
						x2 = 0.9030;
						break;
					case 39:
						x1 = 0.6729;
						x2 = 0.9125;
						break;
					case 40:
						x1 = 0.6440;
						x2 = 0.9245;
						break;
					case 41:
						x1 = 0.6232;
						x2 = 0.9335;
						break;
					case 42:
						x1 = 0.6017;
						x2 = 0.9433;
						break;
					case 43:
						x1 = 0.5757;
						x2 = 0.9557;
						break;
					case 44:
						x1 = 0.5527;
						x2 = 0.9672;
						break;
					case 45:
						x1 = 0.5337;
						x2 = 0.9772;
						break;
					case 46:
						x1 = 0.5238;
						x2 = 0.9827;
						break;
					case 47:
						x1 = 0.5169;
						x2 = 0.9868;
						break;
					case 48:
						x1 = 0.5078;
						x2 = 0.9921;
						break;
					case 49:
						x1 = 0.5006;
						x2 = 0.9967;
						break;
					case 50:
						x1 = 0.4941;
						x2 = 1.0010;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_HWC_NIGHGI99:
					switch ((int) bhage) {
					case 1:
						x1 = 4.361;
						x2 = 0.4638;
						break;
					case 2:
						x1 = 3.678;
						x2 = 0.5047;
						break;
					case 3:
						x1 = 3.359;
						x2 = 0.5302;
						break;
					case 4:
						x1 = 3.221;
						x2 = 0.5432;
						break;
					case 5:
						x1 = 2.857;
						x2 = 0.5749;
						break;
					case 6:
						x1 = 2.459;
						x2 = 0.6134;
						break;
					case 7:
						x1 = 2.229;
						x2 = 0.6373;
						break;
					case 8:
						x1 = 2.149;
						x2 = 0.6436;
						break;
					case 9:
						x1 = 2.026;
						x2 = 0.6550;
						break;
					case 10:
						x1 = 1.842;
						x2 = 0.6756;
						break;
					case 11:
						x1 = 1.692;
						x2 = 0.6937;
						break;
					case 12:
						x1 = 1.547;
						x2 = 0.7120;
						break;
					case 13:
						x1 = 1.440;
						x2 = 0.7267;
						break;
					case 14:
						x1 = 1.392;
						x2 = 0.7337;
						break;
					case 15:
						x1 = 1.360;
						x2 = 0.7383;
						break;
					case 16:
						x1 = 1.333;
						x2 = 0.7422;
						break;
					case 17:
						x1 = 1.294;
						x2 = 0.7487;
						break;
					case 18:
						x1 = 1.254;
						x2 = 0.7554;
						break;
					case 19:
						x1 = 1.211;
						x2 = 0.7625;
						break;
					case 20:
						x1 = 1.162;
						x2 = 0.7716;
						break;
					case 21:
						x1 = 1.125;
						x2 = 0.7787;
						break;
					case 22:
						x1 = 1.086;
						x2 = 0.7864;
						break;
					case 23:
						x1 = 1.049;
						x2 = 0.7942;
						break;
					case 24:
						x1 = 1.026;
						x2 = 0.7991;
						break;
					case 25:
						x1 = 0.9952;
						x2 = 0.8064;
						break;
					case 26:
						x1 = 0.9440;
						x2 = 0.8196;
						break;
					case 27:
						x1 = 0.8852;
						x2 = 0.8356;
						break;
					case 28:
						x1 = 0.8406;
						x2 = 0.8485;
						break;
					case 29:
						x1 = 0.7961;
						x2 = 0.8621;
						break;
					case 30:
						x1 = 0.7478;
						x2 = 0.8781;
						break;
					case 31:
						x1 = 0.7026;
						x2 = 0.8939;
						break;
					case 32:
						x1 = 0.6607;
						x2 = 0.9095;
						break;
					case 33:
						x1 = 0.6217;
						x2 = 0.9251;
						break;
					case 34:
						x1 = 0.5906;
						x2 = 0.9387;
						break;
					case 35:
						x1 = 0.5699;
						x2 = 0.9488;
						break;
					case 36:
						x1 = 0.5538;
						x2 = 0.9571;
						break;
					case 37:
						x1 = 0.5415;
						x2 = 0.9640;
						break;
					case 38:
						x1 = 0.5320;
						x2 = 0.9696;
						break;
					case 39:
						x1 = 0.5231;
						x2 = 0.9750;
						break;
					case 40:
						x1 = 0.5139;
						x2 = 0.9805;
						break;
					case 41:
						x1 = 0.5073;
						x2 = 0.9847;
						break;
					case 42:
						x1 = 0.5038;
						x2 = 0.9875;
						break;
					case 43:
						x1 = 0.5017;
						x2 = 0.9896;
						break;
					case 44:
						x1 = 0.5001;
						x2 = 0.9914;
						break;
					case 45:
						x1 = 0.4991;
						x2 = 0.9929;
						break;
					case 46:
						x1 = 0.4989;
						x2 = 0.9940;
						break;
					case 47:
						x1 = 0.4987;
						x2 = 0.9951;
						break;
					case 48:
						x1 = 0.4963;
						x2 = 0.9973;
						break;
					case 49:
						x1 = 0.4939;
						x2 = 0.9997;
						break;
					case 50:
						x1 = 0.4914;
						x2 = 1.002;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_HWC_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 4.957;
						x2 = 0.4325;
						break;
					case 2:
						x1 = 4.413;
						x2 = 0.4649;
						break;
					case 3:
						x1 = 4.002;
						x2 = 0.4939;
						break;
					case 4:
						x1 = 3.812;
						x2 = 0.5095;
						break;
					case 5:
						x1 = 3.386;
						x2 = 0.5412;
						break;
					case 6:
						x1 = 2.932;
						x2 = 0.5786;
						break;
					case 7:
						x1 = 2.680;
						x2 = 0.6006;
						break;
					case 8:
						x1 = 2.595;
						x2 = 0.6062;
						break;
					case 9:
						x1 = 2.462;
						x2 = 0.6165;
						break;
					case 10:
						x1 = 2.247;
						x2 = 0.6365;
						break;
					case 11:
						x1 = 2.066;
						x2 = 0.6546;
						break;
					case 12:
						x1 = 1.905;
						x2 = 0.6712;
						break;
					case 13:
						x1 = 1.786;
						x2 = 0.6843;
						break;
					case 14:
						x1 = 1.728;
						x2 = 0.6912;
						break;
					case 15:
						x1 = 1.693;
						x2 = 0.6954;
						break;
					case 16:
						x1 = 1.665;
						x2 = 0.6986;
						break;
					case 17:
						x1 = 1.620;
						x2 = 0.7046;
						break;
					case 18:
						x1 = 1.575;
						x2 = 0.7105;
						break;
					case 19:
						x1 = 1.524;
						x2 = 0.7172;
						break;
					case 20:
						x1 = 1.465;
						x2 = 0.7259;
						break;
					case 21:
						x1 = 1.424;
						x2 = 0.7321;
						break;
					case 22:
						x1 = 1.381;
						x2 = 0.7388;
						break;
					case 23:
						x1 = 1.337;
						x2 = 0.7461;
						break;
					case 24:
						x1 = 1.309;
						x2 = 0.7508;
						break;
					case 25:
						x1 = 1.274;
						x2 = 0.7574;
						break;
					case 26:
						x1 = 1.215;
						x2 = 0.7693;
						break;
					case 27:
						x1 = 1.145;
						x2 = 0.7839;
						break;
					case 28:
						x1 = 1.091;
						x2 = 0.7960;
						break;
					case 29:
						x1 = 1.038;
						x2 = 0.8085;
						break;
					case 30:
						x1 = 0.9789;
						x2 = 0.8235;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;

					}
					index = findeSiteIndex3(bhage, height, x1, x2);
					break;
				case SI_HWI_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 4.309;
						x2 = 0.4131;
						break;
					case 2:
						x1 = 4.535;
						x2 = 0.3795;
						break;
					case 3:
						x1 = 4.337;
						x2 = 0.3904;
						break;
					case 4:
						x1 = 3.804;
						x2 = 0.4314;
						break;
					case 5:
						x1 = 3.362;
						x2 = 0.4708;
						break;
					case 6:
						x1 = 3.079;
						x2 = 0.4985;
						break;
					case 7:
						x1 = 2.995;
						x2 = 0.5060;
						break;
					case 8:
						x1 = 3.082;
						x2 = 0.4955;
						break;
					case 9:
						x1 = 3.063;
						x2 = 0.4959;
						break;
					case 10:
						x1 = 2.920;
						x2 = 0.5088;
						break;
					case 11:
						x1 = 2.698;
						x2 = 0.5311;
						break;
					case 12:
						x1 = 2.419;
						x2 = 0.5623;
						break;
					case 13:
						x1 = 2.182;
						x2 = 0.5913;
						break;
					case 14:
						x1 = 2.000;
						x2 = 0.6154;
						break;
					case 15:
						x1 = 1.815;
						x2 = 0.6420;
						break;
					case 16:
						x1 = 1.639;
						x2 = 0.6704;
						break;
					case 17:
						x1 = 1.499;
						x2 = 0.6955;
						break;
					case 18:
						x1 = 1.383;
						x2 = 0.7184;
						break;
					case 19:
						x1 = 1.297;
						x2 = 0.7363;
						break;
					case 20:
						x1 = 1.218;
						x2 = 0.7540;
						break;
					case 21:
						x1 = 1.151;
						x2 = 0.7693;
						break;
					case 22:
						x1 = 1.101;
						x2 = 0.7809;
						break;
					case 23:
						x1 = 1.054;
						x2 = 0.7920;
						break;
					case 24:
						x1 = 1.002;
						x2 = 0.8053;
						break;
					case 25:
						x1 = 0.9410;
						x2 = 0.8220;
						break;
					case 26:
						x1 = 0.8845;
						x2 = 0.8390;
						break;
					case 27:
						x1 = 0.8410;
						x2 = 0.8530;
						break;
					case 28:
						x1 = 0.8032;
						x2 = 0.8657;
						break;
					case 29:
						x1 = 0.7705;
						x2 = 0.8770;
						break;
					case 30:
						x1 = 0.7450;
						x2 = 0.8860;
						break;
					case 31:
						x1 = 0.7277;
						x2 = 0.8924;
						break;
					case 32:
						x1 = 0.7211;
						x2 = 0.8947;
						break;
					case 33:
						x1 = 0.7213;
						x2 = 0.8942;
						break;
					case 34:
						x1 = 0.7279;
						x2 = 0.8910;
						break;
					case 35:
						x1 = 0.7328;
						x2 = 0.8884;
						break;
					case 36:
						x1 = 0.7239;
						x2 = 0.8913;
						break;
					case 37:
						x1 = 0.7072;
						x2 = 0.8972;
						break;
					case 38:
						x1 = 0.6849;
						x2 = 0.9059;
						break;
					case 39:
						x1 = 0.6658;
						x2 = 0.9138;
						break;
					case 40:
						x1 = 0.6478;
						x2 = 0.9218;
						break;
					case 41:
						x1 = 0.6292;
						x2 = 0.9303;
						break;
					case 42:
						x1 = 0.6124;
						x2 = 0.9382;
						break;
					case 43:
						x1 = 0.5916;
						x2 = 0.9484;
						break;
					case 44:
						x1 = 0.5687;
						x2 = 0.9600;
						break;
					case 45:
						x1 = 0.5456;
						x2 = 0.9720;
						break;
					case 46:
						x1 = 0.5267;
						x2 = 0.9819;
						break;
					case 47:
						x1 = 0.5142;
						x2 = 0.9887;
						break;
					case 48:
						x1 = 0.5063;
						x2 = 0.9933;
						break;
					case 49:
						x1 = 0.5003;
						x2 = 0.9970;
						break;
					case 50:
						x1 = 0.4935;
						x2 = 1.002;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;

					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_FDC_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 3.894;
						x2 = 0.5382;
						break;
					case 2:
						x1 = 2.546;
						x2 = 0.6330;
						break;
					case 3:
						x1 = 2.449;
						x2 = 0.6328;
						break;
					case 4:
						x1 = 2.346;
						x2 = 0.6358;
						break;
					case 5:
						x1 = 2.187;
						x2 = 0.6474;
						break;
					case 6:
						x1 = 2.033;
						x2 = 0.6593;
						break;
					case 7:
						x1 = 1.768;
						x2 = 0.6882;
						break;
					case 8:
						x1 = 1.599;
						x2 = 0.7076;
						break;
					case 9:
						x1 = 1.437;
						x2 = 0.7296;
						break;
					case 10:
						x1 = 1.266;
						x2 = 0.7570;
						break;
					case 11:
						x1 = 1.155;
						x2 = 0.7760;
						break;
					case 12:
						x1 = 1.043;
						x2 = 0.7981;
						break;
					case 13:
						x1 = 0.9722;
						x2 = 0.8135;
						break;
					case 14:
						x1 = 0.8972;
						x2 = 0.8310;
						break;
					case 15:
						x1 = 0.8812;
						x2 = 0.8343;
						break;
					case 16:
						x1 = 0.8368;
						x2 = 0.8457;
						break;
					case 17:
						x1 = 0.7872;
						x2 = 0.8595;
						break;
					case 18:
						x1 = 0.7554;
						x2 = 0.8690;
						break;
					case 19:
						x1 = 0.7370;
						x2 = 0.8747;
						break;
					case 20:
						x1 = 0.7165;
						x2 = 0.8819;
						break;
					case 21:
						x1 = 0.7007;
						x2 = 0.8872;
						break;
					case 22:
						x1 = 0.6814;
						x2 = 0.8944;
						break;
					case 23:
						x1 = 0.6810;
						x2 = 0.8950;
						break;
					case 24:
						x1 = 0.6736;
						x2 = 0.8982;
						break;
					case 25:
						x1 = 0.6702;
						x2 = 0.9003;
						break;
					case 26:
						x1 = 0.6579;
						x2 = 0.9055;
						break;
					case 27:
						x1 = 0.6585;
						x2 = 0.9062;
						break;
					case 28:
						x1 = 0.6414;
						x2 = 0.9131;
						break;
					case 29:
						x1 = 0.6236;
						x2 = 0.9204;
						break;
					case 30:
						x1 = 0.6177;
						x2 = 0.9235;
						break;
					case 31:
						x1 = 0.6159;
						x2 = 0.9252;
						break;
					case 32:
						x1 = 0.6032;
						x2 = 0.9314;
						break;
					case 33:
						x1 = 0.5913;
						x2 = 0.9372;
						break;
					case 34:
						x1 = 0.5797;
						x2 = 0.9428;
						break;
					case 35:
						x1 = 0.5635;
						x2 = 0.9506;
						break;
					case 36:
						x1 = 0.5637;
						x2 = 0.9516;
						break;
					case 37:
						x1 = 0.5504;
						x2 = 0.9584;
						break;
					case 38:
						x1 = 0.5455;
						x2 = 0.9615;
						break;
					case 39:
						x1 = 0.5356;
						x2 = 0.9670;
						break;
					case 40:
						x1 = 0.5289;
						x2 = 0.9711;
						break;
					case 41:
						x1 = 0.5182;
						x2 = 0.9772;
						break;
					case 42:
						x1 = 0.5138;
						x2 = 0.9803;
						break;
					case 43:
						x1 = 0.5107;
						x2 = 0.9830;
						break;
					case 44:
						x1 = 0.5035;
						x2 = 0.9877;
						break;
					case 45:
						x1 = 0.4992;
						x2 = 0.9910;
						break;
					case 46:
						x1 = 0.4896;
						x2 = 0.9972;
						break;
					case 47:
						x1 = 0.4844;
						x2 = 1.001;
						break;
					case 48:
						x1 = 0.4861;
						x2 = 1.002;
						break;
					case 49:
						x1 = 0.4837;
						x2 = 1.004;
						break;
					case 50:
						x1 = 0.4889;
						x2 = 1.003;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_SE_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 15.0367;
						x2 = 0.1597;
						break;
					case 2:
						x1 = 22.9003;
						x2 = 0.3805;
						break;
					case 3:
						x1 = 25.4585;
						x2 = 0.4283;
						break;
					case 4:
						x1 = 27.1115;
						x2 = 0.4582;
						break;
					case 5:
						x1 = 30.2259;
						x2 = 0.5148;
						break;
					case 6:
						x1 = 31.7092;
						x2 = 0.5458;
						break;
					case 7:
						x1 = 33.5095;
						x2 = 0.5802;
						break;
					case 8:
						x1 = 35.2571;
						x2 = 0.6081;
						break;
					case 9:
						x1 = 35.0516;
						x2 = 0.6107;
						break;
					case 10:
						x1 = 35.0350;
						x2 = 0.6172;
						break;
					case 11:
						x1 = 35.8094;
						x2 = 0.6353;
						break;
					case 12:
						x1 = 35.4614;
						x2 = 0.6358;
						break;
					case 13:
						x1 = 37.6992;
						x2 = 0.6777;
						break;
					case 14:
						x1 = 38.0211;
						x2 = 0.6900;
						break;
					case 15:
						x1 = 38.2442;
						x2 = 0.7024;
						break;
					case 16:
						x1 = 38.3263;
						x2 = 0.7109;
						break;
					case 17:
						x1 = 38.1493;
						x2 = 0.7155;
						break;
					case 18:
						x1 = 38.4994;
						x2 = 0.7260;
						break;
					case 19:
						x1 = 38.8501;
						x2 = 0.7368;
						break;
					case 20:
						x1 = 38.7709;
						x2 = 0.7419;
						break;
					case 21:
						x1 = 38.5404;
						x2 = 0.7452;
						break;
					case 22:
						x1 = 38.7846;
						x2 = 0.7532;
						break;
					case 23:
						x1 = 38.8850;
						x2 = 0.7587;
						break;
					case 24:
						x1 = 39.0912;
						x2 = 0.7665;
						break;
					case 25:
						x1 = 39.2344;
						x2 = 0.7743;
						break;
					case 26:
						x1 = 39.5050;
						x2 = 0.7843;
						break;
					case 27:
						x1 = 39.5257;
						x2 = 0.7891;
						break;
					case 28:
						x1 = 39.3090;
						x2 = 0.7917;
						break;
					case 29:
						x1 = 39.4347;
						x2 = 0.7980;
						break;
					case 30:
						x1 = 39.6710;
						x2 = 0.8055;
						break;
					case 31:
						x1 = 39.6369;
						x2 = 0.8079;
						break;
					case 32:
						x1 = 39.5534;
						x2 = 0.8093;
						break;
					case 33:
						x1 = 39.9131;
						x2 = 0.8173;
						break;
					case 34:
						x1 = 40.1806;
						x2 = 0.8245;
						break;
					case 35:
						x1 = 40.5841;
						x2 = 0.8343;
						break;
					case 36:
						x1 = 41.3329;
						x2 = 0.8501;
						break;
					case 37:
						x1 = 42.1175;
						x2 = 0.8658;
						break;
					case 38:
						x1 = 42.9714;
						x2 = 0.8811;
						break;
					case 39:
						x1 = 44.0075;
						x2 = 0.8998;
						break;
					case 40:
						x1 = 45.1454;
						x2 = 0.9185;
						break;
					case 41:
						x1 = 45.5908;
						x2 = 0.9275;
						break;
					case 42:
						x1 = 46.0850;
						x2 = 0.9367;
						break;
					case 43:
						x1 = 46.3976;
						x2 = 0.9435;
						break;
					case 44:
						x1 = 46.8023;
						x2 = 0.9517;
						break;
					case 45:
						x1 = 47.3316;
						x2 = 0.9612;
						break;
					case 46:
						x1 = 47.7332;
						x2 = 0.9693;
						break;
					case 47:
						x1 = 48.2180;
						x2 = 0.9774;
						break;
					case 48:
						x1 = 48.7765;
						x2 = 0.9864;
						break;
					case 49:
						x1 = 49.2254;
						x2 = 0.9941;
						break;
					case 50:
						x1 = 49.5000;
						x2 = 1.0000;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndexCommon(bhage, height, x1, 1, index1 -> 1.3 + x1 * ppow(index1, x2));
					break;
				case SI_SS_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 3.317;
						x2 = 0.5634;
						break;
					case 2:
						x1 = 3.277;
						x2 = 0.5663;
						break;
					case 3:
						x1 = 3.287;
						x2 = 0.5654;
						break;
					case 4:
						x1 = 3.232;
						x2 = 0.5699;
						break;
					case 5:
						x1 = 3.164;
						x2 = 0.5756;
						break;
					case 6:
						x1 = 3.140;
						x2 = 0.5776;
						break;
					case 7:
						x1 = 3.281;
						x2 = 0.5658;
						break;
					case 8:
						x1 = 3.463;
						x2 = 0.5502;
						break;
					case 9:
						x1 = 3.540;
						x2 = 0.5421;
						break;
					case 10:
						x1 = 3.508;
						x2 = 0.5418;
						break;
					case 11:
						x1 = 3.386;
						x2 = 0.5481;
						break;
					case 12:
						x1 = 3.224;
						x2 = 0.5578;
						break;
					case 13:
						x1 = 2.982;
						x2 = 0.5746;
						break;
					case 14:
						x1 = 2.708;
						x2 = 0.5959;
						break;
					case 15:
						x1 = 2.474;
						x2 = 0.6158;
						break;
					case 16:
						x1 = 2.248;
						x2 = 0.6373;
						break;
					case 17:
						x1 = 2.056;
						x2 = 0.6574;
						break;
					case 18:
						x1 = 1.911;
						x2 = 0.6738;
						break;
					case 19:
						x1 = 1.794;
						x2 = 0.6879;
						break;
					case 20:
						x1 = 1.680;
						x2 = 0.7026;
						break;
					case 21:
						x1 = 1.568;
						x2 = 0.7182;
						break;
					case 22:
						x1 = 1.456;
						x2 = 0.7349;
						break;
					case 23:
						x1 = 1.355;
						x2 = 0.7514;
						break;
					case 24:
						x1 = 1.273;
						x2 = 0.7656;
						break;
					case 25:
						x1 = 1.220;
						x2 = 0.7753;
						break;
					case 26:
						x1 = 1.185;
						x2 = 0.7820;
						break;
					case 27:
						x1 = 1.155;
						x2 = 0.7877;
						break;
					case 28:
						x1 = 1.126;
						x2 = 0.7938;
						break;
					case 29:
						x1 = 1.089;
						x2 = 0.8020;
						break;
					case 30:
						x1 = 1.074;
						x2 = 0.8052;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;

					}
					index = findeSiteIndex3(bhage, height, x1, x2);
					break;
				case SI_SS_NIGHGI99:
					switch ((int) bhage) {
					case 1:
						x1 = 4.367;
						x2 = 0.5034;
						break;
					case 2:
						x1 = 3.164;
						x2 = 0.5731;
						break;
					case 3:
						x1 = 3.008;
						x2 = 0.5825;
						break;
					case 4:
						x1 = 2.900;
						x2 = 0.5904;
						break;
					case 5:
						x1 = 2.810;
						x2 = 0.5978;
						break;
					case 6:
						x1 = 2.771;
						x2 = 0.6009;
						break;
					case 7:
						x1 = 2.889;
						x2 = 0.5891;
						break;
					case 8:
						x1 = 3.042;
						x2 = 0.5737;
						break;
					case 9:
						x1 = 3.095;
						x2 = 0.5666;
						break;
					case 10:
						x1 = 3.049;
						x2 = 0.5674;
						break;
					case 11:
						x1 = 2.931;
						x2 = 0.5745;
						break;
					case 12:
						x1 = 2.780;
						x2 = 0.5849;
						break;
					case 13:
						x1 = 2.561;
						x2 = 0.6025;
						break;
					case 14:
						x1 = 2.312;
						x2 = 0.6250;
						break;
					case 15:
						x1 = 2.100;
						x2 = 0.6462;
						break;
					case 16:
						x1 = 1.899;
						x2 = 0.6687;
						break;
					case 17:
						x1 = 1.730;
						x2 = 0.6897;
						break;
					case 18:
						x1 = 1.603;
						x2 = 0.7068;
						break;
					case 19:
						x1 = 1.500;
						x2 = 0.7215;
						break;
					case 20:
						x1 = 1.400;
						x2 = 0.7369;
						break;
					case 21:
						x1 = 1.303;
						x2 = 0.7531;
						break;
					case 22:
						x1 = 1.206;
						x2 = 0.7705;
						break;
					case 23:
						x1 = 1.119;
						x2 = 0.7875;
						break;
					case 24:
						x1 = 1.050;
						x2 = 0.8021;
						break;
					case 25:
						x1 = 1.004;
						x2 = 0.8123;
						break;
					case 26:
						x1 = 0.9735;
						x2 = 0.8193;
						break;
					case 27:
						x1 = 0.9481;
						x2 = 0.8253;
						break;
					case 28:
						x1 = 0.9226;
						x2 = 0.8318;
						break;
					case 29:
						x1 = 0.8906;
						x2 = 0.8403;
						break;
					case 30:
						x1 = 0.8782;
						x2 = 0.8435;
						break;
					case 31:
						x1 = 0.8574;
						x2 = 0.8493;
						break;
					case 32:
						x1 = 0.8196;
						x2 = 0.8604;
						break;
					case 33:
						x1 = 0.7985;
						x2 = 0.8670;
						break;
					case 34:
						x1 = 0.7799;
						x2 = 0.8731;
						break;
					case 35:
						x1 = 0.7638;
						x2 = 0.8786;
						break;
					case 36:
						x1 = 0.7491;
						x2 = 0.8839;
						break;
					case 37:
						x1 = 0.7349;
						x2 = 0.8891;
						break;
					case 38:
						x1 = 0.7227;
						x2 = 0.8938;
						break;
					case 39:
						x1 = 0.7112;
						x2 = 0.8985;
						break;
					case 40:
						x1 = 0.6967;
						x2 = 0.9045;
						break;
					case 41:
						x1 = 0.6770;
						x2 = 0.9125;
						break;
					case 42:
						x1 = 0.6551;
						x2 = 0.9217;
						break;
					case 43:
						x1 = 0.6351;
						x2 = 0.9305;
						break;
					case 44:
						x1 = 0.6148;
						x2 = 0.9396;
						break;
					case 45:
						x1 = 0.5924;
						x2 = 0.9498;
						break;
					case 46:
						x1 = 0.5698;
						x2 = 0.9605;
						break;
					case 47:
						x1 = 0.5489;
						x2 = 0.9710;
						break;
					case 48:
						x1 = 0.5301;
						x2 = 0.9808;
						break;
					case 49:
						x1 = 0.5143;
						x2 = 0.9897;
						break;
					case 50:
						x1 = 0.4986;
						x2 = 0.9987;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_CWI_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 3.744;
						x2 = 0.4769;
						break;
					case 2:
						x1 = 4.123;
						x2 = 0.4281;
						break;
					case 3:
						x1 = 4.117;
						x2 = 0.4252;
						break;
					case 4:
						x1 = 3.922;
						x2 = 0.4402;
						break;
					case 5:
						x1 = 3.882;
						x2 = 0.4432;
						break;
					case 6:
						x1 = 3.889;
						x2 = 0.4414;
						break;
					case 7:
						x1 = 3.843;
						x2 = 0.4439;
						break;
					case 8:
						x1 = 3.697;
						x2 = 0.4538;
						break;
					case 9:
						x1 = 3.609;
						x2 = 0.4585;
						break;
					case 10:
						x1 = 3.522;
						x2 = 0.4636;
						break;
					case 11:
						x1 = 3.432;
						x2 = 0.4692;
						break;
					case 12:
						x1 = 3.332;
						x2 = 0.4764;
						break;
					case 13:
						x1 = 3.229;
						x2 = 0.4841;
						break;
					case 14:
						x1 = 3.150;
						x2 = 0.4896;
						break;
					case 15:
						x1 = 3.048;
						x2 = 0.4974;
						break;
					case 16:
						x1 = 2.927;
						x2 = 0.5076;
						break;
					case 17:
						x1 = 2.784;
						x2 = 0.5206;
						break;
					case 18:
						x1 = 2.645;
						x2 = 0.5338;
						break;
					case 19:
						x1 = 2.519;
						x2 = 0.5465;
						break;
					case 20:
						x1 = 2.398;
						x2 = 0.5595;
						break;
					case 21:
						x1 = 2.278;
						x2 = 0.5732;
						break;
					case 22:
						x1 = 2.168;
						x2 = 0.5863;
						break;
					case 23:
						x1 = 2.050;
						x2 = 0.6013;
						break;
					case 24:
						x1 = 1.934;
						x2 = 0.6170;
						break;
					case 25:
						x1 = 1.825;
						x2 = 0.6328;
						break;
					case 26:
						x1 = 1.728;
						x2 = 0.6478;
						break;
					case 27:
						x1 = 1.634;
						x2 = 0.6628;
						break;
					case 28:
						x1 = 1.555;
						x2 = 0.6758;
						break;
					case 29:
						x1 = 1.493;
						x2 = 0.6865;
						break;
					case 30:
						x1 = 1.424;
						x2 = 0.6991;
						break;
					case 31:
						x1 = 1.361;
						x2 = 0.7113;
						break;
					case 32:
						x1 = 1.292;
						x2 = 0.7255;
						break;
					case 33:
						x1 = 1.221;
						x2 = 0.7409;
						break;
					case 34:
						x1 = 1.160;
						x2 = 0.7552;
						break;
					case 35:
						x1 = 1.106;
						x2 = 0.7685;
						break;
					case 36:
						x1 = 1.057;
						x2 = 0.7814;
						break;
					case 37:
						x1 = 1.006;
						x2 = 0.7952;
						break;
					case 38:
						x1 = 0.9524;
						x2 = 0.8106;
						break;
					case 39:
						x1 = 0.8992;
						x2 = 0.8270;
						break;
					case 40:
						x1 = 0.8560;
						x2 = 0.8411;
						break;
					case 41:
						x1 = 0.8145;
						x2 = 0.8554;
						break;
					case 42:
						x1 = 0.7697;
						x2 = 0.8717;
						break;
					case 43:
						x1 = 0.7251;
						x2 = 0.8891;
						break;
					case 44:
						x1 = 0.6819;
						x2 = 0.9070;
						break;
					case 45:
						x1 = 0.6399;
						x2 = 0.9254;
						break;
					case 46:
						x1 = 0.5999;
						x2 = 0.9441;
						break;
					case 47:
						x1 = 0.5644;
						x2 = 0.9617;
						break;
					case 48:
						x1 = 0.5385;
						x2 = 0.9754;
						break;
					case 49:
						x1 = 0.5168;
						x2 = 0.9877;
						break;
					case 50:
						x1 = 0.4945;
						x2 = 1.001;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_LW_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 6.347;
						x2 = 0.2855;
						break;
					case 2:
						x1 = 6.427;
						x2 = 0.2836;
						break;
					case 3:
						x1 = 5.871;
						x2 = 0.3106;
						break;
					case 4:
						x1 = 5.288;
						x2 = 0.3397;
						break;
					case 5:
						x1 = 4.885;
						x2 = 0.3617;
						break;
					case 6:
						x1 = 4.621;
						x2 = 0.3771;
						break;
					case 7:
						x1 = 4.021;
						x2 = 0.4135;
						break;
					case 8:
						x1 = 3.873;
						x2 = 0.4228;
						break;
					case 9:
						x1 = 3.673;
						x2 = 0.4359;
						break;
					case 10:
						x1 = 3.389;
						x2 = 0.4568;
						break;
					case 11:
						x1 = 3.065;
						x2 = 0.4831;
						break;
					case 12:
						x1 = 2.789;
						x2 = 0.5081;
						break;
					case 13:
						x1 = 2.510;
						x2 = 0.5361;
						break;
					case 14:
						x1 = 2.296;
						x2 = 0.5595;
						break;
					case 15:
						x1 = 2.131;
						x2 = 0.5790;
						break;
					case 16:
						x1 = 1.974;
						x2 = 0.5990;
						break;
					case 17:
						x1 = 1.828;
						x2 = 0.6195;
						break;
					case 18:
						x1 = 1.691;
						x2 = 0.6406;
						break;
					case 19:
						x1 = 1.596;
						x2 = 0.6563;
						break;
					case 20:
						x1 = 1.516;
						x2 = 0.6701;
						break;
					case 21:
						x1 = 1.438;
						x2 = 0.6842;
						break;
					case 22:
						x1 = 1.359;
						x2 = 0.6997;
						break;
					case 23:
						x1 = 1.299;
						x2 = 0.7122;
						break;
					case 24:
						x1 = 1.247;
						x2 = 0.7239;
						break;
					case 25:
						x1 = 1.194;
						x2 = 0.7360;
						break;
					case 26:
						x1 = 1.163;
						x2 = 0.7440;
						break;
					case 27:
						x1 = 1.104;
						x2 = 0.7584;
						break;
					case 28:
						x1 = 1.042;
						x2 = 0.7748;
						break;
					case 29:
						x1 = 0.9929;
						x2 = 0.7886;
						break;
					case 30:
						x1 = 0.9542;
						x2 = 0.8000;
						break;
					case 31:
						x1 = 0.9165;
						x2 = 0.8116;
						break;
					case 32:
						x1 = 0.8857;
						x2 = 0.8216;
						break;
					case 33:
						x1 = 0.8539;
						x2 = 0.8325;
						break;
					case 34:
						x1 = 0.8136;
						x2 = 0.8467;
						break;
					case 35:
						x1 = 0.7748;
						x2 = 0.8609;
						break;
					case 36:
						x1 = 0.7447;
						x2 = 0.8726;
						break;
					case 37:
						x1 = 0.7177;
						x2 = 0.8837;
						break;
					case 38:
						x1 = 0.6940;
						x2 = 0.8937;
						break;
					case 39:
						x1 = 0.6729;
						x2 = 0.9030;
						break;
					case 40:
						x1 = 0.6547;
						x2 = 0.9114;
						break;
					case 41:
						x1 = 0.6342;
						x2 = 0.9209;
						break;
					case 42:
						x1 = 0.6081;
						x2 = 0.9332;
						break;
					case 43:
						x1 = 0.5929;
						x2 = 0.9412;
						break;
					case 44:
						x1 = 0.5804;
						x2 = 0.9482;
						break;
					case 45:
						x1 = 0.5657;
						x2 = 0.9566;
						break;
					case 46:
						x1 = 0.5477;
						x2 = 0.9669;
						break;
					case 47:
						x1 = 0.5313;
						x2 = 0.9766;
						break;
					case 48:
						x1 = 0.5172;
						x2 = 0.9855;
						break;
					case 49:
						x1 = 0.5063;
						x2 = 0.9928;
						break;
					case 50:
						x1 = 0.4960;
						x2 = 0.9998;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_PY_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 5.631;
						x2 = 0.2745;
						break;
					case 2:
						x1 = 4.381;
						x2 = 0.3633;
						break;
					case 3:
						x1 = 3.791;
						x2 = 0.4127;
						break;
					case 4:
						x1 = 3.350;
						x2 = 0.4545;
						break;
					case 5:
						x1 = 3.076;
						x2 = 0.4820;
						break;
					case 6:
						x1 = 2.979;
						x2 = 0.4928;
						break;
					case 7:
						x1 = 2.828;
						x2 = 0.5095;
						break;
					case 8:
						x1 = 2.699;
						x2 = 0.5233;
						break;
					case 9:
						x1 = 2.647;
						x2 = 0.5277;
						break;
					case 10:
						x1 = 2.613;
						x2 = 0.5301;
						break;
					case 11:
						x1 = 2.539;
						x2 = 0.5367;
						break;
					case 12:
						x1 = 2.490;
						x2 = 0.5409;
						break;
					case 13:
						x1 = 2.410;
						x2 = 0.5499;
						break;
					case 14:
						x1 = 2.309;
						x2 = 0.5624;
						break;
					case 15:
						x1 = 2.247;
						x2 = 0.5685;
						break;
					case 16:
						x1 = 2.193;
						x2 = 0.5745;
						break;
					case 17:
						x1 = 2.122;
						x2 = 0.5842;
						break;
					case 18:
						x1 = 2.013;
						x2 = 0.5998;
						break;
					case 19:
						x1 = 1.950;
						x2 = 0.6088;
						break;
					case 20:
						x1 = 1.896;
						x2 = 0.6159;
						break;
					case 21:
						x1 = 1.827;
						x2 = 0.6263;
						break;
					case 22:
						x1 = 1.746;
						x2 = 0.6391;
						break;
					case 23:
						x1 = 1.680;
						x2 = 0.6496;
						break;
					case 24:
						x1 = 1.629;
						x2 = 0.6582;
						break;
					case 25:
						x1 = 1.582;
						x2 = 0.6660;
						break;
					case 26:
						x1 = 1.520;
						x2 = 0.6771;
						break;
					case 27:
						x1 = 1.446;
						x2 = 0.6914;
						break;
					case 28:
						x1 = 1.397;
						x2 = 0.7012;
						break;
					case 29:
						x1 = 1.340;
						x2 = 0.7130;
						break;
					case 30:
						x1 = 1.275;
						x2 = 0.7271;
						break;
					case 31:
						x1 = 1.195;
						x2 = 0.7457;
						break;
					case 32:
						x1 = 1.132;
						x2 = 0.7618;
						break;
					case 33:
						x1 = 1.066;
						x2 = 0.7793;
						break;
					case 34:
						x1 = 1.005;
						x2 = 0.7965;
						break;
					case 35:
						x1 = 0.9542;
						x2 = 0.8114;
						break;
					case 36:
						x1 = 0.9156;
						x2 = 0.8231;
						break;
					case 37:
						x1 = 0.8797;
						x2 = 0.8343;
						break;
					case 38:
						x1 = 0.8241;
						x2 = 0.8528;
						break;
					case 39:
						x1 = 0.7806;
						x2 = 0.8682;
						break;
					case 40:
						x1 = 0.7372;
						x2 = 0.8844;
						break;
					case 41:
						x1 = 0.6979;
						x2 = 0.9000;
						break;
					case 42:
						x1 = 0.6669;
						x2 = 0.9130;
						break;
					case 43:
						x1 = 0.6360;
						x2 = 0.9266;
						break;
					case 44:
						x1 = 0.6046;
						x2 = 0.9414;
						break;
					case 45:
						x1 = 0.5801;
						x2 = 0.9535;
						break;
					case 46:
						x1 = 0.5585;
						x2 = 0.9643;
						break;
					case 47:
						x1 = 0.5467;
						x2 = 0.9707;
						break;
					case 48:
						x1 = 0.5338;
						x2 = 0.9778;
						break;
					case 49:
						x1 = 0.5139;
						x2 = 0.9891;
						break;
					case 50:
						x1 = 0.4978;
						x2 = 0.9984;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_BA_NIGHGI:
					switch ((int) bhage) {
					case 1:
						x1 = 12.14;
						x2 = 0.1957;
						break;
					case 2:
						x1 = 10.29;
						x2 = 0.2324;
						break;
					case 3:
						x1 = 8.348;
						x2 = 0.2829;
						break;
					case 4:
						x1 = 6.151;
						x2 = 0.3585;
						break;
					case 5:
						x1 = 5.243;
						x2 = 0.3967;
						break;
					case 6:
						x1 = 4.009;
						x2 = 0.4616;
						break;
					case 7:
						x1 = 3.561;
						x2 = 0.4893;
						break;
					case 8:
						x1 = 3.313;
						x2 = 0.5064;
						break;
					case 9:
						x1 = 3.010;
						x2 = 0.5293;
						break;
					case 10:
						x1 = 2.667;
						x2 = 0.5588;
						break;
					case 11:
						x1 = 2.436;
						x2 = 0.5810;
						break;
					case 12:
						x1 = 2.265;
						x2 = 0.5988;
						break;
					case 13:
						x1 = 2.120;
						x2 = 0.6152;
						break;
					case 14:
						x1 = 1.999;
						x2 = 0.6301;
						break;
					case 15:
						x1 = 1.866;
						x2 = 0.6467;
						break;
					case 16:
						x1 = 1.749;
						x2 = 0.6623;
						break;
					case 17:
						x1 = 1.688;
						x2 = 0.6706;
						break;
					case 18:
						x1 = 1.584;
						x2 = 0.6862;
						break;
					case 19:
						x1 = 1.513;
						x2 = 0.6976;
						break;
					case 20:
						x1 = 1.461;
						x2 = 0.7064;
						break;
					case 21:
						x1 = 1.425;
						x2 = 0.7130;
						break;
					case 22:
						x1 = 1.370;
						x2 = 0.7230;
						break;
					case 23:
						x1 = 1.328;
						x2 = 0.7310;
						break;
					case 24:
						x1 = 1.279;
						x2 = 0.7404;
						break;
					case 25:
						x1 = 1.236;
						x2 = 0.7491;
						break;
					case 26:
						x1 = 1.171;
						x2 = 0.7629;
						break;
					case 27:
						x1 = 1.113;
						x2 = 0.7758;
						break;
					case 28:
						x1 = 1.039;
						x2 = 0.7933;
						break;
					case 29:
						x1 = 0.9762;
						x2 = 0.8092;
						break;
					case 30:
						x1 = 0.9295;
						x2 = 0.8217;
						break;
					case 31:
						x1 = 0.8831;
						x2 = 0.8349;
						break;
					case 32:
						x1 = 0.8522;
						x2 = 0.8444;
						break;
					case 33:
						x1 = 0.8202;
						x2 = 0.8548;
						break;
					case 34:
						x1 = 0.7936;
						x2 = 0.8639;
						break;
					case 35:
						x1 = 0.7720;
						x2 = 0.8716;
						break;
					case 36:
						x1 = 0.7304;
						x2 = 0.8864;
						break;
					case 37:
						x1 = 0.6982;
						x2 = 0.8986;
						break;
					case 38:
						x1 = 0.6690;
						x2 = 0.9101;
						break;
					case 39:
						x1 = 0.6367;
						x2 = 0.9236;
						break;
					case 40:
						x1 = 0.6216;
						x2 = 0.9307;
						break;
					case 41:
						x1 = 0.6003;
						x2 = 0.9406;
						break;
					case 42:
						x1 = 0.5830;
						x2 = 0.9490;
						break;
					case 43:
						x1 = 0.5639;
						x2 = 0.9585;
						break;
					case 44:
						x1 = 0.5475;
						x2 = 0.9670;
						break;
					case 45:
						x1 = 0.5365;
						x2 = 0.9732;
						break;
					case 46:
						x1 = 0.5286;
						x2 = 0.9783;
						break;
					case 47:
						x1 = 0.5291;
						x2 = 0.9792;
						break;
					case 48:
						x1 = 0.5217;
						x2 = 0.9843;
						break;
					case 49:
						x1 = 0.5076;
						x2 = 0.9927;
						break;
					case 50:
						x1 = 0.4952;
						x2 = 1.000;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				case SI_BL_THROWERGI:
					switch ((int) bhage) {
					case 1:
						x1 = 2.4623;
						x2 = 0.5809;
						break;
					case 2:
						x1 = 1.6700;
						x2 = 0.7080;
						break;
					case 3:
						x1 = 1.5688;
						x2 = 0.7235;
						break;
					case 4:
						x1 = 1.5606;
						x2 = 0.7193;
						break;
					case 5:
						x1 = 1.6318;
						x2 = 0.6995;
						break;
					case 6:
						x1 = 1.6382;
						x2 = 0.6940;
						break;
					case 7:
						x1 = 1.5960;
						x2 = 0.6984;
						break;
					case 8:
						x1 = 1.6466;
						x2 = 0.6857;
						break;
					case 9:
						x1 = 1.6580;
						x2 = 0.6803;
						break;
					case 10:
						x1 = 1.6481;
						x2 = 0.6790;
						break;
					case 11:
						x1 = 1.6394;
						x2 = 0.6771;
						break;
					case 12:
						x1 = 1.6172;
						x2 = 0.6784;
						break;
					case 13:
						x1 = 1.6248;
						x2 = 0.6738;
						break;
					case 14:
						x1 = 1.6072;
						x2 = 0.6746;
						break;
					case 15:
						x1 = 1.5771;
						x2 = 0.6775;
						break;
					case 16:
						x1 = 1.5518;
						x2 = 0.6788;
						break;
					case 17:
						x1 = 1.5065;
						x2 = 0.6852;
						break;
					case 18:
						x1 = 1.4929;
						x2 = 0.6851;
						break;
					case 19:
						x1 = 1.4477;
						x2 = 0.6919;
						break;
					case 20:
						x1 = 1.3977;
						x2 = 0.7010;
						break;
					case 21:
						x1 = 1.3589;
						x2 = 0.7074;
						break;
					case 22:
						x1 = 1.3175;
						x2 = 0.7154;
						break;
					case 23:
						x1 = 1.2742;
						x2 = 0.7243;
						break;
					case 24:
						x1 = 1.2404;
						x2 = 0.7311;
						break;
					case 25:
						x1 = 1.1814;
						x2 = 0.7446;
						break;
					case 26:
						x1 = 1.1294;
						x2 = 0.7569;
						break;
					case 27:
						x1 = 1.0878;
						x2 = 0.7668;
						break;
					case 28:
						x1 = 1.0582;
						x2 = 0.7739;
						break;
					case 29:
						x1 = 1.0110;
						x2 = 0.7869;
						break;
					case 30:
						x1 = 0.9693;
						x2 = 0.7988;
						break;
					case 31:
						x1 = 0.9372;
						x2 = 0.8083;
						break;
					case 32:
						x1 = 0.8920;
						x2 = 0.8219;
						break;
					case 33:
						x1 = 0.8510;
						x2 = 0.8354;
						break;
					case 34:
						x1 = 0.8190;
						x2 = 0.8465;
						break;
					case 35:
						x1 = 0.7898;
						x2 = 0.8570;
						break;
					case 36:
						x1 = 0.7551;
						x2 = 0.8702;
						break;
					case 37:
						x1 = 0.7269;
						x2 = 0.8814;
						break;
					case 38:
						x1 = 0.7022;
						x2 = 0.8918;
						break;
					case 39:
						x1 = 0.6809;
						x2 = 0.9012;
						break;
					case 40:
						x1 = 0.6516;
						x2 = 0.9142;
						break;
					case 41:
						x1 = 0.6278;
						x2 = 0.9253;
						break;
					case 42:
						x1 = 0.6080;
						x2 = 0.9353;
						break;
					case 43:
						x1 = 0.5920;
						x2 = 0.9436;
						break;
					case 44:
						x1 = 0.5751;
						x2 = 0.9523;
						break;
					case 45:
						x1 = 0.5606;
						x2 = 0.9604;
						break;
					case 46:
						x1 = 0.5500;
						x2 = 0.9663;
						break;
					case 47:
						x1 = 0.5347;
						x2 = 0.9753;
						break;
					case 48:
						x1 = 0.5230;
						x2 = 0.9825;
						break;
					case 49:
						x1 = 0.5081;
						x2 = 0.9920;
						break;
					case 50:
						x1 = 0.4937;
						x2 = 1.0012;
						break;
					default:
						x1 = 0;
						x2 = 0;
						break;
					}
					index = findSiteIndex2(bhage, height, x1, x2);
					break;
				default:
					index = siteIterate(cuIndex, bhage, SI_AT_BREAST, height);
					break;
				}
			} else
				index = siteIterate(cuIndex, bhage, SI_AT_BREAST, height);
		}
		return index;
	}

	private static double findSiteIndexCommon(
			double bhage, double height, double x1, double heightAgeFactor, DoubleUnaryOperator modifyIndex
	) throws GrowthInterceptMaximumException {
		double index;
		if (x1 == 0) {
			throw new GrowthInterceptMaximumException(
					"Variable height growth intercept formulation, bhage > range: " + x1
			);
		} else {
			index = (height - 1.3) * heightAgeFactor / (bhage - 0.5);
			index = modifyIndex.applyAsDouble(index);
		}
		return index;
	}

	private static double findSiteIndex1(double bhage, double height, double x1, double x2) {
		double index;
		double logBhAge;
		logBhAge = Math.log(bhage);

		index = Utils.computeInFeet(
				height,
				heightFt -> 4.5 + 38.787 - 2.805 * logBhAge * logBhAge + 0.0216 * bhage * logBhAge + x1 * heightFt
						+ x2 * heightFt / bhage
		);
		return index;
	}

	private static double findSiteIndex2(double bhage, double height, double x1, double x2)
			throws GrowthInterceptMaximumException {

		return findSiteIndexCommon(bhage, height, x1, 100, index -> 1.3 + x1 * ppow(index, x2));
	}

	private static double findeSiteIndex3(double bhage, double height, double x1, double x2)
			throws GrowthInterceptMaximumException {
		return findSiteIndexCommon(bhage, height, x1, 100, index -> x1 * ppow(index, x2));
	}

	public static double siteIterate(SiteIndexEquation cuIndex, double age, SiteIndexAgeType ageType, double height)
			throws CommonCalculatorException {
		double site;
		double step;
		double testTop;
		double y2bh;

		/* initial guess */
		site = height;
		if (site < 1.3) {
			site = 1.3;
		}
		step = site / 2.0;

		/* loop until real close, or other end condition */
		do {

			/* estimate y2bh */
			y2bh = SiteIndexYears2BreastHeight.y2bh(cuIndex, site);

			if (ageType == SI_AT_BREAST) {
				testTop = SiteIndex2Height
						.indexToHeight(cuIndex, age, SI_AT_BREAST, site, y2bh, 0.5 /* may have to change */);
			} else {
				/* was age - y2bh */
				testTop = SiteIndex2Height.indexToHeight(
						cuIndex, AgeToAge.ageToAge(cuIndex, age, SI_AT_TOTAL, SI_AT_BREAST, y2bh), SI_AT_BREAST, site,
						y2bh, 0.5
				); // 0.5 may have to change
			}

			/*
			 * System.out. printf("age=%.0f, height=%.1f, test_top=%.1f, site=%.2f, step=%.7f%n", age, height, test_top,
			 * site, step);
			 *
			 */

			if ( (testTop - height > 0.01) || (testTop - height < -0.01)) {
				/* not close enough */
				if (testTop > height) {
					if (step > 0) {
						step = -step / 2.0;
					}
				} else {
					if (step < 0) {
						step = -step / 2.0;
					}
				}
				site += step;
			} else {
				/* done */
				break;
			}

			/* check for lack of convergence, so we're not here forever */
			if (step < 0.00001 && step > -0.00001) {
				/* we have a value, but perhaps not too accurate */
				break;
			}
			if (site > 999.0) {
				site = SI_ERR_NO_ANS;
				break;
			}
			/* site index must be at least 1.3 */
			if (site < 1.3) {
				if (step > 0) {
					site += step;
				} else {
					site -= step;
					step = step / 2.0;
				}
			}
		} while (true);

		if (site == SI_ERR_NO_ANS) {
			throw new NoAnswerException("Iteration could not converge (projected site index > 999), site: " + site);
		} else {
			return site;
		}

	}

	public static double huGarciaQ(double siteIndex, double breastHeightAge) {
		double h, q, step, diff, lastdiff;

		q = 0.02;
		step = 0.01;
		lastdiff = 0;
		diff = 0;

		do {
			h = huGarciaH(q, breastHeightAge);
			lastdiff = diff;
			diff = siteIndex - h;
			if (diff > 0.0000001) {
				if (lastdiff < 0) {
					step = step / 2.0;
				}
				q += step;

			} else if (diff < -0.0000001) {
				if (lastdiff > 0) {
					step = step / 2.0;
				}
				q -= step;

				if (q <= 0) {
					q = 0.0000001;
				}
			} else {
				break;
			}
			if (step < 0.0000001) {
				break;
			}
		} while (true);

		return q;
	}

	public static double huGarciaH(double q, double breastHeightAge) {
		double a, height;

		a = 283.9 * Math.pow(q, 0.5137);
		height = a * Math.pow(1 - (1 - Math.pow(1.3 / a, 0.5829)) * Math.exp(-q * (breastHeightAge - 0.5)), 1.71556);
		return height;
	}

}
