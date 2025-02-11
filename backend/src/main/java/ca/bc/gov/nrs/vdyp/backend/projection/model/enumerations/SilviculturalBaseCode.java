package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * This enumeration classifies the disturbances that occur in History records.
 */
public enum SilviculturalBaseCode {
	/** The layer was disturbed by some natural or man made activity. */
	DISTURBED,

	/** The layer was disturbed by an insect attack. */
	INSECT_ATTACK,

	/** Site preparation activities have occurred. */
	SITE_PREPARATION,

	/** A tree planting occurred. */
	PLANTING,

	/** Stand tending activities have occurred. */
	STAND_TENDING,

	/** Some unknown disturbance occurred. */
	UNKNOWN;

	public static SilviculturalBaseCode getDefault() {
		
		return UNKNOWN;
	}
}
