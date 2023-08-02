package ca.bc.gov.nrs.vdyp.io.parse;

/**
 * Parse a datafile with close util volume coefficients
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class CloseUtilVolumeParser extends OptionalCoefficientParser2<Integer, Integer> {

	public static final String CONTROL_KEY = "CLOSE_UTIL_VOLUME";
	public static final int MAX_GROUPS = 80;

	public CloseUtilVolumeParser() {
		super(1, CONTROL_KEY);
		this.ucIndexKey().space(1).groupIndexKey(MAX_GROUPS).coefficients(4, 10);
	}

}
