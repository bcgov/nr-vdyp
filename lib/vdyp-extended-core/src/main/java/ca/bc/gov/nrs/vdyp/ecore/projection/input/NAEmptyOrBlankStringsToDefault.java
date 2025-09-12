package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToDefault;

public class NAEmptyOrBlankStringsToDefault extends ConvertEmptyOrBlankStringsToDefault {
	@Override
	public String processString(String value) {
		String baseValue = value != null && !value.trim().equalsIgnoreCase("NA") ? value : null;
		return super.processString(baseValue);
	}
}
