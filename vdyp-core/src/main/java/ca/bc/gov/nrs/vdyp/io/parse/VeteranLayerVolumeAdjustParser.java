package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

public class VeteranLayerVolumeAdjustParser extends SimpleCoefficientParser1<String> {

	public static final String CONTROL_KEY = "VETERAN_LAYER_VOLUME_ADJUST";

	public VeteranLayerVolumeAdjustParser(Map<String, Object> control) {
		super(String.class, 0);
		this.speciesKey();
		this.coefficients(4, 9);
	}

}
