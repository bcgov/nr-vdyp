package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Region;

public class VeteranBQParser extends SimpleCoefficientParser2<String, Region> {

	public static final String CONTROL_KEY = "VETERAN_BQ";

	public VeteranBQParser(Map<String, Object> control) {
		super(1);
		this.speciesKey(control).space(1).regionKey().coefficients(3, 9);
	}

}