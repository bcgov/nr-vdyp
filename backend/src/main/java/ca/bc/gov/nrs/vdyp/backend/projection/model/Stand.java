package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

/** 
 * Holds all of the information regarding a single Species Group (Sp0 in VDYP7) component. 
 */
public class Stand implements Comparable<Stand> {

	/** The parent Layer */
	private Layer parentComponent;
	
	/** The SP0 species code of the stand */
	private String sp0Code;
	
	/** The species group's Sp0 Species. */
	private Species sp0;

	/** The number of stands added to the Layer prior to this one. */
	private int standIndex;

	/** The species of the individual species (VDYP7: Sp64) of the species group. */
	private List<Species> sp64s = new ArrayList<Species>();
	
	private Stand() {
	}
	
	public Layer getParentComponent() {
		return parentComponent;
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
	
	public List<Species> getSpecies() {
		return sp64s;
	}
	
	public static class Builder {
		private Stand stand = new Stand();
			
		public Builder parentComponent(Layer parentComponent) {
			stand.parentComponent = parentComponent;
			return this;
		}
		
		// NOTE: speciesGroup and speciesGroupIndex are added post-construction, in updateAfterSpeciesGroupAdded
		
		public Builder species(List<Species> species) {
			stand.sp64s = species;
			return this;
		}
		
		public Builder sp0Code(String sp0Code) {
			stand.sp0Code = sp0Code;
			return this;
		}
		
		public Builder layer(Layer layer) {
			stand.parentComponent = layer;
			return this;
		}
		
		public Stand build() {

			if (stand.parentComponent == null) {
				throw new IllegalArgumentException("Attempted to create a Stand with no parent Layer");
			}
			
			return stand;
		}
	}

	/**
	 * Return the <code>n</code>th species (Sp64) of the layer from a list sorted by <code>criteria</code>.
	 * 
	 * @param n the zero-based index
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
	 * A species (sp64) has been added to the stand. Update the Stand to reflect this.
	 * 
	 * @param sp64 the species that was added.
	 */
	public void updateAfterSp64Added(Species sp64) {
		this.sp0.updateAfterSp64Added(sp64);
		this.sp64s.add(sp64);
	}

	/**
	 * A species group (sp0) has been added to the stand. Update the stand to reflect this.
	 *
	 * @param speciesInstance the sp64 instance being added as the species group to this Stand
	 * @param index the order the sp0 appeared in the containing layer
	 */
	public void addSpeciesGroup(Species speciesInstance, int index) {
		
		int sp64Index = SiteTool.getSpeciesIndex(sp0Code);
		String sp64Code = SiteTool.getSpeciesShortName(sp64Index);
		
		this.sp0 = new Species.Builder() //
				.parentComponent(this) //
				.ageAtBreastHeight(speciesInstance.getAgeAtBreastHeight()) //
				.dominantHeight(speciesInstance.getDominantHeight()) //
				.totalAge(speciesInstance.getTotalAge()) //
				.siteCurve(speciesInstance.getSiteCurve()) //
				.siteIndex(speciesInstance.getSiteIndex()) //
				.speciesCode(sp64Code) //
				.speciesPercent(0.0) //
				.suppliedDominantHeight(speciesInstance.getSuppliedDominantHeight()) //
				.suppliedSiteIndex(speciesInstance.getSiteIndex()) //
				.suppliedTotalAge(speciesInstance.getTotalAge()) //
				.build();
				
		this.standIndex = index;
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
		return parentComponent.hashCode() * 17 + sp0.hashCode();
	}
	
	@Override
	public int compareTo(Stand that) {
		int layerComparisonResult = this.parentComponent.compareTo(that.parentComponent);
		if (layerComparisonResult == 0) {
			return this.sp0.getSpeciesCode().compareTo(that.sp0.getSpeciesCode());
		} else {
			return layerComparisonResult;
		}
	}
	
	@Override 
	public String toString() {
		return parentComponent.toString() + ":" + sp0Code;
	}
}