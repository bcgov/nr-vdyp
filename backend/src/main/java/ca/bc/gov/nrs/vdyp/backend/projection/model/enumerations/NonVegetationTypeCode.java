package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/** Describes classes of non-vegetative outcroppings that occur on a polygon. */
public enum NonVegetationTypeCode {
	
	Water("LA"), //
	ExposedSoil("ES"), //
	BurnedArea("BU"), //
	Rock(""), //
	Snow(""), //
	Other("");

	private String code;
	
	NonVegetationTypeCode(String code) {
		this.code = code;
	}
	
	public static NonVegetationTypeCode fromCode(String code) {
		
		if (code != null && !code.isBlank()) {
			code = code.trim();
			for (NonVegetationTypeCode e: values()) {
				if (e.code.equals(code)) {
					return e;
				}
			}
		}
		
		return getDefault();
	}
	
	public static NonVegetationTypeCode getDefault() { 
		return Other;
	}
}
