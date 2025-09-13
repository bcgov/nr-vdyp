package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;

public class NAEmptyOrBlankStringsToNull extends ConvertEmptyOrBlankStringsToNull {
	@Override
	public String processString(String value) {
		String baseValue = value != null && !value.trim().equalsIgnoreCase("NA") ? value : "";
		baseValue = super.processString(baseValue);
		return baseValue;
	}
}
