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

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(Files.newInputStream(filePath), StandardCharsets.UTF_8)
				)
		) {
			String line;
			int currentIndex = 0;
			int recordsRead = 0;

			while ( (line = reader.readLine()) != null && recordsRead < recordCount) {
				// Process only non-header lines within the desired range
				if (!BatchUtils.isHeaderLine(line)) {
					if (currentIndex >= startIndex) {
						// Collect lines in the desired range
						content.append(line).append("\n");
						recordsRead++;
					}
					currentIndex++;
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

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(Files.newInputStream(polygonFilePath), StandardCharsets.UTF_8)
				)
		) {
			String line;
			int currentIndex = 0;
			int recordsRead = 0;

			while ( (line = reader.readLine()) != null && recordsRead < recordCount) {
				if (!BatchUtils.isHeaderLine(line)) {
					if (currentIndex >= startIndex) {
						String featureId = BatchUtils.extractFeatureId(line);
						if (featureId != null) {
							featureIds.add(featureId);
							recordsRead++;
						}
					}
					currentIndex++;
				}
			}
		}

		return featureIds;
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
		String lastSeenFeatureId = null;

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(Files.newInputStream(layerFilePath), StandardCharsets.UTF_8)
				)
		) {
			String line;
			boolean shouldContinueReading = true;

			while ( (line = reader.readLine()) != null && shouldContinueReading) {
				// Process only data lines with valid FEATURE_IDs (skip headers and invalid lines)
				if (!BatchUtils.isHeaderLine(line)) {
					String featureId = BatchUtils.extractFeatureId(line);
					if (featureId != null) {
						// Check if this is a FEATURE_ID we're looking for
						if (featureIds.contains(featureId)) {
							content.append(line).append("\n");
							lastSeenFeatureId = featureId;
						} else if (lastSeenFeatureId != null && !featureId.equals(lastSeenFeatureId)) {
							// We've moved past a FEATURE_ID we were tracking
							remainingFeatureIds.remove(lastSeenFeatureId);
							lastSeenFeatureId = null;

							// Optimization: If file is sorted and we've processed all FEATURE_IDs, stop reading
							shouldContinueReading = !remainingFeatureIds.isEmpty();
						}
					}
				}
			}
		}
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
