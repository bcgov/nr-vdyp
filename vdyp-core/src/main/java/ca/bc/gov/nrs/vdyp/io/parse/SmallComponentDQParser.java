package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class SmallComponentDQParser extends SimpleCoefficientParser1<String> {

	public static final String CONTROL_KEY = "SMALL_COMP_DQ";

	public SmallComponentDQParser(Map<String, Object> control) {
		super(String.class, 1);
		this.speciesKey();
		this.coefficients(2, 10);
	}

}
