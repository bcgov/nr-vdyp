package ca.bc.gov.nrs.vdyp.io.parse;

public class BreakageParser extends SimpleCoefficientParser1<Integer> {

	public static final int MAX_GROUPS = 40;
	public static final String CONTROL_KEY = "BREAKAGE";

	public BreakageParser() {
		super(Integer.class, 1, CONTROL_KEY);
		this.groupIndexKey(MAX_GROUPS);
		this.coefficients(6, 9);
	}
}
