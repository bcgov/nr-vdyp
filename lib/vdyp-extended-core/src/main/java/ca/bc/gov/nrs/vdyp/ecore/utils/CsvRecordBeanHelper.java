package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.math.BigDecimal;

public class CsvRecordBeanHelper {

	private CsvRecordBeanHelper() {
	}

	public static Double parseDoubleAcceptNull(String doubleText) {
		return doubleText == null ? null : Double.parseDouble(doubleText);
	}

	public static Long parseLongAcceptNull(String longText) {
		if (longText == null) {
			return null;
		}
		if (longText.indexOf('.') >= 0 || longText.indexOf('e') >= 0 || longText.indexOf('E') >= 0) {
			return new BigDecimal(longText).longValue();
		}
		return Long.parseLong(longText);
	}

	public static Short parseShortAcceptNull(String shortText) {
		return shortText == null ? null : Short.parseShort(shortText);
	}

	public static Integer parseIntegerAcceptNull(String integerText) {
		return integerText == null ? null : Integer.valueOf(integerText);
	}
}
