package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * Describes, for a projection, which type of layer the projection represents.
 */
public enum ProjectionTypeCode {
	/** The projection is for the stand's primary layer. */
	PRIMARY(SpecialLayerType.PRIMARY.code),
	/** The projection is for the stand's veteran layer. */
	VETERAN(SpecialLayerType.VETERAN.code),
	/** The projection is for the stand's residual layer. */
	RESIDUAL(SpecialLayerType.RESIDUAL.code),
	/** The projection is for the stand's regeneration layer. */
	REGENERATION(SpecialLayerType.REGEN.code),
	/** The projection is for the stand's dead stem layer. */
	DEAD(SpecialLayerType.DEAD.code),
	/** A live layer that should not be projected. */
	DO_NOT_PROJECT(null),
	/** The unknown layer type */
	UNKNOWN(null);

	public final static List<ProjectionTypeCode> ACTUAL_PROJECTION_TYPES_LIST =
			Collections.unmodifiableList(List.of(PRIMARY, VETERAN, RESIDUAL, REGENERATION, DEAD));
	
	public final String specialLayerTypeCodeText;
	
	ProjectionTypeCode(String specialLayerTypeCodeText) {
		this.specialLayerTypeCodeText = specialLayerTypeCodeText;
	}
	
	/**
	 * Convert from a <code>SpecialLayerType</code>'s code to a ProjectionTypeCode.
	 * @param t the SpecialLayerType's code
	 * @return the equivalent ProjectionTypeCode
	 * @see SpecialLayerType
	 */
	public static ProjectionTypeCode fromSpecialLayerTypeText(String specialLayerTypeText) {
		
		if (specialLayerTypeText != null) {
			for (var e: ProjectionTypeCode.values()) {
				if (specialLayerTypeText.equals(e.specialLayerTypeCodeText)) {
					return e;
				}
			}
		}
		
		throw new IllegalStateException(MessageFormat.format("SpecialLayerType code {0} does not have an equivalent ProjectionType", specialLayerTypeText));
	}
	
	/**
	 * Convert from a SpecialLayerType to a ProjectionTypeCode.
	 * @param t the SpecialLayerType's code
	 * @return the equivalent ProjectionTypeCode
	 * @see SpecialLayerType
	 */
	public static ProjectionTypeCode fromSpecialLayerType(SpecialLayerType t) {
		
		return fromSpecialLayerTypeText(t.code);
	}
}
