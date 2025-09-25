package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.lang.NonNull;
import org.springframework.batch.item.ItemStreamReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Memory-efficient ItemReader that processes polygon data in configurable chunks.
 * 
 * This reader implements chunked processing to handle large datasets efficiently:
 * - Reads polygon and layer CSV files in memory-bounded chunks
 * - Groups data by FEATURE_ID for complete polygon processing
 * - Stores raw CSV data to minimize memory usage and parsing overhead
 * - Supports lazy loading with configurable chunk sizes
 */
public class ChunkBasedPolygonItemReader implements ItemStreamReader<BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(ChunkBasedPolygonItemReader.class);

	private final String partitionName;
	private final BatchMetricsCollector metricsCollector;
	private final Long jobExecutionId;
	private final int chunkSize;

	// File-based reading
	private Path partitionDir;
	private BufferedReader polygonReader;
	private BufferedReader layerReader;
	private String polygonHeader;
	private String layerHeader;

	// Chunk processing state
	private List<String> currentChunk;
	private Iterator<String> chunkIterator;
	private Set<String> currentChunkFeatureIds;
	private Map<String, List<String>> currentChunkLayers;

	// State tracking
	private boolean readerOpened = false;
	private int processedCount = 0;
	private int skippedCount = 0;

	public ChunkBasedPolygonItemReader(String partitionName, BatchMetricsCollector metricsCollector,
			Long jobExecutionId, int chunkSize) {
		this.partitionName = partitionName != null ? partitionName : "unknown";
		this.metricsCollector = metricsCollector;
		this.jobExecutionId = jobExecutionId;
		this.chunkSize = Math.max(chunkSize, 1); // Ensure minimum chunk size of 1
	}

	@Override
	public BatchRecord read() throws Exception {
		if (!readerOpened) {
			throw new IllegalStateException("Reader not opened. Call open() first.");
		}

		if (!ensureChunkAvailable()) {
			return null; // End of data
		}

		String polygonLine = chunkIterator.next();
		logPolygonProcessing(polygonLine);

		try {
			return processPolygonLine(polygonLine);
		} catch (Exception e) {
			return handlePolygonProcessingException(polygonLine, e);
		}
	}

	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		logger.info("[{}] Opening ChunkBasedPolygonItemReader with chunk size: {}", partitionName, chunkSize);

		try {
			// Get partition directory from job parameters
			String partitionBaseDir = executionContext.getString("partitionBaseDir");
			if (partitionBaseDir.trim().isEmpty()) {
				throw new ItemStreamException("partitionBaseDir not found or empty in ExecutionContext");
			}

			partitionDir = Paths.get(partitionBaseDir, partitionName);
			if (!Files.exists(partitionDir)) {
				throw new ItemStreamException("Partition directory does not exist: " + partitionDir);
			}

			logger.info("[{}] Reading from partition directory: {}", partitionName, partitionDir);

			initializeReaders();

			readerOpened = true;
			logger.info("[{}] ChunkBasedPolygonItemReader opened successfully", partitionName);

		} catch (ItemStreamException ise) {
			throw ise;
		} catch (Exception e) {
			throw handleReaderInitializationFailure(e, "Failed to initialize ChunkBasedPolygonItemReader");
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putInt(partitionName + ".processed", processedCount);
		executionContext.putInt(partitionName + ".skipped", skippedCount);
	}

	@Override
	public void close() throws ItemStreamException {
		logger.info("[{}] Closing ChunkBasedPolygonItemReader. Processed: {}, Skipped: {}",
				partitionName, processedCount, skippedCount);

		closeReader(polygonReader, "polygon");
		closeReader(layerReader, "layer");

		clearChunkData();

		readerOpened = false;
	}

	/**
	 * Initialize BufferedReaders for polygon and layer files.
	 */
	private void initializeReaders() throws IOException {
		Path polygonFile = partitionDir.resolve("polygons.csv");
		Path layerFile = partitionDir.resolve("layers.csv");

		if (!Files.exists(polygonFile)) {
			throw new IOException("Polygon file not found: " + polygonFile);
		}

		// Initialize polygon reader and read header
		polygonReader = new BufferedReader(new FileReader(polygonFile.toFile()));
		polygonHeader = polygonReader.readLine();
		if (polygonHeader == null) {
			throw new IOException("Polygon file is empty or has no header");
		}

		// Initialize layer reader and read header (if file exists)
		if (Files.exists(layerFile)) {
			layerReader = new BufferedReader(new FileReader(layerFile.toFile()));
			layerHeader = layerReader.readLine();
		} else {
			logger.warn("[{}] Layer file does not exist: {}", partitionName, layerFile);
			layerHeader = ""; // Empty header for missing layer file
		}

		logger.info("[{}] Initialized readers - Polygon header: present, Layer header present: {}",
				partitionName, layerHeader != null);
	}

	/**
	 * Load next chunk of polygon data and associated layers.
	 * 
	 * @return true if chunk was loaded, false if no more data
	 */
	private boolean loadNextChunk() throws IOException {
		clearChunkData();

		currentChunk = new ArrayList<>();
		currentChunkFeatureIds = new HashSet<>();
		currentChunkLayers = new HashMap<>();

		// Read polygon lines for current chunk
		String line;
		int linesInChunk = 0;
		while (linesInChunk < chunkSize && (line = polygonReader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				currentChunk.add(line);
				String featureId = extractFeatureIdFromLine(line);
				if (featureId != null) {
					currentChunkFeatureIds.add(featureId);
				}
				linesInChunk++;
			}
		}

		if (currentChunk.isEmpty()) {
			logger.debug("[{}] No more polygon data to load", partitionName);
			return false;
		}

		// Load associated layers for current chunk's FEATURE_IDs
		loadLayersForCurrentChunk();

		// Initialize chunk iterator
		chunkIterator = currentChunk.iterator();

		logger.debug("[{}] Loaded chunk with {} polygons and {} unique FEATURE_IDs",
				partitionName, currentChunk.size(), currentChunkFeatureIds.size());

		return true;
	}

	/**
	 * Load layers associated with FEATURE_IDs in current chunk.
	 */
	private void loadLayersForCurrentChunk() throws IOException {
		if (layerReader == null || currentChunkFeatureIds.isEmpty()) {
			return;
		}

		// Reset layer reader to beginning (after header)
		layerReader.close();
		Path layerFile = partitionDir.resolve("layers.csv");
		layerReader = new BufferedReader(new FileReader(layerFile.toFile()));
		String header = layerReader.readLine(); // Skip header
		if (header == null) {
			logger.warn("[{}] Layer file has no header", partitionName);
		}

		String line;
		while ((line = layerReader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				String featureId = extractFeatureIdFromLine(line);
				if (featureId != null && currentChunkFeatureIds.contains(featureId)) {
					currentChunkLayers.computeIfAbsent(featureId, k -> new ArrayList<>()).add(line);
				}
			}
		}

		logger.debug("[{}] Loaded layers for {} FEATURE_IDs in current chunk",
				partitionName, currentChunkLayers.size());
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
	 * Extract FEATURE_ID from CSV line
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
				logger.debug("[{}] Closed {} reader", partitionName, readerType);
			} catch (IOException e) {
				logger.warn("[{}] Failed to close {} reader", partitionName, readerType, e);
			}
		}
	}

	/**
	 * Handle reader initialization failures
	 */
	private ItemStreamException handleReaderInitializationFailure(Exception cause, String errorDescription) {
		performReaderCleanupAfterFailure();

		String contextualMessage = String.format(
				"[%s] %s. Partition: %s, Job execution: %s, Chunk size: %d, Exception type: %s, Root cause: %s",
				partitionName, errorDescription, partitionName, jobExecutionId, chunkSize,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message available");

		logger.error(contextualMessage, cause);

		if (cause instanceof ItemStreamException itemStreamException) {
			return itemStreamException;
		}
		return new ItemStreamException(contextualMessage, cause);
	}

	/**
	 * Ensure chunk is available for reading. Load new chunk if needed.
	 */
	private boolean ensureChunkAvailable() throws IOException {
		if ((chunkIterator == null || !chunkIterator.hasNext()) && !loadNextChunk()) {
			logger.info("[{}] No more chunks to process - returning null", partitionName);
			return false;
		}
		return true;
	}

	/**
	 * Log polygon processing details.
	 */
	private void logPolygonProcessing(String polygonLine) {
		logger.debug("[{}] Processing polygon line from chunk: {}", partitionName,
				polygonLine.length() > 100 ? polygonLine.substring(0, 100) + "..." : polygonLine);
	}

	/**
	 * Process a polygon line and create BatchRecord.
	 */
	private BatchRecord processPolygonLine(String polygonLine) throws Exception {
		String featureId = extractFeatureIdFromLine(polygonLine);
		if (featureId == null || featureId.trim().isEmpty()) {
			logger.warn("[{}] Skipping polygon with null/empty FEATURE_ID", partitionName);
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

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(featureId);
		batchRecord.setRawPolygonData(polygonLine);
		batchRecord.setRawLayerData(layerLines);
		batchRecord.setPolygonHeader(polygonHeader);
		batchRecord.setLayerHeader(layerHeader);
		batchRecord.setPartitionName(partitionName);

		processedCount++;
		logger.debug("[{}] Created BatchRecord for FEATURE_ID: {} with {} layers",
				partitionName, featureId, layerLines.size());

		return batchRecord;
	}

	/**
	 * Handle exceptions during polygon processing.
	 */
	private BatchRecord handlePolygonProcessingException(String polygonLine, Exception e) throws Exception {
		String featureId = extractFeatureIdFromLine(polygonLine);
		logger.error("[{}] Exception processing polygon FEATURE_ID: {} - Exception: {}",
				partitionName, featureId, e.getMessage(), e);

		recordSkipMetrics(featureId, e);
		skippedCount++;
		return read(); // Try next
	}

	/**
	 * Record skip metrics for failed polygon processing.
	 */
	private void recordSkipMetrics(String featureId, Exception e) {
		if (metricsCollector != null && jobExecutionId != null) {
			try {
				Long featureIdLong = featureId != null ? Long.parseLong(featureId) : null;
				metricsCollector.recordSkip(jobExecutionId, featureIdLong, null, e, partitionName, null);
			} catch (NumberFormatException nfe) {
				metricsCollector.recordSkip(jobExecutionId, null, null, e, partitionName, null);
			}
		}
	}

	/**
	 * Perform cleanup after initialization failure.
	 */
	private void performReaderCleanupAfterFailure() {
		try {
			close();
		} catch (Exception cleanupException) {
			logger.warn("[{}] Failed to cleanup after initialization failure for job execution: {}",
					partitionName, jobExecutionId, cleanupException);
		}
	}
}
