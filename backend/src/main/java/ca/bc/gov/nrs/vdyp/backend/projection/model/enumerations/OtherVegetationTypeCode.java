package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;

/**
 * Describes other classes of vegetation that may reside on a polygon that are not trees.
 */
public enum OtherVegetationTypeCode {

	Shrub(1), //
	Herb(2), //
	Bryoid(3);

	private final int code;

	OtherVegetationTypeCode(int code) {
		this.code = code;
	}

	public static OtherVegetationTypeCode fromCode(Short code) {
		if (code != null) {
			for (OtherVegetationTypeCode e : values()) {
				if (e.code == code) {
					return e;
				}
			}
		}

		throw new IllegalArgumentException(MessageFormat.format("\"{0}\" is not a known OtherVegetationType", code));
	}

	public static OtherVegetationTypeCode getDefault() {
		return Shrub;
	}
}
