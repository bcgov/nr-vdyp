package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class SmallComponentBaseAreaParser extends SimpleCoefficientParser1<String> {

	public static final String CONTROL_KEY = "SMALL_COMP_BA";

	public SmallComponentBaseAreaParser(Map<String, Object> control) {
		super(String.class, 1);
		this.speciesKey();
		this.coefficients(4, 10);
	}

}
