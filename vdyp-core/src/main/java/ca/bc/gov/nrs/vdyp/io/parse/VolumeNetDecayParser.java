package ca.bc.gov.nrs.vdyp.io.parse;

/**
 * Parse a datafile with volume net decay coefficients
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VolumeNetDecayParser extends OptionalCoefficientParser2<Integer, Integer> {

	public static final String CONTROL_KEY = "VOLUME_NET_DECAY";
	public static final int MAX_GROUPS = 80;

	public VolumeNetDecayParser() {
		super(1, CONTROL_KEY);
		this.ucIndexKey().space(1).groupIndexKey(MAX_GROUPS).coefficients(4, 10);
	}

}
