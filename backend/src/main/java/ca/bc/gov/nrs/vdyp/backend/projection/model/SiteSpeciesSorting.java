package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.Comparator;
import java.util.List;

/**
 * This structure maintains the sorting of site species.
 * <p>
 * The major difference between Site Species Ordering and Species Group Ordering by Percent is that for Site Species,
 * all Species Groups (Sp0s) corresponding to Cedar and Yellow Cedar (C/Y Sp0 codes) are combined into one Site Species
 * Group. The overall site species corresponding to this pair belongs to the Sp0 encountered first in the array.
 * <p>
 * This same consideration also occurs for PA/PL Sp0 groups.
 * <p>
 * When sorting, sort order is given in the following priority:
 * <ol>
 * <li>The site species has site information.
 * <li>The site species with the higher Species Percent.
 * </ol>
 * <p>
 * A tie means the first encountered SP0 takes precedence.
 */
public class SiteSpeciesSorting {

	/** The individual site species, sorted by the individual SiteSpecies's sorting orders. */
	private List<SiteSpecies> siteSpeciesArray;

	/** If one exists, this member points to the site species corresponding to the C/Y Species Group. */
	private SiteSpecies siteSpeciesC;

	/** If one exists, this member points to the site species corresponding to the PA/PL Species Group. */
	private SiteSpecies siteSpeciesP;

	private SiteSpeciesSorting() {
	}

	public List<SiteSpecies> getSiteSpeciesArray() {
		return siteSpeciesArray;
	}

	public SiteSpecies getSiteSpeciesC() {
		return siteSpeciesC;
	}

	public SiteSpecies getSiteSpeciesP() {
		return siteSpeciesP;
	}

	public static class Builder {
		private SiteSpeciesSorting SiteSpeciesSorting = new SiteSpeciesSorting();

		public Builder setSiteSpeciesArray(List<SiteSpecies> siteSpeciesArray) {

			siteSpeciesArray.sort(new Comparator<SiteSpecies>() {
				@Override
				public int compare(SiteSpecies o1, SiteSpecies o2) {
					return o1.compareTo(o2);
				}
			});
			
			SiteSpeciesSorting.siteSpeciesArray = siteSpeciesArray;

			return this;
		}

		public Builder setSiteSpeciesC(SiteSpecies siteSpeciesC) {
			SiteSpeciesSorting.siteSpeciesC = siteSpeciesC;
			return this;
		}

		public Builder setSiteSpeciesP(SiteSpecies siteSpeciesP) {
			SiteSpeciesSorting.siteSpeciesP = siteSpeciesP;
			return this;
		}
	}
}
