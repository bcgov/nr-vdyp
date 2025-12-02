package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchDataReadException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * ItemReader that processes polygon in chunks to handle large datasets
 */
public class ChunkBasedPolygonItemReader implements ItemStreamReader<BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(ChunkBasedPolygonItemReader.class);

	@NonNull
	private final String partitionName;
	private final BatchMetricsCollector metricsCollector;
	@NonNull
	private final Long jobExecutionId;
	@NonNull
	private final String jobGuid;
	private final int chunkSize;

	// File-based reading
	private Path partitionDir;
	private BufferedReader polygonReader;
	private BufferedReader layerReader;

	// Chunk processing state
	private List<String> currentChunk;
	private Iterator<String> chunkIterator;
	private Set<String> currentChunkFeatureIds;
	private Map<String, List<String>> currentChunkLayers;

	// State tracking
	private boolean readerOpened = false;
	private int processedCount = 0;
	private int skippedCount = 0;

	public ChunkBasedPolygonItemReader(
			@NonNull String partitionName, BatchMetricsCollector metricsCollector, @NonNull Long jobExecutionId,
			@NonNull String jobGuid, @NonNull int chunkSize
	) {
		this.partitionName = partitionName;
		this.metricsCollector = metricsCollector;
		this.jobExecutionId = jobExecutionId;
		this.jobGuid = jobGuid;
		this.chunkSize = Math.max(chunkSize, 1);
	}

	/**
	 * Reads the next BatchRecord from the input partition data.
	 *
	 * @return The next BatchRecord to process, or null when all data has been read. Note: Returning null signals
	 *         end-of-data to Spring Batch framework - this is the standard contract.
	 * @throws Exception Required by Spring Batch ItemReader interface contract.
	 */
	@Override
	public BatchRecord read() throws Exception {
		if (!readerOpened) {
			// it's a framework lifecycle violation
			throw new IllegalStateException("Reader not opened. Call open() first.");
		}

		if (!ensureChunkAvailable()) {
			return null; // End of data - signals Spring Batch that reading is complete
		}

		String polygonLine = chunkIterator.next();
		logger.trace(
				"[GUID: {}, Execution ID: {}, Partition: {}] Processing polygon line from chunk: {}", jobGuid,
				jobExecutionId, partitionName,
				polygonLine.length() > 100 ? polygonLine.substring(0, 100) + "..." : polygonLine
		);

		try {
			return processPolygonLine(polygonLine);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			return handlePolygonProcessingException(polygonLine, e);
		}
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Opening ChunkBasedPolygonItemReader with chunk size: {}", jobGuid,
				jobExecutionId, partitionName, chunkSize
		);

		try {
			// Get partition directory from job parameters
			String jobBaseDir = executionContext.getString(BatchConstants.Job.BASE_DIR);
			if (jobBaseDir.trim().isEmpty()) {
				// Framework contract
				throw new ItemStreamException("jobBaseDir not found or empty in ExecutionContext");
			}

			String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
			partitionDir = Paths.get(jobBaseDir, inputPartitionFolderName);
			if (!Files.exists(partitionDir)) {
				// Framework contract
				throw new ItemStreamException("Partition directory does not exist: " + partitionDir);
			}

			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] Reading from partition directory: {}", jobGuid,
					jobExecutionId, partitionName, partitionDir
			);

			initializeReaders();

			readerOpened = true;
			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] ChunkBasedPolygonItemReader opened successfully", jobGuid,
					jobExecutionId, partitionName
			);

		} catch (ItemStreamException ise) {
			// framework contract
			throw ise;
		} catch (Exception e) {
			// Wrap all other exceptions in ItemStreamException (framework contract)
			throw handleReaderInitializationFailure(e, "Failed to initialize ChunkBasedPolygonItemReader");
		}
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putInt(partitionName + ".processed", processedCount);
		executionContext.putInt(partitionName + ".skipped", skippedCount);
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void close() throws ItemStreamException {
		logger.info(
				"[GUID: {}, EXEID: {}, Partition: {}] Closing ChunkBasedPolygonItemReader. Processed: {}, Skipped: {}",
				jobGuid, jobExecutionId, partitionName, processedCount, skippedCount
		);

		closeReader(polygonReader, "polygon");
		closeReader(layerReader, "layer");

		clearChunkData();

		readerOpened = false;
	}

	/**
	 * Initialize BufferedReaders for polygon and layer files.
	 */
	private void initializeReaders() throws BatchDataReadException {
		Path polygonFile = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
		Path layerFile = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

		if (!Files.exists(polygonFile)) {
			FileNotFoundException fileNotFound = new FileNotFoundException("Polygon file not found: " + polygonFile);
			throw BatchDataReadException.handleDataReadFailure(
					fileNotFound, "Polygon file not found", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		try {
			// Initialize polygon reader
			polygonReader = new BufferedReader(new FileReader(polygonFile.toFile()));

			// Initialize layer reader (if file exists)
			if (Files.exists(layerFile)) {
				layerReader = new BufferedReader(new FileReader(layerFile.toFile()));
			} else {
				logger.warn(
						"[GUID: {}, EXEID: {}, Partition: {}] Layer file does not exist: {}", jobGuid, jobExecutionId,
						partitionName, layerFile
				);
			}

			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] Initialized readers - Polygon reader: ready, Layer reader: {}",
					jobGuid, jobExecutionId, partitionName, layerReader != null ? "ready" : "not available"
			);
		} catch (IOException e) {
			throw BatchDataReadException.handleDataReadFailure(
					e, "Failed to open partition input files", jobGuid, jobExecutionId, partitionName, logger
			);
		}
	}

	/**
	 * Load next chunk of polygon and associated layers.
	 *
	 * @return true if chunk was loaded, false if no more data
	 */
	private boolean loadNextChunk() throws BatchDataReadException {
		try {
			clearChunkData();

			currentChunk = new ArrayList<>();
			currentChunkFeatureIds = new HashSet<>();
			currentChunkLayers = new HashMap<>();

			// Read polygon lines for current chunk (skip header lines)
			String line;
			int linesInChunk = 0;
			while (linesInChunk < chunkSize && (line = polygonReader.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					// Skip header lines
					if (BatchUtils.isHeaderLine(line)) {
						logger.trace(
								"[GUID: {}, EXEID: {}, Partition: {}] Skipped header line in polygon file", jobGuid,
								jobExecutionId, partitionName
						);
						continue;
					}

					currentChunk.add(line);
					String featureId = extractFeatureIdFromLine(line);
					if (featureId != null) {
						currentChunkFeatureIds.add(featureId);
					}
					linesInChunk++;
				}
			}

			if (currentChunk.isEmpty()) {
				logger.debug(
						"[GUID: {}, EXEID: {}, Partition: {}] No more polygon to load", jobGuid, jobExecutionId,
						partitionName
				);
				return false;
			}

			// Load associated layers for current chunk's FEATURE_IDs
			loadLayersForCurrentChunk();

			// Initialize chunk iterator
			chunkIterator = currentChunk.iterator();

			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] Loaded chunk with {} polygons and {} unique FEATURE_IDs",
					jobGuid, jobExecutionId, partitionName, currentChunk.size(), currentChunkFeatureIds.size()
			);

			return true;
		} catch (IOException e) {
			throw BatchDataReadException.handleDataReadFailure(
					e, "Failed to read chunk from partition files", jobGuid, jobExecutionId, partitionName, logger
			);
		}
	}

	/**
	 * Load layers associated with FEATURE_IDs in current chunk.
	 *
	 * @throws IOException caller (loadNextChunk) wraps in BatchDataReadException
	 */
	private void loadLayersForCurrentChunk() throws IOException {
		if (layerReader == null || currentChunkFeatureIds.isEmpty()) {
			return;
		}

		// Reset layer reader to beginning
		layerReader.close();

		Path layerFile = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);
		layerReader = new BufferedReader(new FileReader(layerFile.toFile()));

		String line;
		while ( (line = layerReader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				// Skip header lines
				if (BatchUtils.isHeaderLine(line)) {
					logger.trace(
							"[GUID: {}, EXEID: {}, Partition: {}] Skipped header line in layer file", jobGuid,
							jobExecutionId, partitionName
					);
					continue;
				}

				String featureId = extractFeatureIdFromLine(line);
				if (featureId != null && currentChunkFeatureIds.contains(featureId)) {
					currentChunkLayers.computeIfAbsent(featureId, k -> new ArrayList<>()).add(line);
				}
			}
		}

		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Loaded layers for {} FEATURE_IDs in current chunk", jobGuid,
				jobExecutionId, partitionName, currentChunkLayers.size()
		);
	}

	/**
	 * Clear current chunk data to free memory.
	 */
	private void clearChunkData() {
		if (currentChunk != null) {
			currentChunk.clear();
		}
		if (currentChunkFeatureIds != null) {
			currentChunkFeatureIds.clear();
		}
		if (currentChunkLayers != null) {
			currentChunkLayers.clear();
		}
		chunkIterator = null;
	}

	/**
	 * Extract FEATURE_ID from CSV line.
	 *
	 * @param line The CSV line to parse
	 * @return The FEATURE_ID as a String, or null if the line is null or empty. Callers MUST check for null before
	 *         using the returned value.
	 */
	private String extractFeatureIdFromLine(String line) {
		if (line == null || line.trim().isEmpty()) {
			return null;
		}
		int commaIndex = line.indexOf(',');
		return commaIndex > 0 ? line.substring(0, commaIndex).trim() : line.trim();
	}

	/**
	 * Close a BufferedReader safely.
	 */
	private void closeReader(BufferedReader reader, String readerType) {
		if (reader != null) {
			try {
				reader.close();
				logger.debug(
						"[GUID: {}, EXEID: {}, Partition: {}] Closed {} reader", jobGuid, jobExecutionId, partitionName,
						readerType
				);
			} catch (IOException e) {
				logger.warn(
						"[GUID: {}, EXEID: {}, Partition: {}] Failed to close {} reader", jobGuid, jobExecutionId,
						partitionName, readerType, e
				);
			}
		}
	}

	/**
	 * Handle reader initialization failures
	 */
	private ItemStreamException handleReaderInitializationFailure(Exception cause, String errorDescription) {
		performReaderCleanupAfterFailure();

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d, Patition: %s] %s. Partition: %s, Job execution: %s, Chunk size: %d, Exception type: %s, Root cause: %s",
				jobGuid, jobExecutionId, partitionName, errorDescription, partitionName, jobExecutionId, chunkSize,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : BatchConstants.ErrorMessage.NO_ERROR_MESSAGE
		);

		logger.error(contextualMessage, cause);

		if (cause instanceof ItemStreamException itemStreamException) {
			return itemStreamException;
		}
		return new ItemStreamException(contextualMessage, cause);
	}

	/**
	 * Ensure chunk is available for reading. Load new chunk if needed.
	 */
	private boolean ensureChunkAvailable() throws BatchDataReadException {
		if ( (chunkIterator == null || !chunkIterator.hasNext()) && !loadNextChunk()) {
			logger.debug(
					"[GUID: {}, EXEID: {}, Partition: {}] No more chunks to process - returning null", jobGuid,
					jobExecutionId, partitionName
			);
			return false;
		}
		return true;
	}

	/**
	 * Process a polygon line and create BatchRecord.
	 *
	 * @throws Exception Can recursively call read() which declares Exception per framework contract
	 */
	private BatchRecord processPolygonLine(String polygonLine) throws Exception {
		String featureId = extractFeatureIdFromLine(polygonLine);
		if (featureId == null || featureId.trim().isEmpty()) {
			logger.warn(
					"[GUID: {}, EXEID: {}, Partition: {}] Skipping polygon with null/empty FEATURE_ID", jobGuid,
					jobExecutionId, partitionName
			);
			skippedCount++;
			return read(); // Try next
		}

		return createBatchRecord(polygonLine, featureId);
	}

	/**
	 * Create a BatchRecord from polygon line and feature ID.
	 */
	private BatchRecord createBatchRecord(String polygonLine, String featureId) {
		List<String> layerLines = currentChunkLayers.getOrDefault(featureId, new ArrayList<>());

		BatchRecord batchRecord = new BatchRecord(featureId, polygonLine, layerLines, partitionName);

		processedCount++;
		logger.trace(
				"[GUID: {}, EXEID: {} Partition: {}] Created BatchRecord for FEATURE_ID: {} with {} layers", jobGuid,
				jobExecutionId, partitionName, featureId, layerLines.size()
		);

		return batchRecord;
	}

	/**
	 * Handle exceptions during polygon processing.
	 *
	 * @throws Exception Can recursively call read() which declares Exception per framework contract
	 */
	private BatchRecord handlePolygonProcessingException(String polygonLine, Exception e) throws Exception {
		String featureId = extractFeatureIdFromLine(polygonLine);
		logger.error(
				"[GUID: {}, EXEID: {}, Partition: {}] Exception processing polygon FEATURE_ID: {} - Exception: {}",
				jobGuid, jobExecutionId, partitionName, featureId, e.getMessage(), e
		);

		recordSkipMetrics(featureId, e);
		skippedCount++;
		return read(); // Try next - may throw Exception
	}

	/**
	 * Record skip metrics for failed polygon processing.
	 */
	private void recordSkipMetrics(String featureId, Exception e) throws BatchException {
		if (metricsCollector != null && jobExecutionId != null) {
			metricsCollector.recordSkip(jobExecutionId, jobGuid, featureId, e, partitionName);
		}
	}

	/**
	 * Perform cleanup after initialization failure.
	 */
	private void performReaderCleanupAfterFailure() {
		try {
			close();
		} catch (ItemStreamException cleanupException) {
			logger.warn(
					"[GUID: {}, EXEID: {}, Paritition: {}] Failed to cleanup after initialization failure for job execution: {}",
					jobGuid, jobExecutionId, partitionName, jobExecutionId, cleanupException
			);
		}
	}
}
