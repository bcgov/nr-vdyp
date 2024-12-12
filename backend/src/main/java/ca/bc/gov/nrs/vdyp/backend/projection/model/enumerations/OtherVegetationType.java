package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;

/**
 * Describes other classes of vegetation that may reside on a polygon that are not trees.
 */
public enum OtherVegetationType {
	
	Shrub(1), //
	Herb(2), //
	Bryoid(3);

	private final int code;
	
	OtherVegetationType(int code) {
		this.code = code;
	}
	
	public static OtherVegetationType fromCode(Short code) {
		if (code != null) {
			for (OtherVegetationType e: values()) {
				if (e.code == code) {
					return e;
				}
			}
		}
		
		throw new IllegalArgumentException(MessageFormat.format("\"{0}\" is not a known OtherVegetationType", code));
	}
	
	public static OtherVegetationType getDefault() {
		return Shrub;
	}
}
