package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Identifies the different severities a message may have.
 */
public enum MessageSeverityCode {
	/**
	 * Provides information regarding some aspect of the application. This is not an error.
	 */
	INFORMATION,

	/**
	 * Provides some information as the status of an operation or of the library.
	 */
	STATUS,

	/**
	 * Indicates some missing or invalid information was supplied but did not prevent the application from continuing.
	 */
	WARNING,

	/**
	 * Indicates some condition exists that prevented the operation from completing.
	 */
	ERROR,

	/**
	 * Some condition exists that prevents this and potentially preventing future operations from continuing.
	 */
	FATAL_ERROR
}
