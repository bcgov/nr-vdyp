package ca.bc.gov.nrs.vdyp.batch.util;

public final class Utils {

	private Utils() {
		//
	}

	/**
	 * Sanitizes provided filename for safe logging
	 * Removes control characters, line breaks, and limits length.
	 * 
	 * @param filename the filename to sanitize
	 * @return sanitized filename safe for logging
	 */
	public static String sanitizeForLogging(String filename) {
		if (filename == null) {
			return "null";
		}

		// Remove control characters and line breaks, limit length
		String sanitized = filename.replaceAll("[\\x00-\\x1f\\x7f-\\x9f]", "")
				.trim();

		// Limit length to prevent log flooding
		if (sanitized.length() > 100) {
			sanitized = sanitized.substring(0, 97) + "...";
		}

		return sanitized.isEmpty() ? "empty" : sanitized;
	}
}
