package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.SpeciesProjectionType;

/**
 * Holds all the information surrounding a single species component.
 */
public class SpeciesComponent {

	/** Points to the StandComponent corresponding to the species. */
	private StandComponent parentComponent;

	/** An index to a specific species code as returned by the SiteTools library. */
	private int iSpcsCode;
	
	private double fSpcsPcnt;
	private double fTotalAge;
	private double fBreastHeightAge;
	private double fDominantHeight;
	private double fSI;
	private double fYTBH;
	private int iSiteCurve;

	private double fSuppliedTotalAge;
	private double fSuppliedDominantHeight;
	private double fSuppliedSI;

	private int iNumTimesSupplied;

	/**
	 * The index to the next duplicated instance of this species for each of the requested Projected Values by Species.
	 * On input, multiple examples of a single species may be supplied as input. This index scroll through each instance
	 * and wrap around back to the first when duplicates are exhausted.
	 *
	 * This mechanism assumes that the requester is making species requests for species in the same order as they were
	 * supplied.
	 */
	private Map<SpeciesProjectionType, Integer> iNextDupNdx;

	/**
	 * For each of the times this species was supplied, this array holds the individual species percents that were
	 * supplied. This is necessary because when projected results are requested, those results will have to be prorated
	 * by the input percent.
	 */
	private Map<Integer /* duplicate number */, Double> percentsPerDuplicate;
}
