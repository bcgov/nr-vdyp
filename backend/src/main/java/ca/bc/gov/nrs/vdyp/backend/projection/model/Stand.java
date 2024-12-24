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
	
	/** The species group's Sp0 Species. */
	private Species speciesGroup;

	/** The VDYP7 index for this species group. */
	private int speciesGroupIndex;

	/** The species of the individual species (VDYP7: Sp64) of the species group. */
	private List<Species> species = new ArrayList<Species>();
	
	private Stand() {
	}
	
	public Layer getParentComponent() {
		return parentComponent;
	}
	
	public int getSpeciesGroupIndex() {
		return speciesGroupIndex;
	}
	
	public Species getSpeciesGroup() {
		return speciesGroup;
	}
	
	public List<Species> getSpecies() {
		return species;
	}
	
	public static class Builder {
		private Stand stand = new Stand();
			
		public Builder parentComponent(Layer parentComponent) {
			stand.parentComponent = parentComponent;
			return this;
		}
		
		// NOTE: speciesGroup and speciesGroupIndex are added post-construction, in updateAfterSpeciesGroupAdded
		
		public Builder species(List<Species> species) {
			stand.species = species;
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
		String sp0Name = SiteTool.getSpeciesShortName(getSpeciesGroupIndex());
		boolean isDeciduous = SiteTool.getIsDeciduous(getSpeciesGroupIndex());

		if (isDeciduous || "PL".equals(sp0Name) || "PA".equals(sp0Name)) {
			return 81.0;
		} else {
			return 121.0;
		}
	}

	/**
	 * A species (sp64) has been added to the stand. Update the Stand to reflect this.
	 * 
	 * @param species the species that was added.
	 */
	public void updateAfterSpeciesAdded(Species species) {
		speciesGroup.updateAfterSpeciesAdded(species);
	}

	public void updateAfterSpeciesGroupAdded(Species speciesInstance) {
		speciesGroup = speciesInstance;
		speciesGroupIndex = SiteTool.getSpeciesIndex(speciesInstance.getSpeciesCode());
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
		return parentComponent.hashCode() * 17 + speciesGroup.hashCode();
	}
	
	@Override
	public int compareTo(Stand that) {
		int layerComparisonResult = this.parentComponent.compareTo(that.parentComponent);
		if (layerComparisonResult == 0) {
			return this.speciesGroup.getSpeciesCode().compareTo(that.speciesGroup.getSpeciesCode());
		} else {
			return layerComparisonResult;
		}
	}
	
	@Override 
	public String toString() {
		return parentComponent.toString() + ":" + speciesGroupIndex;
	}
}