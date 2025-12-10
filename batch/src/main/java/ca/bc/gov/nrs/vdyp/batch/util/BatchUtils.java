package ca.bc.gov.nrs.vdyp.batch.util;

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
	 * Determines if a line is a header line.
	 */
	public static boolean isHeaderLine(String line) {
		if (line == null || line.trim().isEmpty()) {
			return true; // Treat empty lines as headers (skip them)
		}

		String upperLine = line.toUpperCase();
		// check if it starts with header keywords
		return upperLine.startsWith("FEATURE") || upperLine.startsWith("TABLE") || upperLine.startsWith("POLYGON")
				|| upperLine.contains("LAYER_ID") || upperLine.contains("SPECIES_CODE");
	}

	/**
	 * Extract FEATURE_ID from the first field of a CSV line.
	 *
	 * @param csvLine The CSV line to parse
	 * @return The FEATURE_ID as a String, or null if the line is null/empty. Callers MUST check for null before using
	 *         the returned value.
	 */
	public static String extractFeatureId(String csvLine) {
		if (csvLine == null || csvLine.trim().isEmpty()) {
			return null;
		}

		int commaIndex = csvLine.indexOf(',');
		if (commaIndex == -1) {
			// No comma found, entire line might be the FEATURE_ID
			return csvLine.trim();
		} else {
			// Extract first field before comma
			return csvLine.substring(0, commaIndex).trim();
		}
	}
}
