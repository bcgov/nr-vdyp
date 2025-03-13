package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Lists all possible processing modes when running one of the two stand models.
 */
public enum ProcessingModeCode {

	/**
	 * Specifies that the underlying growth model will not process the stand. This seems a useless processing mode.
	 */
	FIP_DoNotProcess(-1),

	/**
	 * The underlying FIP model determines which processing mode to use based on the supplied stand characteristics.
	 */
	FIP_Default(0),

	/**
	 * A specific processing mode for the FIPSTART stand model.
	 */
	FIP_FipStart(1),

	/**
	 * A specific processing mode for the FIPSTART stand model.
	 */
	FIP_FipYoung(2),

	/**
	 * Specifies that the underlying growth model will not process the stand. This seems a useless processing mode.
	 */
	VRI_DoNotProcess(-1),

	/**
	 * The underlying VRI model determines which processing mode to use based on the supplied stand characteristics.
	 */
	VRI_Default(0),

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_VriStart(1),

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_VriYoung(2),

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_Minimal(3),

	/**
	 * A specific processing mode for the VRISTART stand model.
	 */
	VRI_CrownClosure(4);

	public final int value;

	private ProcessingModeCode(int value) {
		this.value = value;
	}

	public static ProcessingModeCode getDefault() {
		return VRI_Default;
	}
}
