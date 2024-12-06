package ca.bc.gov.nrs.vdyp.backend.projection.model;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.SilviculturalBase;

/** Records information about disturbances to the polygon. */
public class History {
	
	/** Identifies the Silvicultural Base code that occurred. */
	private SilviculturalBase silvicultureBase;
	
	/** The year the disturbance started. */
	private int disturbanceStartYear;
	
	/** The year the disturbance ended. */
	private int disturbanceEndYear;
	
	/** The percent of the layer that was disturned. */
	private double percent;
}
