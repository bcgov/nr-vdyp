package ca.bc.gov.nrs.vdyp.batch.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.springframework.lang.NonNull;

/**
 * InputStream that reads a specific range of records from a CSV file. This stream skips header lines and reads only the
 * specified range of data records, enabling efficient memory usage for large files.
 *
 * Two modes of operation: - Polygon mode: Reads records by line index (startIndex, recordCount) - Layer mode: Reads ALL
 * records matching a set of FEATURE_IDs (requires sorted input)
 *
 * Assumption: Input CSV files are sorted by FEATURE_ID for efficient layer filtering.
 */
public class BatchRangeInputStream extends InputStream {

	private final InputStream delegate;
	private boolean closed = false;

	private BatchRangeInputStream(InputStream delegate) {
		this.delegate = delegate;
	}

	/**
	 * Creates an InputStream that reads a specific range of polygon records from a CSV file. This method reads records
	 * by line index (for polygon files).
	 *
	 * @param filePath    The path to the polygon CSV file
	 * @param startIndex  The starting record index (0-based, after skipping headers)
	 * @param recordCount The number of records to read
	 * @return An InputStream containing only the specified range of records with trailing newline
	 * @throws IOException if file reading fails
	 */
	public static BatchRangeInputStream create(@NonNull Path filePath, int startIndex, int recordCount)
			throws IOException {
		if (startIndex < 0) {
			throw new IllegalArgumentException("Start index must be non-negative, got: " + startIndex);
		}
		if (recordCount <= 0) {
			throw new IllegalArgumentException("Record count must be positive, got: " + recordCount);
		}

		StringBuilder content = new StringBuilder();

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			int currentIndex = 0; // Physical line index (including empty lines, excluding header)
			int recordsRead = 0;
			boolean headerChecked = false;

			while ( (line = reader.readLine()) != null && recordsRead < recordCount) {
				// Skip blank lines and optionally skip header (only checked once)
				boolean shouldSkip = line.isBlank() || (!headerChecked && BatchUtils.isHeaderLine(line));

				if (!headerChecked && !line.isBlank()) {
					headerChecked = true;
				}

				if (!shouldSkip) {
					if (currentIndex >= startIndex) {
						content.append(line).append("\n");
						recordsRead++;
					}
					currentIndex++; // Increment for all non-empty lines
				}
			}
		}

		// Add trailing newline to match original file structure
		content.append("\n");

		byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
		return new BatchRangeInputStream(new ByteArrayInputStream(bytes));
	}

	/**
	 * Creates an InputStream that reads layer records matching the specified FEATURE_IDs from a polygon CSV file. This
	 * method first extracts FEATURE_IDs from the polygon file range, then reads all matching layer records.
	 *
	 * Assumption: Layer CSV file is sorted by FEATURE_ID for efficient reading. Once all FEATURE_IDs from the polygon
	 * range are found in the layer file, reading stops (no full file scan).
	 *
	 * @param polygonFilePath The path to the polygon CSV file
	 * @param layerFilePath   The path to the layer CSV file
	 * @param startIndex      The starting record index in the polygon file (0-based, after skipping headers)
	 * @param recordCount     The number of polygon records to process
	 * @return An InputStream containing all layer records for the specified polygon FEATURE_IDs
	 * @throws IOException if file reading fails
	 */
	public static BatchRangeInputStream
			createForLayers(@NonNull Path polygonFilePath, @NonNull Path layerFilePath, int startIndex, int recordCount)
					throws IOException {
		if (startIndex < 0) {
			throw new IllegalArgumentException("Start index must be non-negative, got: " + startIndex);
		}
		if (recordCount <= 0) {
			throw new IllegalArgumentException("Record count must be positive, got: " + recordCount);
		}

		// Step 1: Extract FEATURE_IDs from polygon file range
		Set<String> featureIds = extractFeatureIdsFromPolygonRange(polygonFilePath, startIndex, recordCount);

		// Step 2: Read matching layer records (optimized for sorted files)
		StringBuilder content = new StringBuilder();
		readMatchingLayerRecords(layerFilePath, featureIds, content);

		// Add trailing newline to match original file structure
		content.append("\n");

		byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
		return new BatchRangeInputStream(new ByteArrayInputStream(bytes));
	}

	/**
	 * Extract FEATURE_IDs from a specific range of polygon records.
	 *
	 * @param polygonFilePath The path to the polygon CSV file
	 * @param startIndex      The starting record index (0-based, after skipping headers)
	 * @param recordCount     The number of records to read
	 * @return Set of FEATURE_IDs found in the specified range
	 * @throws IOException if file reading fails
	 */
	private static Set<String> extractFeatureIdsFromPolygonRange(Path polygonFilePath, int startIndex, int recordCount)
			throws IOException {
		Set<String> featureIds = new HashSet<>();
		RangeExtractionState state = new RangeExtractionState(startIndex, recordCount);

		try (BufferedReader reader = Files.newBufferedReader(polygonFilePath)) {
			String line;
			while ( (line = reader.readLine()) != null && !state.isComplete()) {
				if (shouldSkipLineForRange(line, state)) {
					continue;
				}

				processFeatureIdLine(line, state, featureIds);
			}
		}

		return featureIds;
	}

	/**
	 * Determines if a line should be skipped during range extraction.
	 */
	private static boolean shouldSkipLineForRange(String line, RangeExtractionState state) {
		boolean shouldSkip = line.isBlank() || (!state.headerChecked && BatchUtils.isHeaderLine(line));

		if (!state.headerChecked && !line.isBlank()) {
			state.headerChecked = true;
		}

		return shouldSkip;
	}

	/**
	 * Processes a line to extract feature ID if within range.
	 */
	private static void processFeatureIdLine(String line, RangeExtractionState state, Set<String> featureIds) {
		if (state.isInRange()) {
			String featureId = BatchUtils.extractFeatureId(line);
			if (featureId != null) {
				featureIds.add(featureId);
				state.recordsRead++;
			}
		}
		state.currentIndex++;
	}

	/**
	 * Helper class to track range extraction state.
	 */
	private static class RangeExtractionState {
		final int startIndex;
		final int recordCount;
		int currentIndex = 0;
		int recordsRead = 0;
		boolean headerChecked = false;

		RangeExtractionState(int startIndex, int recordCount) {
			this.startIndex = startIndex;
			this.recordCount = recordCount;
		}

		boolean isInRange() {
			return currentIndex >= startIndex;
		}

		boolean isComplete() {
			return recordsRead >= recordCount;
		}
	}

	/**
	 * Read all layer records matching the specified FEATURE_IDs. Optimized for sorted layer files - stops reading once
	 * all FEATURE_IDs are processed.
	 *
	 * @param layerFilePath The path to the layer CSV file (assumed to be sorted by FEATURE_ID)
	 * @param featureIds    Set of FEATURE_IDs to match
	 * @param content       StringBuilder to append matching records to
	 * @throws IOException if file reading fails
	 */
	private static void readMatchingLayerRecords(Path layerFilePath, Set<String> featureIds, StringBuilder content)
			throws IOException {
		if (featureIds.isEmpty()) {
			return;
		}

		// Track which FEATURE_IDs we've finished processing (for sorted file optimization)
		Set<String> remainingFeatureIds = new HashSet<>(featureIds);
		LayerReadingState state = new LayerReadingState();

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(Files.newInputStream(layerFilePath), StandardCharsets.UTF_8)
				)
		) {
			// Skip first line if it's a header
			String firstLine = reader.readLine();
			processFirstLayerLine(firstLine, featureIds, content, state);

			// Process remaining data lines (no need to check for headers anymore)
			String line;
			while ( (line = reader.readLine()) != null && state.shouldContinueReading) {
				if (!line.trim().isEmpty()) {
					processLayerLine(line, featureIds, content, remainingFeatureIds, state);
				}
			}
		}
	}

	/**
	 * Process the first line of the layer file.
	 */
	private static void processFirstLayerLine(
			String firstLine, Set<String> featureIds, StringBuilder content, LayerReadingState state
	) {
		if (firstLine != null && !firstLine.isBlank() && !BatchUtils.isHeaderLine(firstLine)) {
			String featureId = BatchUtils.extractFeatureId(firstLine);
			if (featureId != null && featureIds.contains(featureId)) {
				content.append(firstLine).append("\n");
				state.lastSeenFeatureId = featureId;
			}
		}
	}

	/**
	 * Process a single layer line and update state accordingly.
	 */
	private static void processLayerLine(
			String line, Set<String> featureIds, StringBuilder content, Set<String> remainingFeatureIds,
			LayerReadingState state
	) {
		String featureId = BatchUtils.extractFeatureId(line);
		if (featureId == null) {
			return;
		}

		if (featureIds.contains(featureId)) {
			// Found a matching FEATURE_ID
			content.append(line).append("\n");
			state.lastSeenFeatureId = featureId;
		} else if (state.lastSeenFeatureId != null && !featureId.equals(state.lastSeenFeatureId)) {
			// We've moved past a FEATURE_ID we were tracking
			remainingFeatureIds.remove(state.lastSeenFeatureId);
			state.lastSeenFeatureId = null;

			// Optimization: If file is sorted and we've processed all FEATURE_IDs, stop reading
			state.shouldContinueReading = !remainingFeatureIds.isEmpty();
		}
	}

	/**
	 * Helper class to track state while reading layer records.
	 */
	private static class LayerReadingState {
		String lastSeenFeatureId = null;
		boolean shouldContinueReading = true;
	}

	@Override
	public int read() throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		}
		return delegate.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		}
		return delegate.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			delegate.close();
		}
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}
}
