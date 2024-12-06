package ca.bc.gov.nrs.vdyp.backend.projection.model;

/** Holds the relevant details of a particular site species. */
public class SiteSpeciesComponent {

	/** The Species Group (Sp0) component corresponding to the site species. */
	StandComponent speciesGroupComponent;

	/** <code>true</code> iff relevant site information has been applied to the current site species component. */
	boolean hasSiteInfo;

	/**
	 * <code>true</code> iff this entry has been combined with a different SP0 to produce a composite SP0 Site Species.
	 */
	boolean hasBeenCombined;

	/** The total stand percent for this particular site species component. */
	double totalSpeciesPercent;

	/** Indicates the Species Group sorting order of the original stand from which this entry comes. */
	int speciesGroupSortingOrder;
}
