package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VdypProjectionProcessor implements ItemProcessor<BatchRecord, BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionProcessor.class);

	private final BatchRetryPolicy retryPolicy;
	private final BatchMetricsCollector metricsCollector;
	private final VdypProjectionService vdypProjectionService;

	// Partition context information
	private String partitionName = "unknown";
	private Long jobExecutionId;

	// Track records that have been successfully retried
	private static final Set<String> retriedRecords = ConcurrentHashMap.newKeySet();

	// Validation thresholds
	@Value("${batch.validation.max-data-length:50000}")
	private int maxDataLength;

	@Value("${batch.validation.min-polygon-id-length:1}")
	private int minPolygonIdLength;

	@Value("${batch.validation.max-polygon-id-length:50}")
	private int maxPolygonIdLength;

	public VdypProjectionProcessor(
			BatchRetryPolicy retryPolicy, BatchMetricsCollector metricsCollector,
			VdypProjectionService vdypProjectionService) {
		this.retryPolicy = retryPolicy;
		this.metricsCollector = metricsCollector;
		this.vdypProjectionService = vdypProjectionService;
	}

	/**
	 * Initialize processor with step execution context.
	 */
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.partitionName = stepExecution.getExecutionContext().getString("partitionName", "unknown");
		long startLine = stepExecution.getExecutionContext().getLong("startLine", 0);
		long endLine = stepExecution.getExecutionContext().getLong("endLine", 0);

		// Initialize partition metrics
		if (metricsCollector != null) {
			metricsCollector.initializePartitionMetrics(jobExecutionId, partitionName, startLine, endLine);
		}

		logger.info(
				"[{}] VDYP Projection Processor initialized for job {} range {}-{}", partitionName, jobExecutionId,
				startLine, endLine);
	}

	/**
	 * Process a single record
	 *
	 * Processing flow:
	 * 1. Register record with retry policy for tracking
	 * 2. Validate record data quality (throws IllegalArgumentException for
	 * skippable issues)
	 * 3. Perform projection processing with proper error handling
	 * 4. Record retry success if this record was previously retried
	 *
	 * @param batchRecord The data record to process
	 * @return The processed record with projection results
	 * @throws IOException              for retryable errors (network, timeout,
	 *                                  transient issues)
	 * @throws IllegalArgumentException for skippable errors (data validation
	 *                                  failures)
	 */
	@Override
	public BatchRecord process(@NonNull BatchRecord batchRecord) throws IOException, IllegalArgumentException {
		String featureId = batchRecord.getFeatureId();

		// Register record with retry policy for tracking (using featureId hash as
		// featureId)
		if (retryPolicy != null && featureId != null) {
			retryPolicy.registerRecord((long) featureId.hashCode(), batchRecord);
		}

		// Validate record data quality - throws IllegalArgumentException for skippable
		// issues
		validateRecordForProcessing(batchRecord);

		// Perform projection processing
		String projectionResult = performVdypProjectionWithErrorHandling(batchRecord);

		// Store the projection result in the record
		batchRecord.setProjectionResult(projectionResult);

		// Check if this record was previously retried and notify success
		if (featureId != null) {
			String retryKey = partitionName + "_" + featureId;
			if (retryPolicy != null && retriedRecords.contains(retryKey)) {
				retryPolicy.onRetrySuccess((long) featureId.hashCode(), batchRecord);
				retriedRecords.remove(retryKey);
				logger.info(
						"[{}] VDYP Retry success recorded for job {} Feature ID {}", partitionName, jobExecutionId,
						featureId);
			}
		}

		return batchRecord;
	}

	/**
	 * Validate record data quality for production processing. Throws
	 * IllegalArgumentException for data quality issues
	 * that should be skipped.
	 */
	private void validateRecordForProcessing(BatchRecord batchRecord) throws IllegalArgumentException {
		String featureId = batchRecord.getFeatureId();

		// Validate required fields
		if (batchRecord.getFeatureId() == null || batchRecord.getFeatureId().trim().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Missing required VDYP feature ID for Feature ID %s", featureId));
		}

		if (batchRecord.getPolygon() == null) {
			throw new IllegalArgumentException(
					String.format("Missing required polygon data for Feature ID %s", featureId));
		}

		if (batchRecord.getLayers() == null || batchRecord.getLayers().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Missing required layer data for Feature ID %s", featureId));
		}

		// Validate polygon data
		if (batchRecord.getPolygon().getMapId() == null || batchRecord.getPolygon().getMapId().trim().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Missing required map ID in polygon data for Feature ID %s", featureId));
		}

		if (batchRecord.getPolygon().getPolygonNumber() == null) {
			throw new IllegalArgumentException(
					String.format("Missing required polygon number for Feature ID %s", featureId));
		}
	}

	/**
	 * Perform VDYP projection processing with proper error handling.
	 *
	 * This method handles both retryable errors (IOException) and non-retryable
	 * validation errors.
	 */
	private String performVdypProjectionWithErrorHandling(BatchRecord batchRecord)
			throws IOException, IllegalArgumentException {
		try {
			String result = performVdypProjection(batchRecord);
			return validateProjectionResult(result, batchRecord.getFeatureId());
		} catch (Exception e) {
			handleProjectionException(e, batchRecord);
			reclassifyAndThrowException(e, batchRecord.getFeatureId());
			return null;
		}
	}

	/**
	 * Validates the projection result and throws IOException if empty.
	 */
	private String validateProjectionResult(String result, String featureId) throws IOException {
		if (result == null || result.trim().isEmpty()) {
			throw new IOException(String.format("VDYP projection returned empty result for Feature ID %s", featureId));
		}
		return result;
	}

	/**
	 * Handles exception by recording appropriate metrics.
	 */
	private void handleProjectionException(Exception e, BatchRecord batchRecord) {
		if (metricsCollector != null && jobExecutionId != null) {
			String featureId = batchRecord.getFeatureId();
			long recordIdHash = featureId != null ? (long) featureId.hashCode() : 0L;

			if (isRetryableException(e)) {
				metricsCollector
						.recordRetryAttempt(jobExecutionId, recordIdHash, batchRecord, 1, e, false, partitionName);
			} else {
				metricsCollector.recordSkip(jobExecutionId, recordIdHash, batchRecord, e, partitionName, null);
			}
		}
	}

	/**
	 * Determines if an exception should be retried.
	 */
	private boolean isRetryableException(Exception e) {
		return e instanceof IOException || (e instanceof RuntimeException && isTransientError(e));
	}

	/**
	 * Reclassifies and throws exceptions for proper Spring Batch handling.
	 */
	private void reclassifyAndThrowException(Exception e, String featureId)
			throws IOException, IllegalArgumentException {
		if (e instanceof IOException ioException) {
			throw ioException;
		}

		if (e instanceof IllegalArgumentException illegalArgException) {
			throw illegalArgException;
		}

		if (e instanceof RuntimeException && isTransientError(e)) {
			throw new IOException("Transient error during VDYP projection for Feature ID " + featureId, e);
		}

		// Unknown errors treated as data quality issues
		throw new IllegalArgumentException(
				"VDYP projection failed for Feature ID " + featureId + ": " + e.getMessage(), e);
	}

	/**
	 * Determine if a runtime exception represents a transient error that should be
	 * retried.
	 */
	private boolean isTransientError(Exception e) {
		String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
		String className = e.getClass().getSimpleName().toLowerCase();

		return hasTransientMessagePattern(message) || hasTransientClassNamePattern(className);
	}

	/**
	 * Checks if error message contains transient error patterns.
	 */
	private boolean hasTransientMessagePattern(String message) {
		return message.contains("timeout") || message.contains("connection") || message.contains("network")
				|| message.contains("temporary") || message.contains("unavailable");
	}

	/**
	 * Checks if class name contains transient error patterns.
	 */
	private boolean hasTransientClassNamePattern(String className) {
		return className.contains("timeout") || className.contains("connection");
	}

	/**
	 * Performs actual VDYP projection using the extended core library.
	 *
	 * @param batchRecord The VDYP record containing polygon and layer information
	 * @return Projection result string
	 */
	private String performVdypProjection(BatchRecord batchRecord) throws IOException {
		String featureId = batchRecord.getFeatureId();
		String mapId = batchRecord.getPolygon().getMapId();
		Long polygonNumber = batchRecord.getPolygon().getPolygonNumber();

		try {
			logger.debug(
					"[{}] Starting VDYP projection for Feature ID {} (Map: {}, Polygon: {})", partitionName, featureId,
					mapId, polygonNumber);

			// Call the actual VDYP projection service for this specific record and
			// partition
			String projectionResult = vdypProjectionService.performProjectionForRecord(batchRecord, partitionName);

			logger.debug(
					"[{}] Completed VDYP projection for Feature ID {} - Result: {}", partitionName,
					batchRecord.getFeatureId(), projectionResult);

			return projectionResult;

		} catch (Exception e) {
			// Handle projection failure with enhanced context and wrap as IOException
			throw handleProjectionFailure(featureId, mapId, polygonNumber, e);
		}
	}

	/**
	 * Handles VDYP projection failures by logging with enhanced context and
	 * creating appropriate IOException.
	 *
	 * @param featureId     The feature ID being processed
	 * @param mapId         The map ID being processed
	 * @param polygonNumber The polygon number being processed
	 * @param cause         The original exception that caused the failure
	 * @return IOException with enhanced context for retry logic
	 */
	private IOException handleProjectionFailure(String featureId, String mapId, Long polygonNumber, Exception cause) {
		// Create enhanced contextual message
		String contextualMessage = String.format(
				"[%s] VDYP projection failed for Feature ID %s (Map: %s, Polygon: %s). Exception type: %s, Root cause: %s",
				partitionName, featureId, mapId, polygonNumber, cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message available");

		// Log the failure with full context and stack trace
		logger.error(contextualMessage, cause);

		// Return IOException with enhanced context for retry logic
		return new IOException(contextualMessage, cause);
	}
}
