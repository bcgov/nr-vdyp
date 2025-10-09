package ca.bc.gov.nrs.vdyp.batch.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BatchUtils {

	private BatchUtils() {
		//
	}

	public static final DateTimeFormatter dateTimeFormatterForFilenames = DateTimeFormatter
			.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");

	public static String createJobFolderName(String prefix, String timestamp) {
		return String.format("%s-%s", prefix, timestamp);
	}

	public static String createJobTimestamp() {
		return dateTimeFormatterForFilenames.format(LocalDateTime.now());
	}

	/**
	 * Sanitizes provided filename for safe logging Removes control characters, line breaks, and limits length.
	 */
	public static String sanitizeForLogging(String filename) {
		if (filename == null) {
			return "null";
		}

		String sanitized = filename.replaceAll("[\\x00-\\x1f\\x7f-\\x9f]", "").trim();

		if (sanitized.length() > 100) {
			sanitized = sanitized.substring(0, 97) + "...";
		}

		return sanitized.isEmpty() ? "empty" : sanitized;
	}
}
