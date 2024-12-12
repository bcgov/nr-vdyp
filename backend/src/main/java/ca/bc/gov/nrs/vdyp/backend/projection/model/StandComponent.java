package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.List;

/** 
 * Holds all of the information regarding a single Species Group (Sp0 in VDYP7) component. 
 */
public class StandComponent {

	/** The VDYP7 index for this species group. */
	private int speciesGroupIndex;

	/** The species group's SpeciesComponent. */
	private SpeciesComponent speciesGroupSpeciesComponent;

	/** The SpeciesComponents of the individual species (VDYP7: Sp64) of the species group. */
	private List<SpeciesComponent> speciesSpeciesComponent;
}
