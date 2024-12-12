package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Lists all possible processing modes when running one of the two stand models.
 */
public enum ProcessingMode {
	/**
	 * Specifies that the underlying growth model will not process the stand. This seems a useless processing mode.
	 */
	FIP_DoNotProcess,

	/**
	 * The underlying FIP model determines which processing mode to use based on the supplied stand characteristics.
	 */
	FIP_Default,

	/**
	 * A specific processing mode for the FIPSTART stand model.
	 */
	FIP_FipStart,

	/**
	 * A specific processing mode for the FIPSTART stand model.
	 */
	FIP_FipYoung,

	/**
	 * Specifies that the underlying growth model will not process the stand. This seems a useless processing mode.
	 */
	VRI_DoNotProcess,

	/**
	 * The underlying VRI model determines which processing mode to use based on the supplied stand characteristics.
	 */
	VRI_Default, 

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_VriStart, 

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_VriYoung, 

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_Minimal, 
	
	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_CrownClosure;

	public static ProcessingMode getDefault() {
		return VRI_Default;
	}
}
