package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Describes, for a projection, which type of layer the projection represents.
 */
public enum ProjectionType {
	/** The projection is for the stand's primary layer. */
	Primary,
	/** The projection is for the stand's veteran layer. */
	Veteran,
	/** The projection is for the stand's residual layer. */
	Residual,
	/** The projection is for the stand's regeneration layer. */
	Regeneration,
	/** The projection is for the stand's dead stem layer. */
	Dead,
	/** A live layer that should not be projected. */
	DoNoProject;
}
