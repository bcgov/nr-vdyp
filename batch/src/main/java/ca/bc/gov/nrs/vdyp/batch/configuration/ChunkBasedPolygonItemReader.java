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
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * ItemReader that processes polygon in chunks to handle large datasets
 */
public class ChunkBasedPolygonItemReader implements ItemStreamReader<BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(ChunkBasedPolygonItemReader.class);

	@NonNull
	private final String partitionName;
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

	public ChunkBasedPolygonItemReader(
			@NonNull String partitionName, @NonNull Long jobExecutionId, @NonNull String jobGuid, @NonNull int chunkSize
	) {
		this.partitionName = partitionName;
		this.jobExecutionId = jobExecutionId;
		this.jobGuid = jobGuid;
		this.chunkSize = Math.max(chunkSize, 1);
	}

	/**
	 * Reads the next BatchRecord from the input partition data.
	 *
	 * @return The next BatchRecord to process, or null when all data has been read. Note: Returning null signals
	 *         end-of-data to Spring Batch framework - this is the standard contract.
	 * @throws BatchDataReadException
	 */
	@Override
	public BatchRecord read() throws BatchDataReadException {
		if (!readerOpened) {
			throw BatchDataReadException.handleDataReadFailure(
					new IllegalStateException("Reader not opened. Call open() first."),
					"Reader not opened. Call open() first.", jobGuid, jobExecutionId, partitionName, logger
			);
		}

		try {
			if (!ensureChunkAvailable()) {
				return null; // End of data - signals Spring Batch that reading is complete
			}

			String polygonLine = chunkIterator.next();
			logger.trace(
					"[GUID: {}, Execution ID: {}, Partition: {}] Processing polygon line from chunk: {}", jobGuid,
					jobExecutionId, partitionName,
					polygonLine.length() > 100 ? polygonLine.substring(0, 100) + "..." : polygonLine
			);

			return processPolygonLine(polygonLine);

		} catch (IOException | IllegalArgumentException e) {
			throw BatchDataReadException.handleDataReadFailure(
					e, "Failed to read data from partition", jobGuid, jobExecutionId, partitionName, logger
			);
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
				// ItemStream.open() interface only supports ItemStreamException, not BatchException subclasses.
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new IllegalArgumentException("jobBaseDir is empty"),
						"jobBaseDir not found or empty in ExecutionContext", jobGuid, jobExecutionId, partitionName,
						logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
			}

			String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
			partitionDir = Paths.get(jobBaseDir, inputPartitionFolderName);
			if (!Files.exists(partitionDir)) {
				// ItemStream.open() interface only supports ItemStreamException, not BatchException subclasses.
				BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
						new FileNotFoundException("Partition directory does not exist: " + partitionDir),
						"Partition directory does not exist", jobGuid, jobExecutionId, partitionName, logger
				);
				throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
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

		} catch (IOException e) {
			// Cleanup any partially initialized readers before throwing
			try {
				close();
			} catch (ItemStreamException cleanupException) {
				logger.warn(
						"[GUID: {}, EXEID: {}, Partition: {}] Failed to cleanup readers after initialization failure",
						jobGuid, jobExecutionId, partitionName, cleanupException
				);
			}

			// ItemStream.open() interface only supports ItemStreamException, not BatchException subclasses.
			BatchDataReadException dataReadException = BatchDataReadException.handleDataReadFailure(
					e, "Failed to initialize partition readers", jobGuid, jobExecutionId, partitionName, logger
			);
			throw new ItemStreamException(dataReadException.getMessage(), dataReadException);
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) {
		//
	}

	/**
	 * @throws ItemStreamException Required by Spring Batch ItemStreamReader interface contract.
	 */
	@Override
	public void close() throws ItemStreamException {
		logger.debug(
				"[GUID: {}, EXEID: {}, Partition: {}] Closing ChunkBasedPolygonItemReader. Processed: {}", jobGuid,
				jobExecutionId, partitionName, processedCount
		);

		closeReader(polygonReader, "polygon");
		closeReader(layerReader, "layer");

		clearChunkData();

		readerOpened = false;
	}

	/**
	 * Initialize BufferedReaders for polygon and layer files.
	 *
	 * @throws IOException if reader initialization fails
	 */
	private void initializeReaders() throws IOException {
		Path polygonFile = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
		Path layerFile = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

		if (!Files.exists(polygonFile)) {
			throw new FileNotFoundException("Polygon file not found: " + polygonFile);
		}

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
	}

	/**
	 * Load next chunk of polygon and associated layers.
	 *
	 * @return true if chunk was loaded, false if no more data
	 * @throws IOException if reading fails
	 */
	private boolean loadNextChunk() throws IOException {
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
				"[GUID: {}, EXEID: {}, Partition: {}] Loaded chunk with {} polygons and {} unique FEATURE_IDs", jobGuid,
				jobExecutionId, partitionName, currentChunk.size(), currentChunkFeatureIds.size()
		);

		return true;
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
	 * Ensure chunk is available for reading. Load new chunk if needed.
	 *
	 * @throws IOException if chunk loading fails
	 */
	private boolean ensureChunkAvailable() throws IOException {
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
	 * @throws IllegalArgumentException if featureId is null or empty
	 */
	private BatchRecord processPolygonLine(String polygonLine) throws IllegalArgumentException {
		String featureId = extractFeatureIdFromLine(polygonLine);
		if (featureId == null || featureId.trim().isEmpty()) {
			throw new IllegalArgumentException("FEATURE_ID is null or empty");
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

}
