package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.math.BigDecimal;

public class CsvRecordBeanHelper {

	private CsvRecordBeanHelper() {
	}

	public static Double parseDoubleAcceptNull(String doubleText) {
		return doubleText == null ? null : Double.parseDouble(doubleText);
	}

	public static Long parseLongAcceptNull(String longText) {
		return longText == null ? null : toLong(longText);
	}

	public static long toLong(String s) {
		if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
			return new BigDecimal(s).longValue();
		}
		return Long.parseLong(s);
	}

	public static Short parseShortAcceptNull(String shortText) {
		return shortText == null ? null : Short.parseShort(shortText);
	}

	public static Integer parseIntegerAcceptNull(String integerText) {
		return integerText == null ? null : Integer.valueOf(integerText);
	}
}
