package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

/**
 * Holds all of the information regarding a single Species Group (Sp0 in VDYP7) component.
 */
public class Stand implements Comparable<Stand> {

	/** The parent Layer */
	private Layer layer;

	/** The SP0 species code of the stand */
	private String sp0Code;

	/** The species group's Sp0 Species. */
	private Species sp0;

	/** The number of stands added to the Layer prior to this one. */
	private int standIndex;

	/** The individual species (VDYP7: Sp64) of the species group, as ordered on input */
	private List<Species> sp64s = new ArrayList<Species>();

	/** The individual species (VDYP7: Sp64) of the species group, ordered by decreasing percentage */
	private TreeSet<Species> sp64sByPercentage = new TreeSet<>(new Species.ByDecreasingPercentageComparator());

	private Stand() {
	}

	public Layer getLayer() {
		return layer;
	}

	public String getSp0Code() {
		return sp0Code;
	}

	public int getStandIndex() {
		return standIndex;
	}

	public Species getSpeciesGroup() {
		return sp0;
	}

	public List<Species> getSpeciesByPercent() {
		return sp64sByPercentage.stream().toList();
	}

	public List<Species> getSpecies() {
		return Collections.unmodifiableList(sp64s);
	}

	public static class Builder {
		private Stand stand = new Stand();

		// NOTE: sp0, speciesGroupIndex and the individual sp64s are added post-construction

		public Builder layer(Layer layer) {
			stand.layer = layer;
			return this;
		}

		public Builder sp0Code(String sp0Code) {
			stand.sp0Code = sp0Code;
			return this;
		}

		public Stand build() {

			if (stand.layer == null) {
				throw new IllegalArgumentException("Attempted to create a Stand with no parent Layer");
			}

			if (stand.sp0Code == null) {
				throw new IllegalArgumentException("Attempted to create a Stand with no sp0Code value");
			}

			return stand;
		}
	}

	/**
	 * Return the <code>n</code>th species (Sp64) of the layer from a list sorted by <code>criteria</code>.
	 *
	 * @param n        the zero-based index
	 * @param criteria the sorting criteria
	 * @return as described
	 */
	public Double determineMaturityAge() {

		String sp0Name = SiteTool.getSpeciesShortName(getStandIndex());
		boolean isDeciduous = SiteTool.getIsDeciduous(getStandIndex());

		if (isDeciduous || "PL".equals(sp0Name) || "PA".equals(sp0Name)) {
			return 81.0;
		} else {
			return 121.0;
		}
	}

	/**
	 * Add a species (sp64) to the Stand.
	 *
	 * @param sp64 the species to be added.
	 */
	public void addSp64(Species sp64) {

		if (sp0 == null) {
			throw new IllegalStateException("Stand.updateAfterSp64Added: sp0 is null - call addSpeciesGroup first");
		}

		this.sp0.updateAfterSp64Added(sp64);
		this.sp64sByPercentage.add(sp64);
		this.sp64s.add(sp64);
	}

	/**
	 * A species group (sp0) has been added to the stand. Update the stand to reflect this.
	 *
	 * @param speciesInstance the sp64 instance being added as the species group to this Stand
	 * @param index           the order the sp0 appeared in the containing layer
	 */
	public void addSpeciesGroup(Species speciesInstance, int index) {

		int sp64Index = SiteTool.getSpeciesIndex(sp0Code);
		String sp64Code = SiteTool.getSpeciesShortName(sp64Index);

		this.sp0 = new Species.Builder() //
				.stand(this) //
				.ageAtBreastHeight(speciesInstance.getAgeAtBreastHeight()) //
				.dominantHeight(speciesInstance.getDominantHeight()) //
				.totalAge(speciesInstance.getTotalAge()) //
				.siteCurve(speciesInstance.getSiteCurve()) //
				.siteIndex(speciesInstance.getSiteIndex()) //
				.speciesCode(sp64Code) //
				.speciesPercent(0.0) //
				.build();

		this.standIndex = index;
	}

	public static class ByIncreasingNameComparator implements Comparator<Stand> {
		@Override
		public int compare(Stand o1, Stand o2) {
			return o1.getSp0Code().compareTo(o2.getSp0Code());
		}
	}

	public static class ByDecreasingPercentageComparator implements Comparator<Stand> {
		@Override
		public int compare(Stand o1, Stand o2) {
			int result = signum(o2.getSpeciesGroup().getSpeciesPercent() - o1.getSpeciesGroup().getSpeciesPercent());
			if (result == 0) {
				return o1.getStandIndex() - o2.getStandIndex();
			} else {
				return result;
			}
		}

		private int signum(double d) {
			return d > 0 ? 1 : d < 0 ? -1 : 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Stand that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return layer.hashCode() * 17 + sp0.hashCode();
	}

	@Override
	public int compareTo(Stand that) {
		int layerComparisonResult = this.layer.compareTo(that.layer);
		if (layerComparisonResult == 0) {
			return this.sp0.getSpeciesCode().compareTo(that.sp0.getSpeciesCode());
		} else {
			return layerComparisonResult;
		}
	}

	@Override
	public String toString() {
		return layer.toString() + ":" + sp0Code;
	}
}
