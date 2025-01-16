package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;

public enum SpecialLayerType {
	PRIMARY("P", ProjectionTypeCode.PRIMARY),
	VETERAN("V", ProjectionTypeCode.VETERAN),
	REGEN("Y", ProjectionTypeCode.REGENERATION),
	RESIDUAL("R", ProjectionTypeCode.RESIDUAL),
	DEAD("D", ProjectionTypeCode.DEAD);

	public final String code;
	public final ProjectionTypeCode projectionType;
	
	SpecialLayerType(String code, ProjectionTypeCode projectionType) {
		this.code = code;
		this.projectionType = projectionType;
	}
	
	/**
	 * Convert from a ProjectionType to a SpecialLayerCode.
	 * @param projectionType the projection type to be mapped
	 * @return the equivalent SpecialLayerType
	 * @see ProjectionType
	 */
	public SpecialLayerType fromProjectionType(ProjectionTypeCode projectionType) {
		
		for (var e: SpecialLayerType.values()) {
			if (e.projectionType.equals(projectionType)) {
				return e;
			}
		}
		
		throw new IllegalStateException(MessageFormat.format("ProjectionType {0} does not have an equivalent SpecialLayerType", projectionType));
	}

	/**
	 * Return <code>true</code> iff <code>candidate</code> is the text of the <code>code</code>
	 * of a SpecialLayerType.
	 * @param candidate the candidate text
	 * @return as described
	 */
	public static boolean doesLayerIdIdentifyASpecialLayerType(String candidate) {
		for (SpecialLayerType e: SpecialLayerType.values()) {
			if (e.code.equals(candidate))
				return true;
		}
		return false;
	}
}
