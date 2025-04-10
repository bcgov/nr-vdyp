package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/** Lists the growth models availble to be used to start the stand model. */
public enum GrowthModelCode {

	/** Defines that the stand characteristics has FIPSTART properties. */
	FIP,

	/** Defines that the stand characteristics has VRISTART properties. */
	VRI,

	UNKNOWN;

	public static GrowthModelCode getDefault() {
		return VRI;
	}
}
