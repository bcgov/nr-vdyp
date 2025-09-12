package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToDefault;

public class NAEmptyOrBlankStringsToDefault extends ConvertEmptyOrBlankStringsToDefault {
	@Override
	public String processString(String value) {
		return value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("NA") ? value : null;
	}
}
