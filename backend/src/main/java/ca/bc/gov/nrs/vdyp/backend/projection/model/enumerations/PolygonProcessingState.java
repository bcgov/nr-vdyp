package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

public enum PolygonProcessingState {

	/**
	 * The polygon properties are currently being defined. (Ex: layers are being added or the species composition is
	 * being set.
	 */
	DEFINING_POLYGON,

	/**
	 * The polygon has been completely defined but no externally requested processing has occurred.
	 *
	 * In this state, layers will have been combined and other internal processing will have been performed.
	 */
	POLYGON_DEFINED,

	/**
	 * Initial processing has occurred on the polygon. It is now possible to define adjustments or to project the
	 * polygon.
	 */
	INITIALLY_PROCESSED,

	/**
	 * The polygon has been projected to a specified year.
	 */
	PROJECTED,
	
	/**
	 * Some processing error has occurred placing the polygon in an unrecoverable state. 
	 */
	ERROR
}
