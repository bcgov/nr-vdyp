package ca.bc.gov.nrs.vdyp.backend.projection.model;

/** Holds the relevant details of a particular site species. */
public class SiteSpecies implements Comparable<SiteSpecies> {

	/** The Species Group (Sp0) component corresponding to the site species. */
	private Stand speciesGroupComponent;

	/** <code>true</code> iff relevant site information has been applied to the current site species component. */
	private boolean hasSiteInfo;

	/**
	 * <code>true</code> iff this entry has been combined with a different SP0 to produce a composite SP0 Site Species.
	 */
	private boolean hasBeenCombined;

	/** The total stand percent for this particular site species component. */
	private double totalSpeciesPercent;

	/** Indicates the Species Group sorting order of the original stand from which this entry comes. */
	private int speciesGroupSortingOrder;
	
	private SiteSpecies() {
	}

	public Stand getSpeciesGroupComponent() {
		return speciesGroupComponent;
	}

	public boolean isHasSiteInfo() {
		return hasSiteInfo;
	}

	public boolean isHasBeenCombined() {
		return hasBeenCombined;
	}

	public double getTotalSpeciesPercent() {
		return totalSpeciesPercent;
	}

	public int getSpeciesGroupSortingOrder() {
		return speciesGroupSortingOrder;
	}

	public class Builder {
		private SiteSpecies siteSpecies = new SiteSpecies();

		public Builder setSpeciesGroupComponent(Stand speciesGroupComponent) {
			siteSpecies.speciesGroupComponent = speciesGroupComponent;
			return this;
		}
	
		public Builder setHasSiteInfo(boolean hasSiteInfo) {
			siteSpecies.hasSiteInfo = hasSiteInfo;
			return this;
		}
	
		public Builder setHasBeenCombined(boolean hasBeenCombined) {
			siteSpecies.hasBeenCombined = hasBeenCombined;
			return this;
		}
	
		public Builder setTotalSpeciesPercent(double totalSpeciesPercent) {
			siteSpecies.totalSpeciesPercent = totalSpeciesPercent;
			return this;
		}
	
		public Builder setSpeciesGroupSortingOrder(int speciesGroupSortingOrder) {
			siteSpecies.speciesGroupSortingOrder = speciesGroupSortingOrder;
			return this;
		}
	}

	@Override
	public int compareTo(SiteSpecies that) {
		return this.speciesGroupSortingOrder - that.speciesGroupSortingOrder;
	}	
}
