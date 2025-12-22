package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchDataReadException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * Memory-efficient ItemReader that returns chunk metadata instead of loading entire CSV data. The actual file streaming
 * is performed in the ItemWriter.
 */
public class BatchItemReader implements ItemStreamReader<BatchChunkMetadata> {

	private static final Logger logger = LoggerFactory.getLogger(BatchItemReader.class);

	@NonNull
	private final String partitionName;
	@NonNull
	private final Long jobExecutionId;
	@NonNull
	private final String jobGuid;
	private final int chunkSize;

	// Job base directory
	private String jobBaseDir;

	// File paths for chunk-based reading
	private Path polygonFilePath;
	private Path layerFilePath;

	// Total data record counts (excluding headers and blank lines)
	private int totalPolygonDataRecords;

	// Number of processed polygon data records so far (excluding headers and blank lines)
	private int numProcessedPolygonRecords = 0;

	// Current read positions in the files (byte offset where chunk will be read from)
	private long currentPolygonChunkStartByteOffset = 0;
	private long currentLayerChunkStartByteOffset = 0;

	private boolean readerOpened = false;

	public BatchItemReader(
			@NonNull String partitionName, @NonNull Long jobExecutionId, @NonNull String jobGuid, @NonNull int chunkSize
	) {
		this.partitionName = partitionName;
		this.jobExecutionId = jobExecutionId;
		this.jobGuid = jobGuid;
		this.chunkSize = Math.max(chunkSize, 1);
	}

	/**
	 * Reads the next ChunkMetadata for processing.
	 *
	 * @return The next ChunkMetadata to process, or null when all data has been read. Note: Returning null signals
	 *         end-of-data to Spring Batch framework - this is the standard contract.
	 * @throws BatchDataReadException if reading fails
	 */
	@Override
	public BatchChunkMetadata read() throws BatchDataReadException {
		if (!readerOpened) {
			throw BatchDataReadException.handleDataReadFailure(
					new IllegalStateException("Reader not opened. Call open() first."),
					"Reader not opened. Call open() first.", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		// Check if we've read all records
		if (numProcessedPolygonRecords >= totalPolygonDataRecords) {
			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] All records processed (numberOfProcessedPolygonRecords={}, totalRecords={})",
					jobGuid, jobExecutionId, partitionName, numProcessedPolygonRecords, totalPolygonDataRecords
			);
			return null; // End of data - signals Spring Batch that reading is complete
		}

		// Calculate how many polygon data records to include in this chunk
		int remainingPolygonRecords = totalPolygonDataRecords - numProcessedPolygonRecords;
		int polygonRecordsInThisChunk = Math.min(chunkSize, remainingPolygonRecords);

		// Scan polygon chunk to extract metadata (feature IDs and byte positions)
		ChunkMetadata polygonChunkMetaData;
		try {
			polygonChunkMetaData = getPolygonChunkMetadata(
					polygonFilePath, currentPolygonChunkStartByteOffset, polygonRecordsInThisChunk
			);
		} catch (IOException e) {
			throw BatchDataReadException.handleDataReadFailure(
					e, "Failed to scan polygon chunk metadata", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		// Convert polygon feature IDs to Set for efficient layer matching
		Set<String> featureIdsInThisChunk = new LinkedHashSet<>(polygonChunkMetaData.getFeatureIds());

		// Get layer chunk metadata by finding matching FEATURE_IDs starting from current position
		ChunkMetadata layerChunkMetadata;
		try {
			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] Scanning layer metadata for polygon features: {} (count: {}) from layerByte {}",
					jobGuid, jobExecutionId, partitionName, featureIdsInThisChunk, featureIdsInThisChunk.size(),
					currentLayerChunkStartByteOffset
			);

			layerChunkMetadata = getMatchingLayerMetadata(
					layerFilePath, currentLayerChunkStartByteOffset, featureIdsInThisChunk
			);

			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] Scanned {} layer records (startByte={}, endByte={}) for features: {}",
					jobGuid, jobExecutionId, partitionName, layerChunkMetadata.getRecordCount(),
					layerChunkMetadata.getStartByte(), layerChunkMetadata.getEndByte(),
					layerChunkMetadata.getFeatureIds()
			);
		} catch (IOException e) {
			throw BatchDataReadException.handleDataReadFailure(
					e, "Failed to read layer chunk", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		BatchChunkMetadata metadata = new BatchChunkMetadata(
				partitionName, jobBaseDir, polygonChunkMetaData.getStartByte(), polygonChunkMetaData.getRecordCount(),
				layerChunkMetadata.getStartByte(), layerChunkMetadata.getRecordCount()
		);

		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Created chunk metadata: polygonStartByte={}, polygonRecordCount={}, layerStartByte={}, layerRecordCount={}, progress={}/{}",
				jobGuid, jobExecutionId, partitionName, polygonChunkMetaData.getStartByte(),
				polygonChunkMetaData.getRecordCount(), layerChunkMetadata.getStartByte(),
				layerChunkMetadata.getRecordCount(), numProcessedPolygonRecords + polygonRecordsInThisChunk,
				totalPolygonDataRecords
		);

		// Update position for next read
		numProcessedPolygonRecords += polygonRecordsInThisChunk;
		currentPolygonChunkStartByteOffset = polygonChunkMetaData.getEndByte();
		currentLayerChunkStartByteOffset = layerChunkMetadata.getEndByte();

		return metadata;
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Opening BatchItemReader with chunk size: {}", jobGuid,
				jobExecutionId, partitionName, chunkSize
		);

		try {
			// Get partition directory from job parameters
			this.jobBaseDir = executionContext.getString(BatchConstants.Job.BASE_DIR);
			if (this.jobBaseDir.trim().isEmpty()) {
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new IllegalArgumentException("jobBaseDir is empty"), "jobBaseDir is empty in ExecutionContext",
						jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
			Path partitionDir = Paths.get(this.jobBaseDir, inputPartitionFolderName);
			if (!Files.exists(partitionDir)) {
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new FileNotFoundException("Partition directory does not exist: " + partitionDir),
						"Partition directory does not exist", jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] Reading from partition directory: {}", jobGuid,
					jobExecutionId, partitionName, partitionDir
			);

			this.polygonFilePath = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
			this.layerFilePath = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

			this.totalPolygonDataRecords = countTotalDataRecords(this.polygonFilePath);

			// Initialize the current read position as the byte offset of the first data record (skipping headers and
			// blank lines)
			this.currentPolygonChunkStartByteOffset = findFirstDataRecordByteOffset(this.polygonFilePath);
			this.currentLayerChunkStartByteOffset = findFirstDataRecordByteOffset(this.layerFilePath);

			readerOpened = true;
			logger.trace(
					"[GUID: {}, EXEID: {}, Partition: {}] BatchItemReader opened successfully. Total polygon records: {}, First polygon byte: {}, First layer byte: {}",
					jobGuid, jobExecutionId, partitionName, totalPolygonDataRecords, currentPolygonChunkStartByteOffset,
					currentLayerChunkStartByteOffset
			);

		} catch (IOException e) {
			BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
					e, "Failed to initialize partition reader", jobGuid, jobExecutionId, partitionName, logger
			);
			throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) {
		// No state to persist
	}

	@Override
	public void close() {
		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Closing BatchItemReader.", jobGuid, jobExecutionId, partitionName
		);

		readerOpened = false;
	}

	/**
	 * Counts the total number of data records in a CSV file from a file path.
	 *
	 * @param filePath The path to the CSV file
	 * @return The number of data records (excluding headers and blank lines)
	 * @throws IOException if file reading fails
	 */
	private int countTotalDataRecords(Path filePath) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			return BatchUtils.countDataRecords(reader);
		}
	}

	/**
	 * Finds the byte offset of the first data record in a CSV file, skipping blank lines and headers. This offset
	 * points to the start of the first actual data record (after header).
	 *
	 * @param filePath The path to the CSV file
	 * @return The byte offset where the first data record starts (0 if no data records found)
	 * @throws IOException if file reading fails
	 */
	private long findFirstDataRecordByteOffset(Path filePath) throws IOException {
		int lineSeparatorLength = BatchUtils.getLineSeparatorLength();
		long currentByte = 0;
		boolean headerChecked = false;

		try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
			String line;
			while ( (line = reader.readLine()) != null) {
				// Calculate byte length including actual newline bytes
				int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + lineSeparatorLength;

				// Skip blank lines and optionally skip header (only checked once)
				boolean shouldSkip = line.isBlank() || (!headerChecked && BatchUtils.isHeaderLine(line));

				if (!headerChecked && !line.isBlank()) {
					headerChecked = true;
				}

				if (!shouldSkip) {
					// This is the first data record - return its byte offset
					return currentByte;
				}

				currentByte += lineBytes;
			}
		}

		// No data records found - return 0
		return 0;
	}

	/**
	 * Gets metadata for a chunk of polygon records by scanning to extract feature IDs and byte positions. Does NOT load
	 * full record data into memory - only scans to collect feature IDs and track byte positions.
	 *
	 * In chunk-based processing, we need to: 1. Extract FEATURE_IDs from this polygon chunk (to find matching layer
	 * records) 2. Calculate endByte to determine where the next chunk should start
	 *
	 * @param polygonFilePath                    The path to the polygon CSV file
	 * @param currentPolygonChunkStartByteOffset The byte offset to start scanning from
	 * @param polygonRecordsInThisChunk          The number of polygon records to scan
	 * @return ChunkMetadata containing extracted FEATURE_IDs and endByte for next chunk positioning
	 * @throws IOException if file scanning fails
	 */
	private static ChunkMetadata getPolygonChunkMetadata(
			Path polygonFilePath, long currentPolygonChunkStartByteOffset, int polygonRecordsInThisChunk
	) throws IOException {
		int lineSeparatorLength = BatchUtils.getLineSeparatorLength();
		PolygonChunkScanner scanner = new PolygonChunkScanner(
				currentPolygonChunkStartByteOffset, polygonRecordsInThisChunk, lineSeparatorLength
		);
		return scanner.scanChunk(polygonFilePath);
	}

	/**
	 * Gets metadata for layer records matching the given FEATURE_IDs by scanning sequentially. Scans from startByte
	 * until all matching feature IDs are found or a non-matching ID is encountered. Does NOT load full record data -
	 * only collects matching feature IDs and tracks byte positions. Assumes files are sorted by FEATURE_ID.
	 *
	 * @param layerFilePath                    The path to the Layer CSV file
	 * @param currentLayerChunkStartByteOffset The byte offset to start scanning from
	 * @param featureIdsInThisChunk            The set of FEATURE_IDs to find
	 * @return ChunkMetadata containing metadata for all records matching the target FEATURE_IDs
	 * @throws IOException if file scanning fails
	 */
	private static ChunkMetadata getMatchingLayerMetadata(
			Path layerFilePath, long currentLayerChunkStartByteOffset, Set<String> featureIdsInThisChunk
	) throws IOException {
		int lineSeparatorLength = BatchUtils.getLineSeparatorLength();
		LayerChunkScanner scanner = new LayerChunkScanner(
				currentLayerChunkStartByteOffset, featureIdsInThisChunk, lineSeparatorLength
		);
		return scanner.scanChunk(layerFilePath);
	}

	/**
	 * Helper class to scan polygon records and extract feature IDs with byte position tracking. Does NOT load actual
	 * record data into memory - only scans to collect feature IDs needed for layer matching and tracks byte positions
	 * to determine where the next chunk starts.
	 *
	 * IMPORTANT: startByte points to a data record position (header already skipped by BatchItemReader.open()).
	 */
	private static class PolygonChunkScanner {
		private final long startByte;
		private final int polygonRecordsInThisChunk;
		private final int lineSeparatorLength;

		private final List<String> featureIds = new ArrayList<>();
		private int recordsRead = 0;
		private long currentByte;

		PolygonChunkScanner(
				long currentPolygonChunkStartByteOffset, int polygonRecordsInThisChunk, int lineSeparatorLength
		) {
			this.startByte = currentPolygonChunkStartByteOffset;
			this.polygonRecordsInThisChunk = polygonRecordsInThisChunk;
			this.lineSeparatorLength = lineSeparatorLength;
			this.currentByte = startByte; // Start scanning from startByte directly
		}

		/**
		 * Scans polygon records to extract feature IDs and calculate the end byte position for next chunk positioning.
		 * Scans through N records starting at startByte, collecting only feature IDs. Tracks currentByte to calculate
		 * endByte, which becomes the next chunk's startByte.
		 *
		 * @param filePath The CSV file to scan
		 * @return ChunkMetadata containing extracted feature IDs and endByte for next chunk
		 * @throws IOException if file scanning fails
		 */
		ChunkMetadata scanChunk(Path filePath) throws IOException {
			try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
				// Position the channel at startByte
				channel.position(startByte);

				try (BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8))) {
					return scanRecords(reader);
				}
			}
		}

		/**
		 * Scans lines to extract feature IDs and calculate byte positions. Processes only the target number of records,
		 * collecting feature IDs for layer matching. Tracks byte positions (currentByte) to determine where the next
		 * chunk starts.
		 *
		 * @param reader The BufferedReader positioned at the chunk start
		 * @return ChunkMetadata with extracted feature IDs and byte position metadata
		 * @throws IOException if scanning fails
		 */
		private ChunkMetadata scanRecords(BufferedReader reader) throws IOException {
			String line;
			while ( (line = reader.readLine()) != null) {
				int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + lineSeparatorLength;

				// Extract feature ID and add to list
				String featureId = BatchUtils.extractFeatureId(line);
				if (featureId != null && !featureId.isEmpty()) {
					featureIds.add(featureId);
					recordsRead++;

					// Check if we've read enough records
					if (recordsRead >= polygonRecordsInThisChunk) {
						// Update currentByte to point to the next record
						currentByte += lineBytes;
						break;
					}
				}

				// Update currentByte for records that don't reach the target count (handles normal iteration)
				currentByte += lineBytes;
			}

			return createPolygonChunkMetadata();
		}

		/**
		 * Creates metadata for this polygon chunk containing extracted feature IDs and byte position information.
		 *
		 * The endByte value represents where this chunk's scanning stopped, which is the byte offset immediately after
		 * the last record in this chunk. This endByte will be used as the startByte parameter when scanning the next
		 * chunk, allowing sequential chunk processing without gaps or overlaps.
		 *
		 * @return ChunkMetadata containing feature IDs, record count, and byte positions
		 */
		private ChunkMetadata createPolygonChunkMetadata() {
			long endByte = currentByte;
			return new ChunkMetadata(featureIds, recordsRead, startByte, endByte);
		}
	}

	/**
	 * Helper class to scan layer records and extract metadata for records matching polygon feature IDs. Scans
	 * sequentially from startByte and stops when all target feature IDs are found or when a non-matching feature ID is
	 * encountered (since files are sorted by FEATURE_ID). Does NOT load actual record data into memory - only scans to
	 * collect matching feature IDs and track byte positions.
	 *
	 * IMPORTANT: startByte points to a data record position (header already skipped by BatchItemReader.open()).
	 */
	private static class LayerChunkScanner {
		private final long startByte;
		private final Set<String> featureIdsInThisChunk;
		private final int lineSeparatorLength;

		private final Set<String> remainingFeatureIds; // Tracks which feature IDs haven't been found yet (for early
														// termination)
		private String lastSeenFeatureId;
		private long currentBytePosition;
		private int matchedLayerRecordCount = 0;

		LayerChunkScanner(
				long currentLayerChunkStartByteOffset, Set<String> featureIdsInThisChunk, int lineSeparatorLength
		) {
			this.startByte = currentLayerChunkStartByteOffset;
			this.featureIdsInThisChunk = featureIdsInThisChunk; // for matching
			this.lineSeparatorLength = lineSeparatorLength;
			this.currentBytePosition = startByte; // Start scanning from startByte directly

			// for tracking which IDs are found (remove operation for early termination)
			this.remainingFeatureIds = new HashSet<>(featureIdsInThisChunk);
		}

		/**
		 * Scans layer records to extract metadata for records matching the target polygon feature IDs. Scans from
		 * startByte until all matching feature IDs are found or a non-matching ID is encountered. Tracks currentByte to
		 * calculate endByte for position tracking.
		 *
		 * @param filePath The layer CSV file to scan
		 * @return ChunkMetadata containing matched layer feature IDs and byte positions
		 * @throws IOException if file scanning fails
		 */
		ChunkMetadata scanChunk(Path filePath) throws IOException {
			try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
				// Position the channel at startByte
				channel.position(startByte);

				try (BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8))) {
					return scanRecords(reader);
				}
			}
		}

		/**
		 * Scans layer records line by line to collect matching feature IDs and track byte positions. Stops when all
		 * target feature IDs are found or EOF is reached.
		 *
		 * @param reader BufferedReader positioned at the starting byte offset
		 * @return ChunkMetadata containing matched feature IDs and byte position information
		 * @throws IOException if reading fails
		 */
		private ChunkMetadata scanRecords(BufferedReader reader) throws IOException {
			String line;
			while ( (line = reader.readLine()) != null) {
				int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + lineSeparatorLength;
				String featureId = BatchUtils.extractFeatureId(line);

				if (shouldProcessRecord(featureId)) {
					processMatchingRecord(featureId);
					// Update currentBytePosition to point to the next record
					currentBytePosition += lineBytes;
				} else if (shouldStopReading(featureId)) {
					break;
				} else {
					// Update currentBytePosition to point to the next record
					currentBytePosition += lineBytes;
				}
			}
			return createLayerChunkMetadata();
		}

		private boolean shouldProcessRecord(String featureId) {
			return featureId != null && !featureId.isEmpty() && featureIdsInThisChunk.contains(featureId);
		}

		/**
		 * Increments record count and tracks first occurrence of feature ID for early termination.
		 *
		 * @param featureId The feature ID of the matching layer record
		 */
		private void processMatchingRecord(String featureId) {
			matchedLayerRecordCount++;

			// On first occurrence of a new feature ID, mark it as found for early termination tracking
			if (lastSeenFeatureId == null || !lastSeenFeatureId.equals(featureId)) {
				remainingFeatureIds.remove(featureId);
				lastSeenFeatureId = featureId;
			}
		}

		/**
		 * Determines if scanning should stop based on two conditions: 1. EOF is reached (featureId is null or empty) 2.
		 * A non-matching feature ID is encountered (since files are sorted by FEATURE_ID)
		 *
		 * @param featureId The current feature ID being examined
		 * @return true if scanning should stop, false otherwise
		 */
		private boolean shouldStopReading(String featureId) {
			logger.trace(
					"shouldStopReading() checking featureId: {}, featureIdsInThisChunk contains: {}", featureId,
					featureIdsInThisChunk
			);

			// 1. Stop at EOF
			if (featureId == null || featureId.isEmpty()) {
				logger.trace("shouldStopReading() returning true: featureId is null or empty");
				return true;
			}

			// 2. Stop when we encounter a non-matching feature ID, since files are sorted by FEATURE_ID:
			boolean shouldStop = !featureIdsInThisChunk.contains(featureId);

			logger.trace(
					"shouldStopReading() returning {}: featureId {} is {}in featureIdsInThisChunk", shouldStop,
					featureId, shouldStop ? "NOT " : ""
			);

			return shouldStop;
		}

		/**
		 * Creates metadata for matched layer records containing record count and byte position information.
		 *
		 * The endByte value represents where scanning stopped - this is the byte offset immediately after the last
		 * matching record processed. If no matches were found, endByte equals startByte to ensure the next chunk starts
		 * from the same position (allowing subsequent chunks a chance to find their matching records).
		 *
		 * @return ChunkMetadata containing record count and byte positions
		 */
		private ChunkMetadata createLayerChunkMetadata() {
			// If no matches found, don't advance the position (next chunk should start from same byte)
			long endByte = matchedLayerRecordCount > 0 ? currentBytePosition : startByte;
			return new ChunkMetadata(Collections.emptyList(), matchedLayerRecordCount, startByte, endByte);
		}
	}

	/**
	 * Holds chunk metadata extracted from CSV file scanning, including feature IDs and byte position information. This
	 * metadata is used for memory-efficient chunk-based processing - it does NOT contain actual record data, only the
	 * information needed to locate and process records later via streaming.
	 */
	private static class ChunkMetadata {
		private final List<String> featureIds;
		private final int recordCount;
		private final long startByte;
		private final long endByte;

		ChunkMetadata(List<String> featureIds, int recordCount, long startByte, long endByte) {
			this.featureIds = featureIds;
			this.recordCount = recordCount;
			this.startByte = startByte;
			this.endByte = endByte;
		}

		List<String> getFeatureIds() {
			return featureIds;
		}

		int getRecordCount() {
			return recordCount;
		}

		long getStartByte() {
			return startByte;
		}

		long getEndByte() {
			return endByte;
		}
	}
}
