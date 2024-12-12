package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/** Lists the growth models availble to be used to start the stand model. */
public enum GrowthModel {
	
	/** Defines that the stand characteristics has FIPSTART properties. */
	FIP,

	/** Defines that the stand characteristics has VRISTART properties. */
	VRI,
	
	UNKNOWN;

	public static GrowthModel getDefault() {
		return UNKNOWN;
	}
}
