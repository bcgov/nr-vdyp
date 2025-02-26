package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Enumerates all of the different Error Message types that can be generated and returned by this library.
 */
public enum MessageCode {
	/**
	 * Indicates that there is no message. Usually used as a place holder or to indicate success.
	 */
	NONE,

	/**
	 * An internal error indicating the polygon context has been corrupted somehow. Hopefully the text of the message
	 * will give some indication as to what the problem could be.
	 */
	CORRUPT_CONTEXT,

	/** An message returned by the VDYP7CORE library. */
	VDYP7CORE_MSG,

	/** BEC Zone is required but was not supplied. */
	NO_BEC,

	/** The Start Model (generally FIPSTART or VRISTART) was not recognized. */
	BAD_START_MODEL,
	/** A layer that could be classified as a primary layer was not supplied. */
	NO_PRIMARY_LAYER,

	/** No crown closure is available nor derivable from stand information. */
	NO_CC,

	/** The default Crown Closure was used. */
	DEFAULT_CC,

	/** Lorey height at the 7.5cm+ level was not supplied. */
	NO_LH075,

	/** Basal area at the 7.5cm+ level was not supplied. */
	NO_BA075,

	/** Basal area at the 12.5cm+ level was not supplied. */
	NO_BA125,

	/** Whole Stem Volume 7.5cm+ level was not supplied. */
	NO_WSV075,

	/** Whole Stem Volume 12.5cm+ level was not supplied. */
	NO_WSV125,

	/** Close Utilization Volume 12.5cm+ was not supplied. */
	NO_VCU125,

	/** Volume Net Decay 12.5cm+ was not supplied. */
	NO_VD125,

	/** Volume Net Decay and Waste 12.5cm+ was not supplied. */
	NO_VDW125,

	/** A cross utilization level constraint was violated. */
	UTIL_CONSTRAINT,

	/** A cross volume type constraint was violated. */
	VOL_CONSTRAINT,

	/** The primary layer is labelled as Non-Productive and has no stand description. */
	NONPROD_PRIMARY,

	/** The stand was unable to grow to a reasonable height within a reasonable period of time. */
	LOW_SITE,

	/** The stand projection failed. */
	PROJECTION_FAILED,

	/**
	 * The stand did not reach a sufficient height by the target projection year to generate valid projection values.
	 */
	TOO_SHORT,

	/** A duplicate species was encountered. */
	DUP_SPCS,

	/**
	 * The layer has a mixture of young and older species. This is not necessarily an error and will not necessarily be
	 * reported each time it occurs.
	 */
	YOUNG_OLD_SPCS_MIX,

	/** A required/expected species for a layer was not found. */
	NO_SPCS,

	/** A catch all message indicating something was missing or inconsistent with the stand definition. */
	BAD_STAND_DEFN,

	/** Indicates a problem was encountered with the age range of the projection. */
	AGE_RANGE,

	/** Indicates Site Index was reassigned. */
	REASSIGNED_SITE,

	/** Indicates Input Height was reassigned. */
	REASSIGNED_HEIGHT
}
