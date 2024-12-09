package ca.bc.gov.nrs.vdyp.backend.projection.model;

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

	/** The individual site species */
	List<SiteSpeciesComponent> siteSpeciesArray;

	/**
	 * An array holding the indices to the individual elements in <code>siteSpeciesArray</code> in sorted order.
	 */
	int siteSpeciesSorting;

	/** If one exists, this member points to the site species corresponding to the C/Y Species Group. */
	SiteSpeciesComponent siteSpeciesC;

	/** If one exists, this member points to the site species corresponding to the PA/PL Species Group. */
	SiteSpeciesComponent siteSpeciesP;
}
