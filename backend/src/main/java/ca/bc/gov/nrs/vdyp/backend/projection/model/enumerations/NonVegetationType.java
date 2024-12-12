package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/** Describes classes of non-vegetative outcroppings that occur on a polygon. */
public enum NonVegetationType {
	
	Water("LA"), //
	ExposedSoil("ES"), //
	BurnedArea("BU"), //
	Rock(""), //
	Snow(""), //
	Other("");

	private String code;
	
	NonVegetationType(String code) {
		this.code = code;
	}
	
	public static NonVegetationType fromCode(String code) {
		
		if (code != null && !code.isBlank()) {
			code = code.trim();
			for (NonVegetationType e: values()) {
				if (e.code.equals(code)) {
					return e;
				}
			}
		}
		
		return getDefault();
	}
	
	public static NonVegetationType getDefault() { 
		return Other;
	}
}
