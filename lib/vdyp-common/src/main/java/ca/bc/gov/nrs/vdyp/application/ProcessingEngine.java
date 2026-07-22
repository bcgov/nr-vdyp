package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.model.VdypEntity.MISSING_FLOAT_VALUE;

import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
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
	protected static void estimateMissingSiteIndices(LayerProcessingState<?> lps) throws ProcessingException {

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

	protected static float estimateMissingNonPrimarySiteIndices(
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

	protected static float estimateMissingNonPrimarySiteIndex(
			SiteIndexEquation pspSiteCurve, Bank bank, float pspSiteIndex, int spIndex, final int siteCurveNumber
	) throws ProcessingException {
		float spSiteIndex = bank.siteIndices[spIndex];
		if (Float.isNaN(spSiteIndex)) {
			SiteIndexEquation spSiteCurve = SiteIndexEquation.getByIndex(siteCurveNumber);

			try {
				double mappedSiteIndex = SiteTool
						.convertSiteIndexBetweenCurves(pspSiteCurve, pspSiteIndex, spSiteCurve);
				if (mappedSiteIndex > 1.3f) {
					pspSiteIndex = (float) mappedSiteIndex;
				}
				if (mappedSiteIndex > 0.0f) {
					bank.siteIndices[spIndex] = (float) mappedSiteIndex;
				}
			} catch (NoAnswerException e) {
				logger.warn(
						MessageFormat.format(
								"there is no conversion between curves {0} and {1}. Not calculating site index for species {2}",
								pspSiteCurve, spSiteCurve, bank.speciesNames[spIndex]
						)
				);
			} catch (CurveErrorException | SpeciesErrorException e) {
				throw new ProcessingException(
						MessageFormat.format(
								"convertSiteIndexBetweenCurves on {0}, {1} and {2} failed", pspSiteCurve,
								pspSiteIndex, spSiteCurve
						), e
				);
			}
		}
		return pspSiteIndex;
	}

	protected static float estimateMissingPrimarySiteIndex(
			LayerProcessingState<?> lps, int pspIndex, SiteIndexEquation pspSiteCurve
	) throws ProcessingException {
		Bank bank = lps.getBank();
		if (Float.isNaN(bank.siteIndices[pspIndex])) {

			double otherSiteIndicesSum = 0.0f;
			int nOtherSiteIndices = 0;

			for (int spIndex : lps.getIndices()) {

				if (spIndex == pspIndex) {
					continue;
				}

				float spSiteIndex = bank.siteIndices[spIndex];

				if (!Float.isNaN(spSiteIndex)) {
					SiteIndexEquation spSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex));

					try {
						double mappedSiteIndex = SiteTool
								.convertSiteIndexBetweenCurves(spSiteCurve, spSiteIndex, pspSiteCurve);
						if (mappedSiteIndex > 1.3) {
							otherSiteIndicesSum += mappedSiteIndex;
							nOtherSiteIndices += 1;
						}
					} catch (NoAnswerException e) {
						logger.warn(
								MessageFormat.format(
										"there is no conversion from curves {0} to {1}. Excluding species {2}"
												+ " from the estimation of the site index of {3}",
										spSiteCurve, pspSiteCurve, bank.speciesNames[spIndex],
										bank.speciesNames[pspIndex]
								)
						);
					} catch (CurveErrorException | SpeciesErrorException e) {
						throw new ProcessingException(
								MessageFormat.format(
										"convertSiteIndexBetweenCurves on {0}, {1} and {2} failed", spSiteCurve,
										spSiteIndex, pspSiteCurve
								), e
						);
					}
				}
			}

			if (nOtherSiteIndices > 0) {
				bank.siteIndices[pspIndex] = (float) (otherSiteIndicesSum / nOtherSiteIndices);
			}
		}

		return bank.siteIndices[pspIndex];

	}

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
	protected static void estimateMissingSiteIndicesAndAgesExtended(
			LayerProcessingState<?> lps, ProcessingDebugSettings debugSettings
	)
			throws ProcessingException {

		Bank bank = lps.getBank();

		int pspIndex = lps.getPrimarySpeciesIndex();
		int nSpecies = lps.getNSpecies(); // Should correspond to NSPL1.
		SiteIndexEquation pspSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(pspIndex));

		// TODO these slots and values deserve names and documentation. For now, the Fortran code and the above mapping
		// notes are the best guide to their meaning.
		for (int debugSlot = 11; debugSlot <= 20; debugSlot++) {
			int choice = debugSettings.getValue(debugSlot);

			if (choice == 0) {
				break;
			}

			if (choice == 1 || (choice == 15 && !Float.isNaN(bank.ageTotals[pspIndex])
					&& bank.ageTotals[pspIndex] > 0.0f && bank.ageTotals[pspIndex] < 30.0f)) {

				/*
				 * Assign primary site index from conversion of another site index. If that doesn't work, directly move
				 * a site index. Order to check: secondary species first, then species order.
				 */

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

							SiteIndexEquation fromCurve = SiteIndexEquation.getByIndex(spCurveNo);

							try {
								double mapped = SiteTool
										.convertSiteIndexBetweenCurves(fromCurve, spSiteIndex, pspSiteCurve);
								if (mapped > 0.0) {
									unusedSetUsableSiteIndex = (float) mapped;
									break;
								}
							} catch (NoAnswerException e) {
								// Fortran just keeps searching. No warning there.
							} catch (CurveErrorException | SpeciesErrorException e) {
								throw new ProcessingException(
										"Failed converting site index to primary species curve", e
								);
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

			} else if (choice == 2) {

				// Use primary site index to set all other site indices where possible.
				if (bank.siteIndices[pspIndex] > 0.0f && nSpecies > 1) {
					float pspSiteIndex = bank.siteIndices[pspIndex];

					for (int spIndex : lps.getIndices()) {
						if (spIndex == pspIndex || bank.siteIndices[spIndex] > 0.0f) {
							continue;
						}

						SiteIndexEquation spCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex));

						try {
							double mapped = SiteTool.convertSiteIndexBetweenCurves(pspSiteCurve, pspSiteIndex, spCurve);
							if (mapped > 0.0) {
								bank.siteIndices[spIndex] = (float) mapped;
							}
						} catch (NoAnswerException e) {
							// Fortran silently ignores failure here.
						} catch (CurveErrorException | SpeciesErrorException e) {
							throw new ProcessingException(
									"Failed converting primary site index to another species curve", e
							);
						}
					}
				}

			} else if (choice == 3) {
				// When 2 of (TOTAGE, BHAGE, YTBH) are present, fill in the 3rd with algebra.
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

			} else if (choice == 4) {

				// Move total age from non-primary to primary species. Try secondary first, then any species.
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

			} else if (choice == 5 || choice == 6) {

				/*
				 * Estimate dominant height from Lorey height. case 5 is primary species, case 6 non primary species
				 */
				for (int spIndex : lps.getIndices()) {
					if (!Float.isNaN(bank.dominantHeights[spIndex]) && bank.dominantHeights[spIndex] > 0.0f) {
						continue;
					}

					boolean applies = (choice == 5 && spIndex == pspIndex) || (choice == 6 && spIndex != pspIndex);

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

			} else if (choice >= 7 && choice <= 10) {

				/*
				 * Estimate SI from dominant height and age. 7 primary species, total age 8 primary species, BH age 9
				 * non-primary species, total age 10 non-primary species, BH age
				 */
				boolean ageAtBreastHeight = (choice == 8 || choice == 10);

				for (int spIndex : lps.getIndices()) {

					if (bank.siteIndices[spIndex] > 0.0f) {
						continue;
					}

					boolean applies = (spIndex == pspIndex && choice <= 8) || (spIndex != pspIndex && choice > 8);

					if (!applies) {
						continue;
					}

					if (Float.isNaN(bank.dominantHeights[spIndex]) || bank.dominantHeights[spIndex] <= 0.0f) {
						continue;
					}

					float age = ageAtBreastHeight ? bank.yearsAtBreastHeight[spIndex] : bank.ageTotals[spIndex];

					if (Float.isNaN(age) || age <= 0.0f) {
						continue;
					}

					try {
						double siteIndex = SiteTool.heightAndAgeToSiteIndex(
								SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex)), age,
								ageAtBreastHeight ? SiteIndexAgeType.SI_AT_BREAST : SiteIndexAgeType.SI_AT_TOTAL,
								bank.dominantHeights[spIndex], SiteIndexEstimationType.SI_EST_DIRECT
						);

						if (siteIndex > 0.0) {
							bank.siteIndices[spIndex] = (float) siteIndex;

							if (Float.isNaN(bank.yearsToBreastHeight[spIndex])
									|| bank.yearsToBreastHeight[spIndex] <= 0.0f) {
								double ytbh = SiteTool.yearsToBreastHeight(
										SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex)), siteIndex
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

			} else if (choice == 11 || choice == 12) {

				/*
				 * Estimate ages from dominant height and SI.
				 */
				for (int spIndex : lps.getIndices()) {

					boolean applies = (choice == 11 && spIndex == pspIndex) || (choice == 12 && spIndex != pspIndex);

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
						// TODO I have changed this pretty considerably from VDYP7 make sure it is acceptable
						if (Float.isNaN(bank.yearsAtBreastHeight[spIndex])
								|| bank.yearsAtBreastHeight[spIndex] <= 0.0f) {
							bank.yearsAtBreastHeight[spIndex] = (float) SiteTool.heightAndSiteIndexToAge(
									SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex)),
									bank.dominantHeights[spIndex], SiteIndexAgeType.SI_AT_BREAST,
									bank.siteIndices[spIndex], bank.yearsToBreastHeight[spIndex]
							);
						}
						if (Float.isNaN(bank.ageTotals[spIndex]) || bank.ageTotals[spIndex] <= 0.0f) {
							bank.ageTotals[spIndex] = (float) SiteTool.heightAndSiteIndexToAge(
									SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex)),
									bank.dominantHeights[spIndex], SiteIndexAgeType.SI_AT_TOTAL,
									bank.siteIndices[spIndex], bank.yearsToBreastHeight[spIndex]
							);
						} else {
							bank.yearsAtBreastHeight[spIndex] = bank.ageTotals[spIndex]
									- bank.yearsToBreastHeight[spIndex];
						}

					} catch (Exception e) {
						throw new ProcessingException("Failed estimating ages from height and site index", e);
					}

				}

			} else if (choice == 13 || choice == 14) {

				// Calculate YTBH from SI.
				for (int spIndex : lps.getIndices()) {

					boolean applies = (choice == 13 && spIndex == pspIndex) || (choice == 14 && spIndex != pspIndex);

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
						double ytbh = SiteTool.yearsToBreastHeight(
								SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex)), bank.siteIndices[spIndex]
						);
						if (ytbh > 0.0) {
							bank.yearsToBreastHeight[spIndex] = (float) ytbh;
						}
					} catch (Exception e) {
						throw new ProcessingException("Failed estimating years to breast height from site index", e);
					}
				}
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

}
