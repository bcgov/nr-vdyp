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
	 * @param bank         the bank in which the calculations are done.
	 * @param siteCurveMap the Site Curve definitions.
	 * @param lps          the PolygonProcessingState to where the calculated curves are also to be
	 */
	protected static void calculateMissingSiteCurves(
			LayerProcessingState<?> lps, MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap
	) {
		Bank bank = lps.getBank();

		BecDefinition becZone = bank.getBecZone();

		for (int i : bank.getIndices()) {

			if (bank.siteCurveNumbers[i] == VdypEntity.MISSING_INTEGER_VALUE) {

				Optional<SiteIndexEquation> scIndex = Optional.empty();

				Optional<Sp64Distribution> sp0Dist = bank.sp64Distributions[i].getSpeciesDistribution(1);

				// First alternative is to use the name of the first of the species' sp64Distributions
				if (sp0Dist.isPresent()) {
					if (!siteCurveMap.isEmpty()) {
						scIndex = Utils.optSafe(siteCurveMap.get(sp0Dist.get().getGenusAlias(), becZone.getRegion()));
					} else {
						SiteIndexEquation siCurve = SiteTool
								.getSICurve(bank.speciesNames[i], becZone.getRegion().equals(Region.COASTAL));
						scIndex = siCurve == SiteIndexEquation.SI_NO_EQUATION ? Optional.empty() : Optional.of(siCurve);
					}
				}

				// Second alternative is to use the species name as given in the species' "speciesName" field
				if (scIndex.isEmpty()) {
					String sp0 = bank.speciesNames[i];
					if (!siteCurveMap.isEmpty()) {
						scIndex = Utils.optSafe(siteCurveMap.get(sp0, becZone.getRegion()));
					} else {
						SiteIndexEquation siCurve = SiteTool
								.getSICurve(sp0, becZone.getRegion().equals(Region.COASTAL));
						scIndex = siCurve == SiteIndexEquation.SI_NO_EQUATION ? Optional.empty() : Optional.of(siCurve);
					}
				}

				bank.siteCurveNumbers[i] = scIndex.orElseThrow().n();
			}
		}

		lps.setSiteCurveNumbers(bank.siteCurveNumbers);
	}

}
