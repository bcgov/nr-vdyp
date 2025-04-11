package ca.bc.gov.nrs.vdyp.backend.utils;

public class CsvRecordBeanHelper {

	public static Double parseDoubleAcceptNull(String doubleText) {
		return doubleText == null ? null : Double.parseDouble(doubleText);
	}

	public static Long parseLongAcceptNull(String longText) {
		return longText == null ? null : Long.parseLong(longText);
	}

	public static Short parseShortAcceptNull(String shortText) {
		return shortText == null ? null : Short.parseShort(shortText);
	}

	public static Integer parseIntegerAcceptNull(String integerText) {
		return integerText == null ? null : Integer.valueOf(integerText);
	}
}
