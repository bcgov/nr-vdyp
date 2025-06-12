package ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations;

import java.text.MessageFormat;

public enum Vdyp7LayerTypeCode {
	PRIMARY("P"), //
	VETERAN("V"), //
	REGEN("Y"), //
	RESIDUAL("R"), //
	DEAD("D");

	public final String code;

	Vdyp7LayerTypeCode(String code) {
		this.code = code;
	}

	/**
	 * Convert from a String code to a Vdyp7LayerTypeCode.
	 *
	 * @param vdypCode the projection type to be mapped
	 * @return the equivalent Vdyp7LayerTypeCode
	 */
	public static Vdyp7LayerTypeCode fromCode(String vdypCode) {
		for (var e : Vdyp7LayerTypeCode.values()) {
			if (e.code.equals(vdypCode)) {
				return e;
			}
		}

		throw new IllegalArgumentException(
				MessageFormat.format("Code {0} does not have an equivalent Vdyp7LayerTypeCode", vdypCode)
		);
	}

	/**
	 * Return <code>true</code> iff <code>candidate</code> is the text of the <code>code</code> of a SpecialLayerType.
	 *
	 * @param candidate the candidate text
	 * @return as described
	 */
	public static boolean isLayerIdAVdyp7LayerType(String candidate) {
		for (Vdyp7LayerTypeCode e : Vdyp7LayerTypeCode.values()) {
			if (e.code.equals(candidate))
				return true;
		}
		return false;
	}
}
