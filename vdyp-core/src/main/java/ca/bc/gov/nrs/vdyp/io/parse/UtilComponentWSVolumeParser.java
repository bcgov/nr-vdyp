package ca.bc.gov.nrs.vdyp.io.parse;

public class UtilComponentWSVolumeParser extends OptionalCoefficientParser2<Integer, Integer> {

	public static final int MAX_GROUPS = 80;
	public static final String CONTROL_KEY = "UTIL_COMP_WS_VOLUME";

	public UtilComponentWSVolumeParser() {
		super(0, CONTROL_KEY);
		this.ucIndexKey().space(1).groupIndexKey(MAX_GROUPS).coefficients(4, 10);
	}

}