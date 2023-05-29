package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class SmallComponentWSVolumeParser extends SimpleCoefficientParser1<String> {

	public static final String CONTROL_KEY = "SMALL_COMP_WS_VOLUME";

	public SmallComponentWSVolumeParser(Map<String, Object> control) {
		super(String.class, 1);
		this.speciesKey(BaseCoefficientParser.SP0_KEY, control);
		this.coefficients(4, 10);
	}

}
