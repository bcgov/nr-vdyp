package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes, for a projection, which type of layer the projection represents.
 */
public enum ProjectionTypeCode {

	/** The projection is for the stand's primary layer. */
	PRIMARY("Primary", Vdyp7LayerTypeCode.PRIMARY.code),

	/** The projection is for the stand's veteran layer. */
	VETERAN("Veteran", Vdyp7LayerTypeCode.VETERAN.code),

	/** The projection is for the stand's residual layer. */
	RESIDUAL("Residual", Vdyp7LayerTypeCode.RESIDUAL.code),

	/** The projection is for the stand's regeneration layer. */
	REGENERATION("Regeneration", Vdyp7LayerTypeCode.REGEN.code),

	/** The projection is for the stand's dead stem layer. */
	DEAD("Dead", Vdyp7LayerTypeCode.DEAD.code),

	/** A live layer that should not be projected. */
	DO_NOT_PROJECT("Do Not Project", null),

	/** The unknown layer type */
	UNKNOWN("Unknown", null);

	/**
	 * All projection types that may be associated with a layer that will be projected. ORDER MATTERS - do NOT change.
	 */
	public final static List<ProjectionTypeCode> ACTUAL_PROJECTION_TYPES_LIST = Collections
			.unmodifiableList(List.of(PRIMARY, VETERAN, RESIDUAL, REGENERATION, DEAD));

	public final static Set<ProjectionTypeCode> ACTUAL_PROJECTION_TYPES_SET = Collections
			.unmodifiableSet(new HashSet<>(ACTUAL_PROJECTION_TYPES_LIST));

	public final String name;
	public final String specialLayerTypeCodeText;

	ProjectionTypeCode(String name, String specialLayerTypeCodeText) {
		this.name = name;
		this.specialLayerTypeCodeText = specialLayerTypeCodeText;
	}

	/**
	 * Convert from a <code>SpecialLayerType</code>'s code to a ProjectionTypeCode.
	 *
	 * @param t the Vdyp7LayerType's code
	 * @return the equivalent ProjectionTypeCode
	 * @see Vdyp7LayerTypeCode
	 */
	public static ProjectionTypeCode fromVdyp7LayerTypeText(String vdyp7LayerTypeText) {

		if (vdyp7LayerTypeText != null) {
			for (var e : ProjectionTypeCode.values()) {
				if (vdyp7LayerTypeText.equals(e.specialLayerTypeCodeText)) {
					return e;
				}
			}
		}

		throw new IllegalStateException(
				MessageFormat.format(
						"Vdyp7LayerType code {0} does not have an equivalent ProjectionType", vdyp7LayerTypeText
				)
		);
	}

	/**
	 * Convert from a SpecialLayerType to a ProjectionTypeCode.
	 *
	 * @param t the Vdyp7LayerType's code
	 * @return the equivalent ProjectionTypeCode
	 * @see Vdyp7LayerTypeCode
	 */
	public static ProjectionTypeCode fromVdyp7LayerType(Vdyp7LayerTypeCode t) {

		return fromVdyp7LayerTypeText(t.code);
	}
}
