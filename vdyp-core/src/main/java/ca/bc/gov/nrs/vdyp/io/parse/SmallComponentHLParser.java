package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class SmallComponentHLParser extends SimpleCoefficientParser1<String> {

	public static final String CONTROL_KEY = "SMALL_COMP_HL";

	public SmallComponentHLParser(Map<String, Object> control) {
		super(String.class, 1);
		this.speciesKey(BaseCoefficientParser.SP0_KEY, control);
		this.coefficients(2, 10);
	}

}
