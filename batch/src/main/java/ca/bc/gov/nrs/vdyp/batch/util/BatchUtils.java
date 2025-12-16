package ca.bc.gov.nrs.vdyp.batch.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
	 * Calculates byte offsets for each data record in a partitioned CSV file using a pre-detected newline length. This
	 * allows efficient reuse of the newline length across multiple files in the same job, avoiding redundant detection.
	 *
	 * @param filePath The path to the partitioned CSV file
	 * @return RecordByteOffsets containing the byte offset and FEATURE_ID of each data record
	 * @throws IOException if file reading fails
	 */
	public static RecordByteOffsets calculateRecordByteOffsets(Path filePath) throws IOException {
		List<Long> offsets = new ArrayList<>();
		List<String> featureIds = new ArrayList<>();
		long currentByte = 0;
		int recordCount = 0;
		boolean headerChecked = false;

		try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			String line;
			while ( (line = reader.readLine()) != null) {
				// Use platform line separator length (partitioned files are created with PrintWriter.println() which
				// uses System.lineSeparator()) - The newline length in bytes (1 for \n LF, 2 for \r\n CRLF)
				int newlineLength = System.lineSeparator().length();

				// Calculate byte length including actual newline bytes
				int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + newlineLength;

				// Skip blank lines and optionally skip header (only checked once)
				boolean shouldSkip = line.isBlank() || (!headerChecked && isHeaderLine(line));

				if (!headerChecked && !line.isBlank()) {
					headerChecked = true;
				}

				if (!shouldSkip) {
					// Record the byte offset at the START of this data record
					offsets.add(currentByte);

					// Extract and store FEATURE_ID
					String featureId = extractFeatureId(line);
					featureIds.add(featureId != null ? featureId : "");

					recordCount++;
				}

				currentByte += lineBytes;
			}
		}

		return new RecordByteOffsets(offsets, featureIds, recordCount);
	}

	/**
	 * Calculates the byte offset for a specific record index.
	 *
	 * @param offsets    The RecordByteOffsets for the file
	 * @param startIndex The starting record index (0-based)
	 * @return The byte offset where the record starts
	 */
	public static long getChunkStartByte(RecordByteOffsets offsets, int startIndex) {
		if (startIndex >= offsets.getTotalRecords()) {
			return 0;
		}
		return offsets.getOffsetForRecord(startIndex);
	}

	/**
	 * Calculates the byte range for layer records matching the FEATURE_IDs from a specific polygon chunk. This method
	 * uses the cached FEATURE_ID metadata to efficiently find the byte range without reading the layer file again.
	 *
	 * @param polygonOffsets     The byte offsets for the polygon file
	 * @param polygonStartIndex  The starting record index in the polygon file
	 * @param polygonRecordCount The number of polygon records in the chunk
	 * @param layerOffsets       The byte offsets for the layer file
	 * @return LayerByteRange containing the start byte and record count for matching layer records
	 */
	public static LayerByteRange calculateLayerByteRange(
			RecordByteOffsets polygonOffsets, int polygonStartIndex, int polygonRecordCount,
			RecordByteOffsets layerOffsets
	) {
		// Step 1: Extract FEATURE_IDs from the polygon chunk
		Set<String> featureIds = extractFeatureIdsFromPolygonChunk(
				polygonOffsets, polygonStartIndex, polygonRecordCount
		);

		if (featureIds.isEmpty()) {
			return new LayerByteRange(0, 0);
		}

		// Step 2: Find the first and last matching record indices in the layer file
		// Assumption: Both files are sorted by FEATURE_ID, so matching records are contiguous
		int firstMatchingIndex = -1;
		int lastMatchingIndex = -1;
		int totalLayerRecords = layerOffsets.getTotalRecords();

		for (int i = 0; i < totalLayerRecords; i++) {
			String layerFeatureId = layerOffsets.getFeatureIdForRecord(i);
			if (featureIds.contains(layerFeatureId)) {
				if (firstMatchingIndex == -1) {
					firstMatchingIndex = i;
				}
				lastMatchingIndex = i;
			}
		}

		// Step 3: Calculate the byte range
		if (firstMatchingIndex == -1) {
			// No matching layer records found for this polygon chunk
			// This is valid - some polygons may not have layer data
			return new LayerByteRange(0, 0);
		}

		long startByte = layerOffsets.getOffsetForRecord(firstMatchingIndex);
		// Since files are sorted by FEATURE_ID, all records between first and last are matching
		int recordCount = lastMatchingIndex - firstMatchingIndex + 1;

		return new LayerByteRange(startByte, recordCount);
	}

	/**
	 * Extracts FEATURE_IDs from a specific chunk of polygon records. Since RecordByteOffsets already contains
	 * FEATURE_IDs, we can retrieve them directly without reading the file again.
	 *
	 * @param polygonOffsets     The byte offsets for the polygon file
	 * @param polygonStartIndex  The starting record index
	 * @param polygonRecordCount The number of records to read
	 * @return Set of FEATURE_IDs from the polygon chunk
	 */
	private static Set<String> extractFeatureIdsFromPolygonChunk(
			RecordByteOffsets polygonOffsets, int polygonStartIndex, int polygonRecordCount
	) {
		Set<String> featureIds = new LinkedHashSet<>();

		// Extract FEATURE_IDs directly from the cached metadata
		for (int i = 0; i < polygonRecordCount; i++) {
			int recordIndex = polygonStartIndex + i;
			if (recordIndex < polygonOffsets.getTotalRecords()) {
				String featureId = polygonOffsets.getFeatureIdForRecord(recordIndex);
				if (featureId != null && !featureId.isEmpty()) {
					featureIds.add(featureId);
				}
			}
		}

		return featureIds;
	}

	/**
	 * Holds metadata about record byte offsets in a CSV file. This allows efficient FileChannel-based streaming by
	 * jumping directly to specific records without loading the entire file into memory.
	 */
	public static class RecordByteOffsets {
		private final List<Long> offsets;
		private final List<String> featureIds;
		private final int totalRecords;

		public RecordByteOffsets(List<Long> offsets, List<String> featureIds, int totalRecords) {
			this.offsets = offsets;
			this.featureIds = featureIds;
			this.totalRecords = totalRecords;
		}

		public long getOffsetForRecord(int recordIndex) {
			if (recordIndex < 0 || recordIndex >= offsets.size()) {
				throw new IllegalArgumentException(
						"Record index out of bounds: " + recordIndex + " (total records: " + totalRecords + ")"
				);
			}
			return offsets.get(recordIndex);
		}

		public String getFeatureIdForRecord(int recordIndex) {
			if (recordIndex < 0 || recordIndex >= featureIds.size()) {
				throw new IllegalArgumentException(
						"Record index out of bounds: " + recordIndex + " (total records: " + totalRecords + ")"
				);
			}
			return featureIds.get(recordIndex);
		}

		public int getTotalRecords() {
			return totalRecords;
		}
	}

	/**
	 * Holds the byte range information for layer records matching a specific set of polygon FEATURE_IDs.
	 */
	public static class LayerByteRange {
		private final long startByte;
		private final int recordCount;

		public LayerByteRange(long startByte, int recordCount) {
			this.startByte = startByte;
			this.recordCount = recordCount;
		}

		public long getStartByte() {
			return startByte;
		}

		public int getRecordCount() {
			return recordCount;
		}
	}
}
