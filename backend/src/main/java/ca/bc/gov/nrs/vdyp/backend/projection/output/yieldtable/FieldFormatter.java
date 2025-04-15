package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.time.format.DateTimeFormatter;

public class FieldFormatter {

	static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

	private static final int DOUBLE_PRECISION = 5;
	private static final String DOUBLE_FORMAT = "%." + DOUBLE_PRECISION + "f";

	static String format(Double d) {
		if (d != null) {
			return String.format(FieldFormatter.DOUBLE_FORMAT, d);
		} else {
			return "";
		}
	}

	static String format(Long l) {
		if (l != null) {
			return Long.toString(l);
		} else {
			return "";
		}
	}

	static String format(Integer i) {
		if (i != null) {
			return Integer.toString(i);
		} else {
			return "";
		}
	}

	static String format(String s) {
		if (s != null) {
			return s;
		} else {
			return "";
		}
	}
}
