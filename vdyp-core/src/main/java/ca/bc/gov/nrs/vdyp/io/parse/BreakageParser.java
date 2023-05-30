package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class BreakageParser extends SimpleCoefficientParser1<Integer> {

	public static final String CONTROL_KEY = "BREAKAGE";

	public BreakageParser(Map<String, Object> control) {
		super(Integer.class, 1);
		this.groupIndexKey(40);
		this.coefficients(6, 9);
	}
}
