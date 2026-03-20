package ca.bc.gov.nrs.vdyp.batch.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class BatchUtils {

	private BatchUtils() {
		//
	}

	private static final DateTimeFormatter dateTimeFormatterForFilenames = DateTimeFormatter
			.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");

	public static String createJobFolderName(String prefix, String guid) {
		return String.format("%s-%s", prefix, guid);
	}

	public static String createJobTimestamp() {
		return dateTimeFormatterForFilenames.format(LocalDateTime.now());
	}

	public static String createJobGuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Builds the input partition folder name.
	 *
	 * @param partitionName the partition name (e.g., "partition0")
	 * @return the input partition folder name (e.g., "input-partition0")
	 */
	public static String buildInputPartitionFolderName(String partitionName) {
		return BatchConstants.Partition.INPUT_PREFIX + "-" + partitionName;
	}

	/**
	 * Builds the output partition folder name.
	 *
	 * @param partitionName the partition name (e.g., "partition0")
	 * @return the output partition folder name (e.g., "output-partition0")
	 */
	public static String buildOutputPartitionFolderName(String partitionName) {
		return BatchConstants.Partition.OUTPUT_PREFIX + "-" + partitionName;
	}

	/**
	 * Builds a unique batch projection ID for VDYP projection operations.
	 *
	 * @param jobExecutionId the job execution ID
	 * @param partitionName  the partition name
	 * @param projectionKind the projection request kind
	 * @return the batch projection ID (e.g., "batch-1-partition0-projection-HCSV-2025_10_02_14_06_43_4933")
	 */
	public static String buildBatchProjectionId(Long jobExecutionId, String partitionName, Object projectionKind) {
		StringBuilder sb = new StringBuilder("batch-");
		sb.append(jobExecutionId).append("-");
		sb.append(partitionName).append("-");
		sb.append("projection-").append(projectionKind).append("-");
		sb.append(dateTimeFormatterForFilenames.format(LocalDateTime.now()));
		return sb.toString();
	}

	/**
	 * Extracts the first field from a CSV line, handling both quoted and unquoted fields.
	 *
	 * Supports standard CSV quoting: - Unquoted: "value,other" -> "value" - Quoted: ""value"",other" -> "value" - With
	 * spaces: " value ,other" -> "value"
	 *
	 * @param csvLine The CSV line to parse
	 * @return The first field trimmed, or null if the line is null/empty or has no comma separator
	 */
	private static String extractFirstField(String csvLine) {
		if (csvLine == null || csvLine.isEmpty()) {
			return null;
		}
		String field = csvLine;
		// Quick path for unquoted fields (most common case)
		int commaIndex = csvLine.indexOf(',');
		if (commaIndex > 0) {
			field = csvLine.substring(0, commaIndex).trim();
		}

		// Handle quoted fields: remove surrounding quotes and unescape doubled quotes
		if (field.length() >= 2 && field.charAt(0) == '"' && field.charAt(field.length() - 1) == '"') {
			field = field.substring(1, field.length() - 1).replace("\"\"", "\"");
		}

		return field.isEmpty() ? null : field;
	}

	/**
	 * Determines if a line is a header line.
	 *
	 * Handles both quoted and unquoted headers. Checks if the first field matches known header keywords.
	 */
	public static boolean isHeaderLine(String line) {
		if (line == null || line.trim().isEmpty()) {
			return true; // Treat empty lines as headers (skip them)
		}

		String firstField = extractFirstField(line);
		if (firstField == null) {
			return true; // Treat lines without comma as headers
		}

		String upperField = firstField.toUpperCase();
		// Check if it starts with header keywords
		return upperField.startsWith("FEATURE") || upperField.startsWith("TABLE") || upperField.startsWith("POLYGON")
				|| upperField.contains("LAYER_ID") || upperField.contains("SPECIES_CODE");
	}

	/**
	 * Extract FEATURE_ID from the first field of a CSV line.
	 *
	 * Handles both quoted and unquoted values from any valid CSV file.
	 *
	 * @param csvLine The CSV line to parse
	 * @return The FEATURE_ID as a String, or null if the line is null/empty. Callers MUST check for null before using
	 *         the returned value.
	 */
	public static String extractFeatureId(String csvLine) {
		return extractFirstField(csvLine);
	}

	/**
	 * Extract FEATURE_ID from the first field of a CSV line as a Long for numeric comparisons.
	 *
	 * Handles both quoted and unquoted values from any valid CSV file.
	 *
	 * @param csvLine The CSV line to parse
	 * @return The FEATURE_ID as a Long, or null if the line is null/empty or the first field is not a valid number.
	 *         Callers MUST check for null before using the returned value.
	 */
	public static Long extractFeatureIdLong(String csvLine) {
		String field = extractFirstField(csvLine);
		if (field == null) {
			return null;
		}
		try {
			return Long.valueOf(field);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Counts the total number of data records in a CSV file, skipping blank lines and header lines.
	 *
	 * @param reader The BufferedReader to read from
	 * @return The number of data records (excluding headers and blank lines)
	 * @throws IOException if reading fails
	 */
	public static int countDataRecords(BufferedReader reader) throws IOException {
		int count = 0;
		boolean headerChecked = false;

		String line;
		while ( (line = reader.readLine()) != null) {
			// Skip blank lines and optionally skip header (only checked once)
			boolean shouldSkip = line.isBlank() || (!headerChecked && isHeaderLine(line));

			if (!headerChecked && !line.isBlank()) {
				headerChecked = true;
			}

			if (!shouldSkip) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Returns the line separator length used in partitioned CSV files.
	 *
	 * Partitioned files are created with PrintWriter.println() which uses System.lineSeparator(), so we can directly
	 * use the platform's line separator length instead of detecting it from files.
	 *
	 * @return The newline length in bytes (1 for \n LF, 2 for \r\n CRLF)
	 */
	public static int getLineSeparatorLength() {
		return System.lineSeparator().length();
	}

	public static Path getFinalZipName(Path jobBasePath, String jobTimestamp) {
		String finalZipName = String.format("vdyp-output-%s.zip", jobTimestamp);
		return jobBasePath.resolve(finalZipName);
	}

	public static String buildResultZipFileName(String reportTitle) {
		String base = (reportTitle != null && !reportTitle.isBlank()) ? reportTitle : "Projection";
		String sanitized = (base + "_All Files").replaceAll("[^a-zA-Z0-9._\\-]", "_").replaceAll("_+", "_")
				.replaceAll("(^_)|(_$)", "");
		return sanitized + ".zip";
	}

	public static void confirmDirectoryExists(Path dirPath) throws IOException {
		if (!Files.exists(dirPath)) {
			throw new IOException("Directory does not exist: " + dirPath);
		}

		if (!Files.isDirectory(dirPath)) {
			throw new IOException("Path is not a directory: " + dirPath);
		}
	}
}
