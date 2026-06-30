package ca.bc.gov.nrs.vdyp.application;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;

public class ProcessingEngine {

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
		BecDefinition becZone = bank.getBecZone();

		for (int i : bank.getIndices()) {

			if (bank.siteCurveNumbers[i] == VdypEntity.MISSING_INTEGER_VALUE) {

				Optional<Sp64Distribution> sp0Dist = bank.sp64Distributions[i].getSpeciesDistribution(1);
				final String speciesId = bank.speciesNames[i];

				Optional<SiteIndexEquation> scIndex = calculateMissingSiteCurve(
						siteCurveMap, becZone, sp0Dist, speciesId
				);

				bank.siteCurveNumbers[i] = scIndex.orElseThrow().n();
			}
		}

	}

	/**
	 * Calculate the siteCurve number of a species group.
	 *
	 * @param siteCurveMap the Site Curve definitions.
	 * @param becZone      the bec zone of the polygon
	 * @param sp0Dist      The species distribution
	 * @param speciesId    The species group to be calculated
	 * @return
	 */
	protected static Optional<SiteIndexEquation> calculateMissingSiteCurve(
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap, BecDefinition becZone,
			Optional<Sp64Distribution> sp0Dist, final String speciesId
	) {
		return sp0Dist
				// First alternative is to use the name of the first of the species' sp64Distributions
				.flatMap(dist -> calculateMissingSiteCurve(siteCurveMap, becZone, speciesId, dist.getGenusAlias()))
				// Second alternative is to use the species name as given in the species' "speciesName" field
				.or(() -> calculateMissingSiteCurve(siteCurveMap, becZone, speciesId, speciesId));
	}

	/**
	 * Calculate the siteCurve number of a species group.
	 *
	 * @param siteCurveMap the Site Curve definitions.
	 * @param becZone      the bec zone of the polygon
	 * @param speciesId    The species group to be used if the map is empty
	 * @param sp0          The species group to be used if the map is not empty
	 * @return
	 */
	protected static Optional<SiteIndexEquation> calculateMissingSiteCurve(
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap, BecDefinition becZone, final String speciesId,
			String sp0
	) {
		Optional<SiteIndexEquation> scIndex;
		if (!siteCurveMap.isEmpty()) {
			scIndex = Utils.optSafe(siteCurveMap.get(sp0, becZone.getRegion()));
		} else {
			SiteIndexEquation siCurve = SiteTool.getSICurve(speciesId, becZone.getRegion().equals(Region.COASTAL));
			scIndex = siCurve == SiteIndexEquation.SI_NO_EQUATION ? Optional.empty() : Optional.of(siCurve);
		}
		return scIndex;
	}

}
