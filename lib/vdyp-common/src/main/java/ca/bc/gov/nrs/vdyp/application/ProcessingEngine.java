package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.log;
import static ca.bc.gov.nrs.vdyp.model.VdypEntity.MISSING_FLOAT_VALUE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.ReconcilationMethods;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.PrimarySpeciesDetails;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.sindex.Reference;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEstimationType;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.SpeciesErrorException;

public class ProcessingEngine<S extends ProcessingState<L>, L extends LayerProcessingState<L>> {

	private static final Logger logger = LoggerFactory.getLogger(ProcessingEngine.class);
	protected static final int UC_ALL_INDEX = UtilizationClass.ALL.ordinal();
	protected static final int UC_SMALL_INDEX = UtilizationClass.SMALL.ordinal();
	private static final float[] DEFAULT_QUAD_MEAN_DIAMETERS = new float[] { Float.NaN, 10.0f, 15.0f, 20.0f, 25.0f };
	private static final float V_BASE_MIN = 0.1f;
	private static final float B_BASE_MIN = 0.01f;

	public ProcessingEngine(S processingState) {
		this.state = processingState;
	}

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

	private final S state;

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

	/**
	 * CVSET1 - computes cvVolume, cvBasalArea, cvQuadraticMeanDiameter and cvSmall and assigns them to the current
	 * LayerProcessingState.
	 *
	 * @throws ProcessingException
	 */
	@SuppressWarnings("unchecked")
	protected void setCompatibilityVariables() throws ProcessingException {

		Coefficients aAdjust = new Coefficients(new float[] { 0.0f, 0.0f, 0.0f, 0.0f }, 1);

		var growthDetails = getState().getControlMap().getControlVariables();
		var lps = getState().getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		// Note: L1COM2 (INL1VGRP, INL1DGRP, INL1BGRP) is initialized when
		// PolygonProcessingState (volumeEquationGroups, decayEquationGroups
		// breakageEquationGroups, respectively) is constructed. Copying
		// the values into LCOM1 is not necessary. Note, however, that
		// VolumeEquationGroup 10 is mapped to 11 (VGRPFIND) - this is done
		// when volumeEquationGroups is built (i.e., when the equivalent to
		// INL1VGRP is built, rather than when LCOM1 VGRPL is built in the
		// original code.)

		var cvVolume = new MatrixMap3[lps.getNSpecies() + 1];
		var cvBasalArea = new ca.bc.gov.nrs.vdyp.model.MatrixMap2[lps.getNSpecies() + 1];
		var cvQuadraticMeanDiameter = new ca.bc.gov.nrs.vdyp.model.MatrixMap2[lps.getNSpecies() + 1];
		var cvSmall = new HashMap[lps.getNSpecies() + 1];

		for (int s : lps.getIndices()) {

			String genusName = bank.speciesNames[s];

			float spLoreyHeight_All = bank.loreyHeights[s][UtilizationClass.ALL.ordinal()];

			UtilizationVector basalAreas = Utils.utilizationVector();
			UtilizationVector wholeStemVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecay = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecayAndWaste = Utils.utilizationVector();
			UtilizationVector quadMeanDiameters = Utils.utilizationVector();
			UtilizationVector treesPerHectare = Utils.utilizationVector();

			cvVolume[s] = new MatrixMap3Impl<UtilizationClass, VolumeVariable, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0f
			);
			cvBasalArea[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);
			cvQuadraticMeanDiameter[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);

			for (UtilizationClass uc : UtilizationClass.ALL_BUT_SMALL) {

				basalAreas.setCoe(uc.index, bank.basalAreas[s][uc.ordinal()]);
				wholeStemVolumes.setCoe(uc.index, bank.wholeStemVolumes[s][uc.ordinal()]);
				closeUtilizationVolumes.setCoe(uc.index, bank.closeUtilizationVolumes[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecay.setCoe(uc.index, bank.cuVolumesMinusDecay[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecayAndWaste
						.setCoe(uc.index, bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()]);

				quadMeanDiameters.setCoe(uc.index, bank.quadMeanDiameters[s][uc.ordinal()]);
				if (uc != UtilizationClass.ALL && quadMeanDiameters.getCoe(uc.index) <= 0.0f) {
					quadMeanDiameters.setCoe(uc.index, DEFAULT_QUAD_MEAN_DIAMETERS[uc.ordinal()]);
				}
			}

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {

				float adjustment;
				float baseVolume;

				// Volume less decay and waste
				adjustment = 0.0f;
				baseVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP094
					getState().getEstimators().estimateNetDecayAndWasteVolume(
							lps.getBecZone().getRegion(), uc, aAdjust, bank.speciesNames[s], spLoreyHeight_All,
							quadMeanDiameters, closeUtilizationVolumes, closeUtilizationVolumesNetOfDecay,
							closeUtilizationVolumesNetOfDecayAndWaste
					);

					float actualVolume = bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecayAndWaste.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s]
						.put(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY, adjustment);

				// Volume less decay
				adjustment = 0.0f;
				baseVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP093
					int decayGroup = lps.getDecayEquationGroups()[s];
					getState().getEstimators().estimateNetDecayVolume(
							bank.speciesNames[s], lps.getBecZone().getRegion(), uc, aAdjust, decayGroup,
							lps.getPrimarySpeciesAgeAtBreastHeight(), quadMeanDiameters, closeUtilizationVolumes,
							closeUtilizationVolumesNetOfDecay
					);

					float actualVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecay.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s].put(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY, adjustment);

				// Volume
				adjustment = 0.0f;
				baseVolume = bank.wholeStemVolumes[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP092
					int volumeGroup = lps.getVolumeEquationGroups()[s];
					getState().getEstimators().estimateCloseUtilizationVolume(
							uc, aAdjust, volumeGroup, spLoreyHeight_All, quadMeanDiameters, wholeStemVolumes,
							closeUtilizationVolumes
					);

					float actualVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumes.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s].put(uc, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, adjustment);
			}

			int primarySpeciesVolumeGroup = lps.getVolumeEquationGroups()[s];
			float primarySpeciesQMDAll = bank.quadMeanDiameters[s][UC_ALL_INDEX];
			var wholeStemVolume = bank.treesPerHectare[s][UC_ALL_INDEX] * getState().getEstimators()
					.estimateWholeStemVolumePerTree(primarySpeciesVolumeGroup, spLoreyHeight_All, primarySpeciesQMDAll);

			wholeStemVolumes.setCoe(UC_ALL_INDEX, wholeStemVolume);

			getState().getEstimators().estimateWholeStemVolume(
					UtilizationClass.ALL, 0.0f, primarySpeciesVolumeGroup, spLoreyHeight_All, quadMeanDiameters,
					basalAreas, wholeStemVolumes
			);

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float adjustment = 0.0f;
				float basalArea = basalAreas.getCoe(uc.index);
				if (growthDetails.allowCalculation(basalArea, B_BASE_MIN, (l, r) -> l > r)) {
					adjustment = calculateWholeStemVolume(
							bank.wholeStemVolumes[s][uc.ordinal()], basalArea, wholeStemVolumes.getCoe(uc.index)
					);
				}

				cvVolume[s].put(uc, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, adjustment);
			}

			getState().getEstimators()
					.estimateQuadMeanDiameterByUtilization(lps.getBecZone(), quadMeanDiameters, genusName);

			getState().getEstimators()
					.estimateBaseAreaByUtilization(lps.getBecZone(), quadMeanDiameters, basalAreas, genusName);

			// Calculate trees-per-hectare per utilization
			treesPerHectare.setCoe(UtilizationClass.ALL.index, bank.treesPerHectare[s][UC_ALL_INDEX]);
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				treesPerHectare.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreas.getCoe(uc.index), quadMeanDiameters.getCoe(uc.index))
				);
			}

			ReconcilationMethods.reconcileComponents(basalAreas, treesPerHectare, quadMeanDiameters);

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float baCvValue = bank.basalAreas[s][uc.ordinal()] - basalAreas.getCoe(uc.index);
				cvBasalArea[s].put(uc, LayerType.PRIMARY, baCvValue);

				float originalQmd = bank.quadMeanDiameters[s][uc.ordinal()];
				float adjustedQmd = quadMeanDiameters.getCoe(uc.index);

				float qmdCvValue;
				if (growthDetails.allowCalculation(() -> bank.basalAreas[s][uc.ordinal()] < B_BASE_MIN)) {
					qmdCvValue = 0.0f;
				} else if (originalQmd > 0 && adjustedQmd > 0) {
					qmdCvValue = originalQmd - adjustedQmd;
				} else {
					qmdCvValue = 0.0f;
				}

				cvQuadraticMeanDiameter[s].put(uc, LayerType.PRIMARY, qmdCvValue);
			}

			// Small components

			cvSmall[s] = calculateSmallCompatibilityVariables(s, growthDetails);
		}

		lps.setCompatibilityVariableDetails(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvSmall);
	}

	/**
	 * Function that calculates values for the small component compatibility variables and returns the result.
	 *
	 * @param speciesIndex            the index of the species for which this operation is to be performed
	 * @param forwardControlVariables the control variables for this run
	 *
	 * @throws ProcessingException
	 */
	private HashMap<UtilizationClassVariable, Float>
			calculateSmallCompatibilityVariables(int speciesIndex, ProcessingControlVariables forwardControlVariables) {

		final L lps = getState().getPrimaryLayerProcessingState();
		final Bank bank = lps.getBank();
		final EstimationMethods estimators = getState().getEstimators();

		final Region region = lps.getBecZone().getRegion();
		final String speciesName = bank.speciesNames[speciesIndex];

		final float spLoreyHeight_All = bank.loreyHeights[speciesIndex][UC_ALL_INDEX]; // HLsp
		final float spQuadMeanDiameter_All = bank.quadMeanDiameters[speciesIndex][UC_ALL_INDEX]; // DQsp

		// this WHOLE operation on Actual BA's, not 100% occupancy.
		// TODO: verify this: float fractionAvailable = polygon.getPercentForestLand();
		final float spBaseArea_All = bank.basalAreas[speciesIndex][UC_ALL_INDEX] /* * fractionAvailable */;

		// EMP080
		final float smallProbability = estimators.estimateSmallComponentProbability(
				speciesName, //
				lps.getBank().yearsAtBreastHeight[lps.getPrimarySpeciesIndex()], //
				spLoreyHeight_All, //
				region
		); // PROBsp

		// EMP081
		final float conditionalExpectedBaseArea = estimators.estimateSmallComponentConditionalExpectedBasalArea(
				speciesName, spBaseArea_All, spLoreyHeight_All, region
		); // BACONDsp

		// TODO (see previous TODO): conditionalExpectedBaseArea /= fractionAvailable;

		final float spBaSmall = smallProbability * conditionalExpectedBaseArea;

		// EMP082
		final float spDqSmall = estimators.estimateSmallComponentQuadMeanDiameter(speciesName, spLoreyHeight_All); // DQSMsp

		// EMP085
		final float spLhSmall = estimators
				.estimateSmallComponentLoreyHeight(speciesName, spLoreyHeight_All, spDqSmall, spQuadMeanDiameter_All); // HLSMsp

		// EMP086
		final float spMeanVolumeSmall = estimators.estimateMeanVolumeSmall(speciesName, spLhSmall, spDqSmall); // VMEANSMs

		final var spCvSmall = new HashMap<UtilizationClassVariable, Float>();

		final float spInputBasalArea_Small = bank.basalAreas[speciesIndex][UC_SMALL_INDEX];
		spCvSmall.put(UtilizationClassVariable.BASAL_AREA, spInputBasalArea_Small - spBaSmall);

		if (forwardControlVariables.allowCalculation(spInputBasalArea_Small, B_BASE_MIN, (l, r) -> l > r)) {
			final float spInputQuadMeanDiameter_Small = bank.quadMeanDiameters[speciesIndex][UC_SMALL_INDEX];
			spCvSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, spInputQuadMeanDiameter_Small - spDqSmall);
		} else {
			spCvSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f);
		}

		final float spInputLoreyHeight_Small = bank.loreyHeights[speciesIndex][UC_SMALL_INDEX];
		if (spInputLoreyHeight_Small > 1.3f && spLhSmall > 1.3f && spInputBasalArea_Small > 0.0f) {
			final float cvLoreyHeight = FloatMath.log( (spInputLoreyHeight_Small - 1.3f) / (spLhSmall - 1.3f));
			spCvSmall.put(UtilizationClassVariable.LOREY_HEIGHT, cvLoreyHeight);
		} else {
			spCvSmall.put(UtilizationClassVariable.LOREY_HEIGHT, 0.0f);
		}

		final float spInputWholeStemVolume_Small = bank.wholeStemVolumes[speciesIndex][UC_SMALL_INDEX];
		if (spInputWholeStemVolume_Small > 0.0f && spMeanVolumeSmall > 0.0f
				&& forwardControlVariables.allowCalculation(spInputBasalArea_Small, B_BASE_MIN, (l, r) -> l >= r)) {

			final float spInputTreePerHectare_Small = bank.treesPerHectare[speciesIndex][UC_SMALL_INDEX];

			final var spWsVolumeSmall = FloatMath
					.log(spInputWholeStemVolume_Small / spInputTreePerHectare_Small / spMeanVolumeSmall);
			spCvSmall.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, spWsVolumeSmall);

		} else {
			spCvSmall.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f);
		}

		return spCvSmall;
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

	private static float calculateCompatibilityVariable(float actualVolume, float baseVolume, float staticVolume) {

		float staticRatio = staticVolume / baseVolume;
		float staticLogit;
		if (staticRatio <= 0.0f) {
			staticLogit = -7.0f;
		} else if (staticRatio >= 1.0f) {
			staticLogit = 7.0f;
		} else {
			staticLogit = clamp(log(staticRatio / (1.0f - staticRatio)), -7.0f, 7.0f);
		}

		float actualRatio = actualVolume / baseVolume;
		float actualLogit;
		if (actualRatio <= 0.0f) {
			actualLogit = -7.0f;
		} else if (actualRatio >= 1.0f) {
			actualLogit = 7.0f;
		} else {
			actualLogit = clamp(log(actualRatio / (1.0f - actualRatio)), -7.0f, 7.0f);
		}

		return actualLogit - staticLogit;
	}

	private static float calculateWholeStemVolume(float actualVolume, float basalArea, float staticVolume) {

		float staticRatio = staticVolume / basalArea;
		float staticLogit;
		if (staticRatio <= 0.0f) {
			staticLogit = -2.0f;
		} else {
			staticLogit = log(staticRatio);
		}

		float actualRatio = actualVolume / basalArea;
		float actualLogit;
		if (actualRatio <= 0.0f) {
			actualLogit = -2.0f;
		} else {
			actualLogit = log(actualRatio);
		}

		return actualLogit - staticLogit;
	}

	public S getState() {
		return state;
	}

}
