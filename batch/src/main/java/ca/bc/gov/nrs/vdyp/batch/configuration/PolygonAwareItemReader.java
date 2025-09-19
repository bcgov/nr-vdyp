package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.Polygon;
import ca.bc.gov.nrs.vdyp.batch.model.Layer;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * FEATURE_ID-aware ItemReader that assembles complete Polygon objects for VDYP
 * batch processing.
 *
 * This reader implements the FEATURE_ID-based processing strategy that ensures
 * each BatchRecord contains a complete
 * polygon (1 polygon + all associated layers).
 *
 * Processing Strategy: - Reads both polygon and layer CSV files - Groups all
 * data by FEATURE_ID - Only processes
 * FEATURE_IDs assigned to this partition - Assembles complete BatchRecord
 * objects containing polygon + all layers
 */
public class PolygonAwareItemReader implements ItemReader<BatchRecord>, ItemStream {

	private static final Logger logger = LoggerFactory.getLogger(PolygonAwareItemReader.class);

	private FlatFileItemReader<Polygon> polygonReader;
	private FlatFileItemReader<Layer> layerReader;

	private String partitionName;
	private Set<Long> assignedFeatureIds;
	private long processedCount = 0;
	private long skippedCount = 0;
	private boolean readerOpened = false;

	// Job execution context for metrics
	private Long jobExecutionId;

	// Metrics collector for skip tracking
	private final BatchMetricsCollector metricsCollector;

	// Polygon and layer data storage
	private Map<Long, Polygon> polygonDataMap = new HashMap<>();
	private Map<Long, List<Layer>> layerDataMap = new HashMap<>();
	private Iterator<Long> featureIdIterator;

	public PolygonAwareItemReader(
			Resource polygonResource, Resource layerResource, BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;

		// Initialize polygon reader
		BeanWrapperFieldSetMapper<Polygon> polygonMapper = new BeanWrapperFieldSetMapper<>();
		polygonMapper.setTargetType(Polygon.class);

		this.polygonReader = new FlatFileItemReaderBuilder<Polygon>().name("polygonReader").resource(polygonResource)
				.delimited().delimiter(",").names(getPolygonFieldNames()).fieldSetMapper(polygonMapper).linesToSkip(1) // Skip
																														// header
				.build();

		// Initialize layer reader
		BeanWrapperFieldSetMapper<Layer> layerMapper = new BeanWrapperFieldSetMapper<>();
		layerMapper.setTargetType(Layer.class);

		this.layerReader = new FlatFileItemReaderBuilder<Layer>().name("layerReader").resource(layerResource)
				.delimited().delimiter(",").names(getLayerFieldNames()).fieldSetMapper(layerMapper).linesToSkip(1) // Skip
																													// header
				.build();
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		this.partitionName = executionContext.getString("partitionName", "unknown");

		// Parse assigned FEATURE_IDs for this partition
		String assignedFeatureIdsStr = executionContext.getString("assignedFeatureIds", "");
		this.assignedFeatureIds = parseAssignedFeatureIds(assignedFeatureIdsStr);

		logger.info(
				"[{}] PolygonAwareItemReader initialized with {} assigned FEATURE_IDs: {}", partitionName,
				assignedFeatureIds.size(),
				assignedFeatureIds.size() > 10 ? assignedFeatureIds.size() + " FEATURE_IDs" : assignedFeatureIds);
	}

	@Override
	public BatchRecord read() throws Exception {
		if (!readerOpened) {
			throw new IllegalStateException("ItemReader not opened. Call open() first.");
		}

		if (featureIdIterator == null) {
			return null;
		}

		// Get next FEATURE_ID to process
		if (!featureIdIterator.hasNext()) {
			logger.info(
					"[{}] Completed processing all assigned FEATURE_IDs. Processed: {}, Skipped: {}", partitionName,
					processedCount, skippedCount);
			return null;
		}

		Long featureId = featureIdIterator.next();

		try {
			// Get polygon data for this FEATURE_ID
			Polygon polygonData = polygonDataMap.get(featureId);
			if (polygonData == null) {
				logger.warn("[{}] No polygon data found for FEATURE_ID: {}", partitionName, featureId);
				skippedCount++;
				return read(); // Try next FEATURE_ID
			}

			// Get all layer data for this FEATURE_ID
			List<Layer> layerDataList = layerDataMap.getOrDefault(featureId, new ArrayList<>());

			// Create complete BatchRecord with polygon + all layers
			BatchRecord batchRecord = new BatchRecord();
			batchRecord.setFeatureId(String.valueOf(featureId));
			batchRecord.setPolygon(polygonData);
			batchRecord.setLayers(layerDataList);

			processedCount++;

			logger.debug(
					"[{}] Assembled complete BatchRecord for FEATURE_ID: {} (1 polygon + {} layers)", partitionName,
					featureId, layerDataList.size());

			return batchRecord;

		} catch (Exception e) {
			logger.error("[{}] Error processing FEATURE_ID: {}", partitionName, featureId, e);
			skippedCount++;

			// Report skip to metrics collector
			if (metricsCollector != null && jobExecutionId != null) {
				metricsCollector.recordSkip(jobExecutionId, featureId, null, e, partitionName, null);
			}

			return read(); // Try next FEATURE_ID
		}
	}

	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		logger.info("[{}] Opening PolygonAwareItemReader...", partitionName);

		try {
			// Open both readers
			polygonReader.open(executionContext);
			layerReader.open(executionContext);

			// Load all polygon data
			loadAllPolygonData();

			// Load all layer data
			loadAllLayerData();

			// Filter and create iterator for assigned FEATURE_IDs only
			List<Long> availableFeatureIds = assignedFeatureIds.stream().filter(polygonDataMap::containsKey).toList();

			this.featureIdIterator = availableFeatureIds.iterator();

			logger.info(
					"[{}] PolygonAwareItemReader opened successfully. Available FEATURE_IDs: {}, Total layers loaded: {}",
					partitionName, availableFeatureIds.size(),
					layerDataMap.values().stream().mapToInt(List::size).sum());

			readerOpened = true;

		} catch (ItemStreamException ise) {
			// Handle ItemStreamException with cleanup and enhanced context
			throw handleReaderInitializationFailure(
					ise, "ItemStreamException during PolygonAwareItemReader initialization");
		} catch (Exception e) {
			// Handle all other exceptions with cleanup and wrap in ItemStreamException
			throw handleReaderInitializationFailure(
					e, "Unexpected failure during PolygonAwareItemReader initialization");
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putLong(partitionName + ".processedCount", processedCount);
		executionContext.putLong(partitionName + ".skippedCount", skippedCount);
	}

	@Override
	public void close() throws ItemStreamException {
		logger.info(
				"[{}] Closing PolygonAwareItemReader. Final stats - Processed: {}, Skipped: {}", partitionName,
				processedCount, skippedCount);

		Exception closingException = null;

		try {
			if (polygonReader != null) {
				polygonReader.close();
			}
		} catch (Exception e) {
			closingException = e;
			logger.error("[{}] Error closing polygon reader", partitionName, e);
		}

		try {
			if (layerReader != null) {
				layerReader.close();
			}
		} catch (Exception e) {
			if (closingException == null) {
				closingException = e;
			}
			logger.error("[{}] Error closing layer reader", partitionName, e);
		}

		// Always clean up data structures, even if close() operations failed
		try {
			polygonDataMap.clear();
			layerDataMap.clear();
			readerOpened = false;
		} catch (Exception e) {
			logger.warn("[{}] Error during cleanup of data structures", partitionName, e);
		}

		// If any closing operation failed, throw with contextual information
		if (closingException != null) {
			String contextualMessage = String.format(
					"[%s] Failed to properly close PolygonAwareItemReader. Partition: %s, Processed: %d, Skipped: %d, Root cause: %s",
					partitionName, partitionName, processedCount, skippedCount, closingException.getMessage());
			logger.error(contextualMessage, closingException);
			throw new ItemStreamException(contextualMessage, closingException);
		}
	}

	/**
	 * Load all polygon data into memory for efficient FEATURE_ID-based lookup.
	 *
	 * This ensures it has complete polygon data before attempting to assemble
	 * complete Polygon objects for projection.
	 */
	private void loadAllPolygonData() throws Exception {
		logger.info("[{}] Loading polygon data...", partitionName);

		Polygon polygonData;
		int polygonCount = 0;

		while ((polygonData = polygonReader.read()) != null) {
			try {
				// Skip rows with empty or null FEATURE_ID
				String featureIdStr = polygonData.getFeatureId();
				if (featureIdStr == null || featureIdStr.trim().isEmpty()) {
					logger.debug("[{}] Skipping polygon row with empty FEATURE_ID", partitionName);
					continue;
				}

				Long featureId = Long.parseLong(featureIdStr.trim());
				polygonDataMap.put(featureId, polygonData);
				polygonCount++;

				logger.debug("[{}] Loaded polygon data for FEATURE_ID: {}", partitionName, featureId);

			} catch (NumberFormatException e) {
				logger.warn(
						"[{}] Invalid FEATURE_ID in polygon data: '{}' - skipping row", partitionName,
						polygonData.getFeatureId());
			}
		}

		logger.info("[{}] Loaded {} polygon records", partitionName, polygonCount);
	}

	/**
	 * Load all layer data into memory, grouped by FEATURE_ID.
	 *
	 * This ensures its has all layer data for each polygon before assembling
	 * complete Polygon objects for projection.
	 */
	private void loadAllLayerData() throws Exception {
		logger.info("[{}] Loading layer data...", partitionName);

		Layer layerData;
		int layerCount = 0;

		while ((layerData = layerReader.read()) != null) {
			try {
				// Skip rows with empty or null FEATURE_ID
				String featureIdStr = layerData.getFeatureId();
				if (featureIdStr == null || featureIdStr.trim().isEmpty()) {
					logger.debug("[{}] Skipping layer row with empty FEATURE_ID", partitionName);
					continue;
				}

				Long featureId = Long.parseLong(featureIdStr.trim());

				layerDataMap.computeIfAbsent(featureId, k -> new ArrayList<>()).add(layerData);
				layerCount++;

				logger.debug("[{}] Loaded layer data for FEATURE_ID: {}", partitionName, featureId);

			} catch (NumberFormatException e) {
				logger.warn(
						"[{}] Invalid FEATURE_ID in layer data: '{}' - skipping row", partitionName,
						layerData.getFeatureId());
			}
		}

		logger.info(
				"[{}] Loaded {} layer records for {} unique FEATURE_IDs", partitionName, layerCount,
				layerDataMap.size());
	}

	/**
	 * Handles reader initialization failures by performing cleanup, logging, and
	 * creating appropriate exception.
	 *
	 * @param cause            The original exception that caused the failure
	 * @param errorDescription A description of the type of error that occurred
	 * @return ItemStreamException with enhanced context
	 */
	private ItemStreamException handleReaderInitializationFailure(Exception cause, String errorDescription) {
		// Perform cleanup of reader resources
		performCleanupAfterFailure();

		// Create enhanced contextual message
		String contextualMessage = String.format(
				"[%s] %s. Partition: %s, Assigned FEATURE_IDs: %d, Exception type: %s, Root cause: %s", partitionName,
				errorDescription, partitionName, assignedFeatureIds != null ? assignedFeatureIds.size() : 0,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message available");

		// Log the failure with full context
		logger.error(contextualMessage, cause);

		// Return appropriate exception with enhanced context
		return new ItemStreamException(contextualMessage, cause);
	}

	/**
	 * Performs cleanup operations after a failure during open() method. This method
	 * safely handles cleanup without throwing exceptions.
	 */
	private void performCleanupAfterFailure() {
		try {
			close();
		} catch (Exception cleanupException) {
			logger.warn("[{}] Failed to cleanup after open() failure", partitionName, cleanupException);
		}
	}

	/**
	 * Parse assigned FEATURE_IDs from partition context.
	 */
	private Set<Long> parseAssignedFeatureIds(String assignedFeatureIdsStr) {
		if (assignedFeatureIdsStr == null || assignedFeatureIdsStr.trim().isEmpty()) {
			logger.warn("[{}] No assigned FEATURE_IDs found in partition context", partitionName);
			return new HashSet<>();
		}

		return Arrays.stream(assignedFeatureIdsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.map(Long::parseLong).collect(HashSet::new, HashSet::add, HashSet::addAll);
	}

	/**
	 * Get polygon CSV field names for mapping.
	 */
	private String[] getPolygonFieldNames() {
		return new String[] { "featureId", "mapId", "polygonNumber", "orgUnit", "tsaName", "tflName",
				"inventoryStandardCode", "tsaNumber", "shrubHeight", "shrubCrownClosure", "shrubCoverPattern",
				"herbCoverTypeCode", "herbCoverPct", "herbCoverPatternCode", "bryoidCoverPct", "becZoneCode",
				"cfsEcozone", "preDisturbanceStockability", "yieldFactor", "nonProductiveDescriptorCd",
				"bclcsLevel1Code", "bclcsLevel2Code", "bclcsLevel3Code", "bclcsLevel4Code", "bclcsLevel5Code",
				"photoEstimationBaseYear", "referenceYear", "pctDead", "nonVegCoverType1", "nonVegCoverPct1",
				"nonVegCoverPattern1", "nonVegCoverType2", "nonVegCoverPct2", "nonVegCoverPattern2", "nonVegCoverType3",
				"nonVegCoverPct3", "nonVegCoverPattern3", "landCoverClassCd1", "landCoverPct1", "landCoverClassCd2",
				"landCoverPct2", "landCoverClassCd3", "landCoverPct3" };
	}

	/**
	 * Get layer CSV field names for mapping.
	 */
	private String[] getLayerFieldNames() {
		return new String[] { "featureId", "treeCoverLayerEstimatedId", "mapId", "polygonNumber", "layerLevelCode",
				"vdyp7LayerCd", "layerStockability", "forestCoverRankCode", "nonForestDescriptorCode",
				"estSiteIndexSpeciesCd", "estimatedSiteIndex", "crownClosure", "basalArea75", "stemsPerHa75",
				"speciesCd1", "speciesPct1", "speciesCd2", "speciesPct2", "speciesCd3", "speciesPct3", "speciesCd4",
				"speciesPct4", "speciesCd5", "speciesPct5", "speciesCd6", "speciesPct6", "estAgeSpp1", "estHeightSpp1",
				"estAgeSpp2", "estHeightSpp2", "adjInd", "loreyHeight75", "basalArea125", "wsVolPerHa75",
				"wsVolPerHa125", "cuVolPerHa125", "dVolPerHa125", "dwVolPerHa125" };
	}
}