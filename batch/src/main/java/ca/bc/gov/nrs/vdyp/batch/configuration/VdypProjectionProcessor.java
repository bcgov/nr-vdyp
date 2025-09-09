package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VdypProjectionProcessor implements ItemProcessor<BatchRecord, BatchRecord> {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionProcessor.class);

	@Autowired
	private BatchRetryPolicy retryPolicy;

	@Autowired
	private BatchMetricsCollector metricsCollector;

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
				startLine, endLine
		);
	}

	/**
	 * Process a single record
	 *
	 * Processing flow: 1. Register record with retry policy for tracking 2. Validate record data quality (throws
	 * IllegalArgumentException for skippable issues) 3. Perform projection processing with proper error handling 4.
	 * Record retry success if this record was previously retried
	 *
	 * @param record The data record to process
	 * @return The processed record with projection results
	 * @throws Exception IOException for retryable errors, IllegalArgumentException for skippable errors
	 */
	@Override
	public BatchRecord process(@NonNull BatchRecord batchRecord) throws Exception {
		Long recordId = batchRecord.getId();

		// Register record with retry policy for tracking
		if (retryPolicy != null) {
			retryPolicy.registerRecord(recordId, batchRecord);
		}

		// Validate record data quality - throws IllegalArgumentException for skippable
		// issues
		validateRecordForProcessing(batchRecord);

		// Perform projection processing
		String projectionResult = performVdypProjectionWithErrorHandling(batchRecord);

		// Store the projection result in the record
		batchRecord.setProjectionResult(projectionResult);

		// Check if this record was previously retried and notify success
		String retryKey = partitionName + "_" + recordId;
		if (retryPolicy != null && retriedRecords.contains(retryKey)) {
			retryPolicy.onRetrySuccess(recordId, batchRecord);
			retriedRecords.remove(retryKey);
			logger.info(
					"[{}] VDYP Retry success recorded for job {} record ID {}", partitionName, jobExecutionId, recordId
			);
		}

		return batchRecord;
	}

	/**
	 * Validate record data quality for production processing. Throws IllegalArgumentException for data quality issues
	 * that should be skipped.
	 */
	private void validateRecordForProcessing(BatchRecord batchRecord) throws IllegalArgumentException {
		Long recordId = batchRecord.getId();

		// Validate required fields
		if (batchRecord.getData() == null || batchRecord.getData().trim().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Missing required VDYP data field for record ID %d", recordId)
			);
		}

		if (batchRecord.getPolygonId() == null || batchRecord.getPolygonId().trim().isEmpty()) {
			throw new IllegalArgumentException(String.format("Missing required polygon ID for record ID %d", recordId));
		}

		if (batchRecord.getLayerId() == null || batchRecord.getLayerId().trim().isEmpty()) {
			throw new IllegalArgumentException(String.format("Missing required layer ID for record ID %d", recordId));
		}

		// Validate data lengths and formats
		if (batchRecord.getData().length() > maxDataLength) {
			throw new IllegalArgumentException(
					String.format(
							"VDYP data field too long for record ID %d (length: %d, max: %d)", recordId,
							batchRecord.getData().length(), maxDataLength
					)
			);
		}

		String polygonId = batchRecord.getPolygonId();
		if (polygonId.length() < minPolygonIdLength || polygonId.length() > maxPolygonIdLength) {
			throw new IllegalArgumentException(
					String.format(
							"Invalid polygon ID length for record ID %d (length: %d)", recordId, polygonId.length()
					)
			);
		}
	}

	/**
	 * Perform VDYP projection processing with proper error handling.
	 *
	 * This method handles both retryable errors (IOException) and non-retryable validation errors.
	 */
	private String performVdypProjectionWithErrorHandling(BatchRecord batchRecord)
			throws IOException, IllegalArgumentException {
		try {
			String result = performVdypProjection(batchRecord);
			return validateProjectionResult(result, batchRecord.getId());
		} catch (Exception e) {
			handleProjectionException(e, batchRecord);
			reclassifyAndThrowException(e, batchRecord.getId());
			return null;
		}
	}

	/**
	 * Validates the projection result and throws IOException if empty.
	 */
	private String validateProjectionResult(String result, Long recordId) throws IOException {
		if (result == null || result.trim().isEmpty()) {
			throw new IOException(String.format("VDYP projection returned empty result for record ID %d", recordId));
		}
		return result;
	}

	/**
	 * Handles exception by recording appropriate metrics.
	 */
	private void handleProjectionException(Exception e, BatchRecord batchRecord) {
		if (metricsCollector != null && jobExecutionId != null) {
			if (isRetryableException(e)) {
				metricsCollector.recordRetryAttempt(
						jobExecutionId, batchRecord.getId(), batchRecord, 1, e, false, partitionName
				);
			} else {
				metricsCollector.recordSkip(jobExecutionId, batchRecord.getId(), batchRecord, e, partitionName, null);
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
	private void reclassifyAndThrowException(Exception e, Long recordId) throws IOException, IllegalArgumentException {
		if (e instanceof IOException ioException) {
			throw ioException;
		}

		if (e instanceof IllegalArgumentException illegalArgException) {
			throw illegalArgException;
		}

		if (e instanceof RuntimeException && isTransientError(e)) {
			throw new IOException("Transient error during VDYP projection for record ID " + recordId, e);
		}

		// Unknown errors treated as data quality issues
		throw new IllegalArgumentException(
				"VDYP projection failed for record ID " + recordId + ": " + e.getMessage(), e
		);
	}

	/**
	 * Determine if a runtime exception represents a transient error that should be retried.
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
	 * This is a placeholder implementation that will be replaced with actual VDYP extended core service calls.
	 *
	 * @param batchRecord The VDYP record containing polygon and layer information
	 * @return Projection result string
	 */
	private String performVdypProjection(BatchRecord batchRecord) throws IOException {
		String polygonId = batchRecord.getPolygonId();
		String layerId = batchRecord.getLayerId();
		String data = batchRecord.getData();

		try {
			Thread.sleep(10); // Minimal delay to simulate processing
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Processing interrupted for record ID " + batchRecord.getId(), e);
		}

		return String.format(
				"PROJECTED[P:%s,L:%s,Data:%s]", polygonId != null ? polygonId : "N/A",
				layerId != null ? layerId : "N/A",
				data != null && data.length() > 10 ? data.substring(0, 10) + "..." : data
		);
	}
}