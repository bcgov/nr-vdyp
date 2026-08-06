package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.model.VdypEntity.MISSING_FLOAT_VALUE;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.PrimarySpeciesDetails;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.sindex.Reference;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEstimationType;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.SpeciesErrorException;

public class ProcessingEngine {

	private static final Logger logger = LoggerFactory.getLogger(ProcessingEngine.class);
	protected static final int UC_ALL_INDEX = UtilizationClass.ALL.ordinal();
	protected static final int UC_SMALL_INDEX = UtilizationClass.SMALL.ordinal();

	/**
	 * Calculate the siteCurve number of all species for which one was not supplied. All calculations are done in the
	 * given bank but the result is also stored in the LayerProcessingState.
	 * <p>
	 * FORTRAN notes: the original SXINXSET function set both INXSC/INXSCV and BANK3/SCNB, except for index 0 of SCNB.
	 *
	 * @param lps          the LayerProcessingState in which the calculations are done.
	 * @param siteCurveMap the Site Curve definitions.
	 */
	protected static void calculateMissingSiteCurves(
			LayerProcessingState<?> lps, MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap
	) {
		Bank bank = lps.getBank();

		calculateMissingSiteCurves(bank, siteCurveMap);

		lps.setSiteCurveNumbers(bank.siteCurveNumbers);
	}

	/**
	 * Calculate the siteCurve number of all species for which one was not supplied. All calculations are done in the
	 * given bank but the result is also stored in the LayerProcessingState.
	 * <p>
	 * FORTRAN notes: the original SXINXSET function set both INXSC/INXSCV and BANK3/SCNB, except for index 0 of SCNB.
	 *
	 * @param bank         the Bank in which the calculations are done.
	 * @param siteCurveMap the Site Curve definitions.
	 */
	protected static void
			calculateMissingSiteCurves(Bank bank, MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap) {
		Region region = bank.getBecZone().getRegion();

		for (int i : bank.getIndices()) {

			if (bank.siteCurveNumbers[i] == VdypEntity.MISSING_INTEGER_VALUE) {

				Optional<Sp64Distribution> sp0Dist = bank.sp64Distributions[i].getSpeciesDistribution(1);
				final String speciesId = bank.speciesNames[i];

				Optional<SiteIndexEquation> scIndex = calculateMissingSiteCurve(
						siteCurveMap, region, sp0Dist, speciesId
				);

				bank.siteCurveNumbers[i] = scIndex.orElseThrow().n();
			}
		}

	}

	/**
	 * Calculate the siteCurve number of a species group.
	 *
	 * @param siteCurveMap the Site Curve definitions.
	 * @param region       the BEC region of the polygon
	 * @param sp0Dist      The species distribution
	 * @param speciesId    The species group to be calculated
	 * @return
	 */
	protected static Optional<SiteIndexEquation> calculateMissingSiteCurve(
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap, Region region,
			Optional<Sp64Distribution> sp0Dist, final String speciesId
	) {
		return sp0Dist
				// First alternative is to use the name of the first of the species' sp64Distributions
				.flatMap(dist -> calculateMissingSiteCurve(siteCurveMap, region, speciesId, dist.getGenusAlias()))
				// Second alternative is to use the species name as given in the species' "speciesName" field
				.or(() -> calculateMissingSiteCurve(siteCurveMap, region, speciesId, speciesId));
	}

	/**
	 * Calculate the siteCurve number of a species group.
	 *
	 * @param siteCurveMap the Site Curve definitions.
	 * @param region       the BEC region of the polygon
	 * @param speciesId    The species group to be used if the map is empty
	 * @param sp0          The species group to be used if the map is not empty
	 * @return
	 */
	protected static Optional<SiteIndexEquation> calculateMissingSiteCurve(
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap, Region region, final String speciesId,
			String sp0
	) {
		Optional<SiteIndexEquation> scIndex;
		if (!siteCurveMap.isEmpty()) {
			scIndex = Utils.optSafe(siteCurveMap.get(sp0, region));
		} else {
			SiteIndexEquation siCurve = SiteTool.getSICurve(speciesId, region.equals(Region.COASTAL));
			scIndex = Optional.of(siCurve).filter(sc -> sc != SiteIndexEquation.SI_NO_EQUATION);
		}
		return scIndex;
	}

	/**
	 * VPRIME1, method == 1: calculate the percentage of forested land covered by each species by dividing the basal
	 * area of each given species with the basal area of the polygon covered by forest.
	 *
	 * @param state the bank in which the calculations are performed
	 */
	public static void calculateCoverages(LayerProcessingState<?> lps) {

		Bank bank = lps.getBank();

		logger.atDebug().addArgument(lps.getNSpecies()).addArgument(bank.basalAreas[0][0]).log(
				"Calculating coverages as a ratio of Species BA over Total BA. # species: {}; Layer total 7.5cm+ basal area: {}"
		);

		for (int i : lps.getIndices()) {
			bank.percentagesOfForestedLand[i] = bank.basalAreas[i][UC_ALL_INDEX] / bank.basalAreas[0][UC_ALL_INDEX]
					* 100.0f;

			logger.atDebug().addArgument(i).addArgument(bank.speciesIndices[i]).addArgument(bank.speciesNames[i])
					.addArgument(bank.basalAreas[i][UC_ALL_INDEX]).addArgument(bank.percentagesOfForestedLand[i])
					.log("Species {}: SP0 {}, Name {}, Species 7.5cm+ BA {}, Calculated Percent {}");
		}
	}

	/**
	 * SITEADD
	 * <p>
	 * (1) If the site index of the primary species has not been set, calculate it as the average of the site indices of
	 * the other species that -do- have one, after converting each between the site curve of the other species and that
	 * of the primary species.
	 * <p>
	 * (2) If the site index of the primary species has (now) been set, calculate that of the other species whose site
	 * index has not been set from the primary site index after converting it between the site curve of the other
	 * species and that of the primary species.
	 *
	 * @param lps the bank in which the calculations are done.
	 * @throws ProcessingException
	 */
	protected void estimateMissingSiteIndices(LayerProcessingState<?> lps) throws ProcessingException {

		Bank bank = lps.getBank();

		int pspIndex = lps.getPrimarySpeciesIndex();
		SiteIndexEquation pspSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(pspIndex));

		// (1)

		float pspSiteIndex = estimateMissingPrimarySiteIndex(lps, pspIndex, pspSiteCurve);

		// (2)

		pspSiteIndex = estimateMissingNonPrimarySiteIndices(lps, pspIndex, pspSiteCurve);

		// Finally, set bank.siteIndices[0] to that of the primary species.
		bank.siteIndices[0] = pspSiteIndex;
	}

	protected float estimateMissingNonPrimarySiteIndices(
			LayerProcessingState<?> lps, int pspIndex, SiteIndexEquation pspSiteCurve
	) throws ProcessingException {
		Bank bank = lps.getBank();
		float pspSiteIndex = bank.siteIndices[pspIndex];
		if (!Float.isNaN(pspSiteIndex)) {
			for (int spIndex : lps.getIndices()) {

				if (spIndex == pspIndex) {
					continue;
				}
				final int siteCurveNumber = lps.getSiteCurveNumber(spIndex);

				pspSiteIndex = estimateMissingNonPrimarySiteIndex(
						pspSiteCurve, bank, pspSiteIndex, spIndex, siteCurveNumber
				);
			}
		}
		return pspSiteIndex;
	}

	/**
	 * Wraps {@link SiteTool.convertSiteIndexBetweenCurves} and handles its exceptions.
	 *
	 * @param siteCurve1 source curve
	 * @param siteIndex  site index to convert
	 * @param siteCurve2 target curve
	 * @return
	 * @throws ProcessingException
	 */
	protected Optional<Float>
			convertSiteIndex(SiteIndexEquation siteCurve1, double siteIndex, SiteIndexEquation siteCurve2)
					throws ProcessingException {
		try {
			double mappedSiteIndex = convertSiteIndexBetweenCurves(siteCurve1, siteIndex, siteCurve2);
			return Optional.of((float) mappedSiteIndex);
		} catch (NoAnswerException e) {
			logger.warn(
					MessageFormat.format("there is no conversion between curves {0} and {1}.", siteCurve1, siteCurve2)
			);
			return Optional.empty();
		} catch (CurveErrorException | SpeciesErrorException e) {
			throw new ProcessingException(
					MessageFormat.format(
							"convertSiteIndexBetweenCurves on {0}, {1} and {2} failed", siteCurve1, siteIndex,
							siteCurve2
					), e
			);
		}

	}

	protected float estimateMissingNonPrimarySiteIndex(
			SiteIndexEquation pspSiteCurve, Bank bank, float pspSiteIndex, int spIndex, final int siteCurveNumber
	) throws ProcessingException {
		float spSiteIndex = bank.siteIndices[spIndex];
		if (Float.isNaN(spSiteIndex)) {
			SiteIndexEquation spSiteCurve = getSiteIndexEquationByIndex(siteCurveNumber);

			var mappedSiteIndex = convertSiteIndex(pspSiteCurve, pspSiteIndex, spSiteCurve).filter(msi -> msi > 0.0f);
			mappedSiteIndex.ifPresentOrElse(
					msi -> bank.siteIndices[spIndex] = msi,
					() -> logger.info("Not calculating site index for species {}", bank.speciesNames[spIndex])
			);
			pspSiteIndex = mappedSiteIndex.filter(msi -> msi > 1.3f).orElse(pspSiteIndex);

		}
		return pspSiteIndex;
	}

	protected float
			estimateMissingPrimarySiteIndex(LayerProcessingState<?> lps, int pspIndex, SiteIndexEquation pspSiteCurve)
					throws ProcessingException {
		Bank bank = lps.getBank();
		if (Float.isNaN(bank.siteIndices[pspIndex])) {

			DoubleAdder otherSiteIndicesSum = new DoubleAdder();
			AtomicInteger nOtherSiteIndices = new AtomicInteger();

			for (int spIndex : lps.getIndices()) {

				if (spIndex == pspIndex) {
					continue;
				}

				float spSiteIndex = bank.siteIndices[spIndex];

				if (!Float.isNaN(spSiteIndex)) {
					SiteIndexEquation spSiteCurve = getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex));

					var mappedSiteIndex = convertSiteIndex(spSiteCurve, spSiteIndex, pspSiteCurve);

					mappedSiteIndex.filter(msi -> msi > 1.3).ifPresentOrElse(msi -> {
						otherSiteIndicesSum.add(msi);
						nOtherSiteIndices.incrementAndGet();
					}, () -> logger.info(
							"Excluding species {} from the estimation of the site index of {}",
							bank.speciesNames[spIndex], bank.speciesNames[pspIndex]
					));

				}
			}

			if (nOtherSiteIndices.get() > 0) {
				bank.siteIndices[pspIndex] = (float) (otherSiteIndicesSum.doubleValue() / nOtherSiteIndices.get());
			}
		}

		return bank.siteIndices[pspIndex];

	}

	@FunctionalInterface
	interface SiteIndexAndAgeEstimationChoice {
		void apply(LayerProcessingState<?> lps, Bank bank, int pspIndex, int nSpecies, SiteIndexEquation pspSiteCurve)
				throws ProcessingException;
	}

	// Would heave made it static but it needs access to `this` when being initialized
	private final SiteIndexAndAgeEstimationChoice[] ESTIMATION_CHOICES = {
			// 0
			(l, b, pi, n, psc) -> {
				// Should be handled by the if-break at the start of the loop
				throw new IllegalStateException();
			},

			// 1
			this::assignPrimarySiteIndexByConversion,

			// 2
			this::setOtherIndicesUsingPrimary,

			// 3
			(l, b, pi, n, psc) -> fillMissingAgeOfTriplet(l, b),

			// 4
			(l, b, pi, n, psc) -> moveTotalAgeFromNonPriamryToPrimary(l, b, pi),

			// 5
			(l, b, pi, n, psc) -> estimateDominantHeightFromLoreyHeight(l, b, pi, SpeciesToApplyTo.PRIMARY),
			// 6
			(l, b, pi, n, psc) -> estimateDominantHeightFromLoreyHeight(l, b, pi, SpeciesToApplyTo.NONPRIMARY),

			// 7
			(
					l, b, pi, n, psc
			) -> estimateSiteIndexFromHeightAndAge(l, b, pi, SiteIndexAgeType.SI_AT_TOTAL, SpeciesToApplyTo.PRIMARY),
			// 8
			(
					l, b, pi, n, psc
			) -> estimateSiteIndexFromHeightAndAge(l, b, pi, SiteIndexAgeType.SI_AT_BREAST, SpeciesToApplyTo.PRIMARY),
			// 9
			(
					l, b, pi, n, psc
			) -> estimateSiteIndexFromHeightAndAge(l, b, pi, SiteIndexAgeType.SI_AT_TOTAL, SpeciesToApplyTo.NONPRIMARY),
			// 10
			(l, b, pi, n, psc) -> estimateSiteIndexFromHeightAndAge(
					l, b, pi, SiteIndexAgeType.SI_AT_BREAST, SpeciesToApplyTo.NONPRIMARY
			),

			// 11
			(l, b, pi, n, psc) -> estimateAgesFromHeightAndSiteIndex(l, b, pi, SpeciesToApplyTo.PRIMARY),
			// 12
			(l, b, pi, n, psc) -> estimateAgesFromHeightAndSiteIndex(l, b, pi, SpeciesToApplyTo.NONPRIMARY),

			// 13
			(l, b, pi, n, psc) -> calculateYearsToBreastHeightFromSiteIndex(l, b, pi, SpeciesToApplyTo.PRIMARY),
			// 14
			(l, b, pi, n, psc) -> calculateYearsToBreastHeightFromSiteIndex(l, b, pi, SpeciesToApplyTo.NONPRIMARY),

			// 15 Same as 1 but only if age total between 0 and 30 exclusive
			(l, b, pi, n, psc) -> {
				if (!Float.isNaN(b.ageTotals[pi]) && b.ageTotals[pi] > 0.0f && b.ageTotals[pi] < 30.0f) {
					assignPrimarySiteIndexByConversion(l, b, pi, n, psc);
				}
			} };

	/**
	 * estimateMissingSiteIndicesAndAgesExtended (Formerly SITEADDU)
	 * <p>
	 * Augments missing site indices, ages, years-to-breast-height, and dominant heights using a sequence of
	 * debug-controlled strategies.
	 * <p>
	 * This is an expanded version of SITEADD. In the original Fortran, options 11-25 are read from NDEBUG(11..25), and
	 * processing continues until an option value of 0 is encountered.
	 * <p>
	 * Notes on object mapping:
	 * <ul>
	 * <li>bank.siteIndices[] ~= SIL1S(*)</li>
	 * <li>bank.dominantHeights[] ~= HDL1S(*)</li>
	 * <li>bank.totalAges[] ~= AGETOTL1S(*)</li>
	 * <li>bank.breastHeightAges[] ~= AGEBHL1S(*)</li>
	 * <li>bank.yearsToBreastHeight[] ~= YTBHL1S(*)</li>
	 * <li>lps.getSiteCurveNumber(i) ~= INXSCV(i,1)</li>
	 * </ul>
	 *
	 * @param lps the layer processing state
	 * @throws ProcessingException on serious calculation failures
	 */
	protected void estimateMissingSiteIndicesAndAgesExtended(
			LayerProcessingState<?> lps, ProcessingDebugSettings debugSettings
	) throws ProcessingException {

		final Bank bank = lps.getBank();

		final int pspIndex = lps.getPrimarySpeciesIndex();
		final int nSpecies = lps.getNSpecies(); // Should correspond to NSPL1.
		final SiteIndexEquation pspSiteCurve = getSiteIndexEquationByIndex(lps.getSiteCurveNumber(pspIndex));

		for (int debugSlot = 11; debugSlot <= 20; debugSlot++) {
			int choice = debugSettings.getValue(debugSlot);

			if (choice == 0) {
				break;
			}

			if (choice > 0 && choice < ESTIMATION_CHOICES.length) {
				ESTIMATION_CHOICES[choice].apply(lps, bank, pspIndex, nSpecies, pspSiteCurve);
			} else {
				logger.warn("Unknown site index/age estimation choice {}.  Ignoring.", choice);
			}

		}

		// Fill in L1COM3 equivalents from the primary species.
		lps.setPrimarySpeciesDetails(
				new PrimarySpeciesDetails(
						bank.dominantHeights[pspIndex], bank.siteIndices[pspIndex], bank.ageTotals[pspIndex],
						bank.yearsAtBreastHeight[pspIndex], bank.yearsToBreastHeight[pspIndex]
				)
		);

		// Fill in SC for primary species, equivalent to INXSCV(0,1) = INXSCV(IPOSP,1)
		bank.siteCurveNumbers[0] = bank.siteCurveNumbers[pspIndex];
		bank.siteIndices[0] = bank.siteIndices[pspIndex];

		if (Float.isNaN(bank.yearsAtBreastHeight[pspIndex]) || bank.yearsAtBreastHeight[pspIndex] <= 0.0f
				|| Float.isNaN(bank.siteIndices[pspIndex]) || bank.siteIndices[pspIndex] <= 0.0f) {
			throw new ProcessingException("Primary species lacks BH age or site index");
		}
	}

	protected enum SpeciesToApplyTo {
		/**
		 * Apply only to primary species
		 */
		PRIMARY {
			@Override
			boolean applies(int spIndex, int primaryIndex) {
				return spIndex == primaryIndex;
			}
		},
		/**
		 * Apply only to non-primary species
		 */
		NONPRIMARY {
			@Override
			boolean applies(int spIndex, int primaryIndex) {
				return spIndex != primaryIndex;
			}
		};

		abstract boolean applies(int spIndex, int primaryIndex);
	}

	/**
	 * Calculate years to breast height from site index.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param applyTo  Which species should this be applied to
	 * @throws ProcessingException
	 */
	protected void calculateYearsToBreastHeightFromSiteIndex(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, SpeciesToApplyTo applyTo
	) throws ProcessingException {
		for (int spIndex : lps.getIndices()) {

			boolean applies = applyTo.applies(spIndex, pspIndex);

			if (!applies) {
				continue;
			}

			if (Float.isNaN(bank.siteIndices[spIndex]) || bank.siteIndices[spIndex] <= 0.0f) {
				continue;
			}
			if (bank.yearsToBreastHeight[spIndex] > 0.0f) {
				continue;
			}

			try {
				double ytbh = yearsToBreastHeight(
						getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex)), bank.siteIndices[spIndex]
				);
				if (ytbh > 0.0) {
					bank.yearsToBreastHeight[spIndex] = (float) ytbh;
				}
			} catch (Exception e) {
				throw new ProcessingException("Failed estimating years to breast height from site index", e);
			}
		}
	}

	/**
	 * Estimate ages from dominant height and site index.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param applyTo  Which species should this be applied to
	 * @throws ProcessingException
	 */
	protected void estimateAgesFromHeightAndSiteIndex(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, SpeciesToApplyTo applyTo
	) throws ProcessingException {

		for (int spIndex : lps.getIndices()) {

			boolean applies = applyTo.applies(spIndex, pspIndex);

			if (!applies) {
				continue;
			}

			if (Float.isNaN(bank.siteIndices[spIndex]) || bank.siteIndices[spIndex] <= 0.0f) {
				continue;
			}
			if (Float.isNaN(bank.dominantHeights[spIndex]) || bank.dominantHeights[spIndex] <= 1.3f) {
				continue;
			}
			try {
				// TODO this has been changed pretty considerably from VDYP7 make sure it is acceptable
				if (Float.isNaN(bank.yearsAtBreastHeight[spIndex]) || bank.yearsAtBreastHeight[spIndex] <= 0.0f) {
					bank.yearsAtBreastHeight[spIndex] = (float) heightAndSiteIndexToAge(
							getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex)), bank.dominantHeights[spIndex],
							SiteIndexAgeType.SI_AT_BREAST, bank.siteIndices[spIndex], bank.yearsToBreastHeight[spIndex]
					);
				}
				if (Float.isNaN(bank.ageTotals[spIndex]) || bank.ageTotals[spIndex] <= 0.0f) {
					bank.ageTotals[spIndex] = (float) heightAndSiteIndexToAge(
							getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex)), bank.dominantHeights[spIndex],
							SiteIndexAgeType.SI_AT_TOTAL, bank.siteIndices[spIndex], bank.yearsToBreastHeight[spIndex]
					);
				} else {
					bank.yearsAtBreastHeight[spIndex] = bank.ageTotals[spIndex] - bank.yearsToBreastHeight[spIndex];
				}

			} catch (Exception e) {
				throw new ProcessingException("Failed estimating ages from height and site index", e);
			}

		}
	}

	/**
	 * Estimate site index from dominant height and age.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param ageToUse Which age to use
	 * @param applyTo  Which species should this be applied to
	 * @throws ProcessingException
	 */
	protected void estimateSiteIndexFromHeightAndAge(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, SiteIndexAgeType ageToUse, SpeciesToApplyTo applyTo
	) throws ProcessingException {

		for (int spIndex : lps.getIndices()) {

			if (bank.siteIndices[spIndex] > 0.0f) {
				continue;
			}

			boolean applies = applyTo.applies(spIndex, pspIndex);

			if (!applies) {
				continue;
			}

			if (Float.isNaN(bank.dominantHeights[spIndex]) || bank.dominantHeights[spIndex] <= 0.0f) {
				continue;
			}

			float age = switch (ageToUse) {
			case SI_AT_BREAST -> bank.yearsAtBreastHeight[spIndex];
			case SI_AT_TOTAL -> bank.ageTotals[spIndex];
			default -> {
				throw new UnsupportedOperationException();
			}
			};

			if (Float.isNaN(age) || age <= 0.0f) {
				continue;
			}

			try {
				double siteIndex = heightAndAgeToSiteIndex(
						getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex)), age, ageToUse,
						bank.dominantHeights[spIndex], SiteIndexEstimationType.SI_EST_DIRECT
				);

				if (siteIndex > 0.0) {
					bank.siteIndices[spIndex] = (float) siteIndex;

					if (Float.isNaN(bank.yearsToBreastHeight[spIndex]) || bank.yearsToBreastHeight[spIndex] <= 0.0f) {
						double ytbh = yearsToBreastHeight(
								getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex)), siteIndex
						);
						if (ytbh > 0.0) {
							bank.yearsToBreastHeight[spIndex] = (float) ytbh;
						}
					}
				}

			} catch (Exception e) {
				throw new ProcessingException("Failed estimating site index from height and age", e);
			}
		}
	}

	/**
	 * Estimate dominant height from Lorey height.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param applyTo  Which species should this be applied to
	 * @throws ProcessingException
	 */
	protected void estimateDominantHeightFromLoreyHeight(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, SpeciesToApplyTo applyTo
	) throws ProcessingException {

		for (int spIndex : lps.getIndices()) {
			if (!Float.isNaN(bank.dominantHeights[spIndex]) && bank.dominantHeights[spIndex] > 0.0f) {
				continue;
			}

			boolean applies = applyTo.applies(spIndex, pspIndex);

			if (!applies) {
				continue;
			}

			try {
				bank.dominantHeights[spIndex] = lps.getParent().estimators.estimateLeadHeightFromPrimaryHeight(
						bank.loreyHeights[spIndex][UC_ALL_INDEX], bank.speciesNames[spIndex],
						lps.getBecZone().getRegion(), bank.treesPerHectare[spIndex][UC_ALL_INDEX]
				);

			} catch (Exception e) {
				throw new ProcessingException("Failed estimating dominant height from Lorey height", e);
			}
		}
	}

	/**
	 * Move total age from non-primary to primary species. Try secondary first, then any species.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 */

	protected void moveTotalAgeFromNonPriamryToPrimary(LayerProcessingState<?> lps, Bank bank, int pspIndex) {
		if (Float.isNaN(bank.ageTotals[pspIndex]) || bank.ageTotals[pspIndex] <= 0.0f) {

			int secondarySpeciesIndex = lps.getSecondarySpeciesIndex().orElse(-1);

			if (secondarySpeciesIndex > 0 && bank.ageTotals[secondarySpeciesIndex] > 0.0f) {
				bank.ageTotals[pspIndex] = bank.ageTotals[secondarySpeciesIndex];
			} else {
				for (int spIndex : lps.getIndices()) {
					if (spIndex != pspIndex && bank.ageTotals[spIndex] > 0.0f) {
						bank.ageTotals[pspIndex] = bank.ageTotals[spIndex];
						break;
					}
				}
			}
		}
	}

	/**
	 * When 2 of (total, at breast height, and to breast height) are present, fill in the 3rd with algebra.
	 *
	 * @param lps
	 * @param bank
	 */
	protected void fillMissingAgeOfTriplet(LayerProcessingState<?> lps, Bank bank) {

		for (int spIndex : lps.getIndices()) {

			Reference<Double> totalAge = new Reference<>(
					Float.isNaN(bank.ageTotals[spIndex]) ? -9.0 : bank.ageTotals[spIndex]
			);
			Reference<Double> bhAge = new Reference<>(
					Float.isNaN(bank.yearsAtBreastHeight[spIndex]) ? -9.0 : bank.yearsAtBreastHeight[spIndex]
			);
			Reference<Double> ytbh = new Reference<>(
					Float.isNaN(bank.yearsToBreastHeight[spIndex]) ? -9.0 : bank.yearsToBreastHeight[spIndex]
			);
			SiteTool.fillInAgeTripletWithoutCorrection(totalAge, bhAge, ytbh);

			bank.ageTotals[spIndex] = totalAge.get().floatValue();
			if (bank.ageTotals[spIndex] <= 0.0f)
				bank.ageTotals[spIndex] = MISSING_FLOAT_VALUE;
			bank.yearsAtBreastHeight[spIndex] = bhAge.get().floatValue();
			if (bank.yearsAtBreastHeight[spIndex] <= 0.0f)
				bank.yearsAtBreastHeight[spIndex] = MISSING_FLOAT_VALUE;
			bank.yearsToBreastHeight[spIndex] = ytbh.get().floatValue();
			if (bank.yearsToBreastHeight[spIndex] <= 0.0f)
				bank.yearsToBreastHeight[spIndex] = MISSING_FLOAT_VALUE;
		}
	}

	/**
	 * Use primary site index to set all other site indices where possible.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param nSpecies
	 * @param pspSiteCurve
	 * @throws ProcessingException
	 */
	protected void setOtherIndicesUsingPrimary(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, int nSpecies, SiteIndexEquation pspSiteCurve
	) throws ProcessingException {
		if (bank.siteIndices[pspIndex] > 0.0f && nSpecies > 1) {
			float pspSiteIndex = bank.siteIndices[pspIndex];

			for (int spIndex : lps.getIndices()) {
				if (spIndex == pspIndex || bank.siteIndices[spIndex] > 0.0f) {
					continue;
				}

				SiteIndexEquation spCurve = getSiteIndexEquationByIndex(lps.getSiteCurveNumber(spIndex));

				try {
					double mapped = convertSiteIndexBetweenCurves(pspSiteCurve, pspSiteIndex, spCurve);
					if (mapped > 0.0) {
						bank.siteIndices[spIndex] = (float) mapped;
					}
				} catch (NoAnswerException e) {
					// Fortran silently ignores failure here.
				} catch (CurveErrorException | SpeciesErrorException e) {
					throw new ProcessingException("Failed converting primary site index to another species curve", e);
				}
			}
		}
	}

	/**
	 * Assign primary site index from conversion of another site index. If that doesn't work, directly move a site
	 * index. Order to check: secondary species first, then species order.
	 *
	 * @param lps
	 * @param bank
	 * @param pspIndex
	 * @param nSpecies
	 * @param pspSiteCurve
	 * @throws ProcessingException
	 */
	protected void assignPrimarySiteIndexByConversion(
			LayerProcessingState<?> lps, Bank bank, int pspIndex, int nSpecies, SiteIndexEquation pspSiteCurve
	) throws ProcessingException {

		if (Float.isNaN(bank.siteIndices[pspIndex]) && nSpecies > 1) {
			int secondarySpeciesIndex = lps.getSecondarySpeciesIndex().orElse(-1);

			float movedSiteIndex = Float.NaN;
			float usableSiteIndex = Float.NaN;
			// FIXME VDYP-1047 Once we are confident we have accurate numberss per VDYP7 we should fix this
			// purposeful error replace unusedSetUsableSiteINdex references with usableSiteIndex
			float unusedSetUsableSiteIndex = Float.NaN;

			for (int ii = 0; ii <= nSpecies; ii++) {

				int spIndex;
				if (ii == 0) {
					spIndex = secondarySpeciesIndex;
				} else {
					spIndex = ii;
				}

				if (spIndex == secondarySpeciesIndex && ii == 0 && spIndex < 0) {
					continue;
				}

				if (spIndex == secondarySpeciesIndex && ii != 0) {
					continue;
				}

				if (spIndex == pspIndex) {
					continue;
				}

				int spCurveNo = lps.getSiteCurveNumber(spIndex);
				if (spCurveNo <= 0) {
					continue;
				}

				float spSiteIndex = bank.siteIndices[spIndex];
				if (spSiteIndex > 0.0f) {
					if (Float.isNaN(movedSiteIndex)) {
						movedSiteIndex = spSiteIndex;
					}

					SiteIndexEquation fromCurve = getSiteIndexEquationByIndex(spCurveNo);

					try {
						double mapped = convertSiteIndexBetweenCurves(fromCurve, spSiteIndex, pspSiteCurve);
						if (mapped > 0.0) {
							unusedSetUsableSiteIndex = (float) mapped;
							break;
						}
					} catch (NoAnswerException e) {
						// Fortran just keeps searching. No warning there.
					} catch (CurveErrorException | SpeciesErrorException e) {
						throw new ProcessingException("Failed converting site index to primary species curve", e);
					}
				}
			}

			if (Float.isNaN(unusedSetUsableSiteIndex) && movedSiteIndex > 0.0f) {
				usableSiteIndex = movedSiteIndex;
			}

			if (usableSiteIndex > 0.0f) {
				bank.siteIndices[pspIndex] = usableSiteIndex;
			}
		}
	}

	// The following methods wrap SiteTool methods, but are not static so they can be mocked for tests

	SiteIndexEquation getSICurve(String sp64CodeName, boolean isCoastal) {
		return SiteTool.getSICurve(sp64CodeName, isCoastal);
	}

	double yearsToBreastHeight(SiteIndexEquation curve, double siteIndex) throws CommonCalculatorException {
		return SiteTool.yearsToBreastHeight(curve, siteIndex);
	}

	double heightAndSiteIndexToAge(
			SiteIndexEquation curve, double height, SiteIndexAgeType ageType, double siteIndex,
			double years2BreastHeight
	) throws CommonCalculatorException {
		return SiteTool.heightAndSiteIndexToAge(curve, height, ageType, siteIndex, years2BreastHeight);
	}

	double heightAndAgeToSiteIndex(
			SiteIndexEquation curve, double age, SiteIndexAgeType ageType, double height,
			SiteIndexEstimationType estType
	) throws CommonCalculatorException {
		return SiteTool.heightAndAgeToSiteIndex(curve, age, ageType, height, estType);
	}

	double convertSiteIndexBetweenCurves(SiteIndexEquation siteCurve1, double siteIndex1, SiteIndexEquation siteCurve2)
			throws CurveErrorException, SpeciesErrorException, NoAnswerException {
		return SiteTool.convertSiteIndexBetweenCurves(siteCurve1, siteIndex1, siteCurve2);
	}

	public SiteIndexEquation getSiteIndexEquationByIndex(int n) {
		return SiteIndexEquation.getByIndex(n);
	}

	static record AgeTriplet(float total, float toBreastHeight, float atBreastHeight) {
	}

	/**
	 * VHDOM1 METH_H = 2, METH_A = 2, METH_SI = 2.
	 *
	 * @param lps             layer processing state
	 * @param hl1Coefficients the configured dominant height recalculation coefficients
	 *
	 * @throws ProcessingException
	 */
	public void calculateDominantHeightAgeSiteIndex(
			LayerProcessingState<?> lps, MatrixMap2<String, Region, Coefficients> hl1Coefficients
	) throws ProcessingException {

		Bank bank = lps.getBank();

		// Calculate primary species values
		int primarySpeciesIndex = lps.getPrimarySpeciesIndex();

		float primarySpeciesDominantHeight = calculatePrimarySpeciesDominantHeight(
				lps.getBecZone().getRegion(), hl1Coefficients, bank, primarySpeciesIndex
		);

		var primarySpeciesAges = calculatePrimarySpeciesAges(bank, primarySpeciesIndex, lps.getSecondarySpeciesIndex());

		float primarySpeciesSiteIndex = calculatePrimarySpeciesSiteIndex(lps, bank.siteIndices, primarySpeciesIndex);

		lps.setPrimarySpeciesDetails(
				new PrimarySpeciesDetails(
						primarySpeciesDominantHeight, primarySpeciesSiteIndex, primarySpeciesAges.total(),
						primarySpeciesAges.atBreastHeight(), primarySpeciesAges.toBreastHeight()
				)
		);
	}

	float calculatePrimarySpeciesSiteIndex(LayerProcessingState<?> lps, float[] siteIndices, int primarySpeciesIndex)
			throws ProcessingException {
		Optional<Integer> activeIndex;
		// (3) Site Index
		float primarySpeciesSiteIndex = siteIndices[primarySpeciesIndex];
		if (Float.isNaN(primarySpeciesSiteIndex)) {

			activeIndex = findIndexInOneIndexedFloatArray(
					lps.getSecondarySpeciesIndex(), siteIndices, lps.getNSpecies()
			);

			primarySpeciesSiteIndex = siteIndices[activeIndex
					.orElseThrow(() -> new ProcessingException("Site Index data unavailable for ALL species", 7))];
		} else {
			activeIndex = Optional.of(primarySpeciesIndex);
		}

		SiteIndexEquation siteCurve1 = getSiteIndexEquationByIndex(lps.getSiteCurveNumber(activeIndex.get()));
		SiteIndexEquation siteCurve2 = getSiteIndexEquationByIndex(lps.getSiteCurveNumber(0));

		try {
			double newSI = convertSiteIndexBetweenCurves(siteCurve1, primarySpeciesSiteIndex, siteCurve2);
			if (newSI > 1.3) {
				primarySpeciesSiteIndex = (float) newSI;
			}
		} catch (CommonCalculatorException e) {
			// do nothing. primarySpeciesSiteIndex will not be modified.
		}
		return primarySpeciesSiteIndex;
	}

	static AgeTriplet
			calculatePrimarySpeciesAges(Bank bank, int primarySpeciesIndex, Optional<Integer> secondarySpeciesIndex)
					throws ProcessingException {
		// (2) Age (total, years at breast height, years to breast height
		float primarySpeciesTotalAge = bank.ageTotals[primarySpeciesIndex];
		float primarySpeciesYearsAtBreastHeight = bank.yearsAtBreastHeight[primarySpeciesIndex];
		float primarySpeciesYearsToBreastHeight = bank.yearsToBreastHeight[primarySpeciesIndex];

		Optional<Integer> activeIndex;

		if (Float.isNaN(primarySpeciesTotalAge)) {

			activeIndex = findIndexInOneIndexedFloatArray(secondarySpeciesIndex, bank.ageTotals, bank.getNSpecies());

			var index = activeIndex
					.orElseThrow(() -> new ProcessingException("Age data unavailable for ALL species", 5));

			primarySpeciesTotalAge = bank.ageTotals[index];
			if (!Float.isNaN(primarySpeciesYearsToBreastHeight)) {
				primarySpeciesYearsAtBreastHeight = primarySpeciesTotalAge - primarySpeciesYearsToBreastHeight;
			} else if (!Float.isNaN(primarySpeciesYearsAtBreastHeight)) {
				primarySpeciesYearsToBreastHeight = primarySpeciesTotalAge - primarySpeciesYearsAtBreastHeight;
			} else {
				primarySpeciesYearsAtBreastHeight = bank.yearsAtBreastHeight[index];
				primarySpeciesYearsToBreastHeight = bank.yearsToBreastHeight[index];
			}
		}

		return new AgeTriplet(
				primarySpeciesTotalAge, primarySpeciesYearsToBreastHeight, primarySpeciesYearsAtBreastHeight
		);
	}

	static float calculatePrimarySpeciesDominantHeight(
			Region primarySpeciesRegion, MatrixMap2<String, Region, Coefficients> hl1Coefficients, Bank bank,
			int primarySpeciesIndex
	) throws ProcessingException {
		// (1) Dominant Height
		float primarySpeciesDominantHeight = bank.dominantHeights[primarySpeciesIndex];
		if (Float.isNaN(primarySpeciesDominantHeight)) {
			float loreyHeight = bank.loreyHeights[primarySpeciesIndex][UC_ALL_INDEX];

			if (Float.isNaN(loreyHeight)) {
				throw new ProcessingException(
						MessageFormat.format(
								"Neither dominant nor lorey height[All] is available for primary species {0}",
								bank.speciesNames[primarySpeciesIndex]
						), 2
				);
			}

			// Estimate dominant height from the lorey height
			String primarySpeciesAlias = bank.speciesNames[primarySpeciesIndex];

			var coefficients = hl1Coefficients.get(primarySpeciesAlias, primarySpeciesRegion);
			float a0 = coefficients.getCoe(1);
			float a1 = coefficients.getCoe(2);
			float a2 = coefficients.getCoe(3);

			float treesPerHectare = bank.treesPerHectare[primarySpeciesIndex][UC_ALL_INDEX];
			float hMult = a0 - a1 + a1 * FloatMath.exp(a2 * (treesPerHectare - 100.0f));

			primarySpeciesDominantHeight = 1.3f + (loreyHeight - 1.3f) / hMult;
		}
		return primarySpeciesDominantHeight;
	}

	static Optional<Integer> findIndexInOneIndexedFloatArray(Optional<Integer> tryFirst, float[] array, int arraySize) {
		return tryFirst.filter(index -> !Float.isNaN(array[index])).or(() -> {
			for (int i = 1; i <= arraySize; i++) {
				if (!Float.isNaN(array[i])) {
					return Optional.of(i);
				}
			}
			return Optional.empty();
		});
	}

}
