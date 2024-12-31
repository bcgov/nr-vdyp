package ca.bc.gov.nrs.vdyp.backend.projection.model;

/** Holds the relevant details of a particular site species. */
public class SiteSpecies implements Comparable<SiteSpecies> {

	/** The Species Group (Sp0) component corresponding to the site species. */
	private Stand stand;

	/** <code>true</code> iff relevant site information has been applied to the current site species component. */
	private boolean hasSiteInfo;

	/**
	 * <code>true</code> iff this entry has been combined with a different SP0 to produce a composite SP0 Site Species.
	 */
	private boolean hasBeenCombined;

	/** The total stand percent for this particular site species component. */
	private double totalSpeciesPercent;

	private SiteSpecies() {
	}

	public Stand getStand() {
		return stand;
	}

	public boolean getHasSiteInfo() {
		return hasSiteInfo;
	}

	public boolean getHasBeenCombined() {
		return hasBeenCombined;
	}

	public double getTotalSpeciesPercent() {
		return totalSpeciesPercent;
	}

	public static class Builder {
		private SiteSpecies siteSpecies = new SiteSpecies();

		public Builder stand(Stand stand) {
			siteSpecies.stand = stand;
			return this;
		}
	
		public Builder hasSiteInfo(boolean hasSiteInfo) {
			siteSpecies.hasSiteInfo = hasSiteInfo;
			return this;
		}
	
		public Builder hasBeenCombined(boolean hasBeenCombined) {
			siteSpecies.hasBeenCombined = hasBeenCombined;
			return this;
		}
	
		public Builder totalSpeciesPercent(double totalSpeciesPercent) {
			siteSpecies.totalSpeciesPercent = totalSpeciesPercent;
			return this;
		}
	
		public SiteSpecies build() {
			return siteSpecies;
		}
	}

	public void incrementTotalSpeciesPercent(double speciesPercent) {
		totalSpeciesPercent += speciesPercent;
		hasBeenCombined = true;
	}	
	
	@Override
	public int compareTo(SiteSpecies that) {
		int result = (int)Math.signum(that.getTotalSpeciesPercent() - this.getTotalSpeciesPercent());
		if (result == 0) {
			result = this.getStand().getStandIndex() - that.getStand().getStandIndex();
		} 
		return result;
	}
	
	@Override
	public String toString() {
		return stand.getSpeciesGroup().getSpeciesCode() + ": Combined? " + (hasBeenCombined ? "Y" : "N")
			+ " " + totalSpeciesPercent + "%";
	}
}
