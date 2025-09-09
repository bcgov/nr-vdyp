package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.lang.NonNull;
import org.springframework.validation.BindException;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import java.util.concurrent.ConcurrentHashMap;

public class BatchSkipPolicy implements SkipPolicy {

	private static final Logger logger = LoggerFactory.getLogger(BatchSkipPolicy.class);

	private final long maxSkipCount;
	private Long jobExecutionId;
	private String partitionName;

	private static final String UNKNOWN = "unknown";

	private final BatchMetricsCollector metricsCollector;

	// Thread-safe storage for record data before skip processing
	private static final ConcurrentHashMap<String, BatchRecord> recordDataCache = new ConcurrentHashMap<>();

	public BatchSkipPolicy(long maxSkipCount, BatchMetricsCollector metricsCollector) {
		this.maxSkipCount = maxSkipCount;
		this.metricsCollector = metricsCollector;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.partitionName = stepExecution.getExecutionContext().getString("partitionName", UNKNOWN);
	}

	@Override
	public boolean shouldSkip(@NonNull Throwable t, long skipCount) throws SkipLimitExceededException {
		validateSkipLimit(skipCount, t);

		boolean shouldSkip = isSkippableException(t);
		logSkipDecision(t, shouldSkip, skipCount);

		if (shouldSkip) {
			processSkippableException(t);
		}

		return shouldSkip;
	}

	private void validateSkipLimit(long skipCount, Throwable t) throws SkipLimitExceededException {
		if (skipCount >= maxSkipCount) {
			throw new SkipLimitExceededException((int) maxSkipCount, t);
		}
	}

	private void logSkipDecision(Throwable t, boolean shouldSkip, long skipCount) {
		logger.info(
				"[VDYP Skip Policy] Exception: {}, Skippable: {}, Current Skipped count: {}",
				t.getClass().getSimpleName(), shouldSkip, skipCount
		);
	}

	private void processSkippableException(Throwable t) {
		SkipContext skipContext = createSkipContext(t);
		ExecutionContext executionContext = getCurrentExecutionContext();

		recordSkipMetrics(skipContext, executionContext);
		logSkipDetails(t, executionContext.currentPartitionName);
	}

	private SkipContext createSkipContext(Throwable t) {
		Long recordId = extractRecordId(t);
		BatchRecord batchRecord = extractRecord(t);
		Long lineNumber = extractLineNumber(t);

		return new SkipContext(recordId, batchRecord, lineNumber, t);
	}

	private ExecutionContext getCurrentExecutionContext() {
		Long currentJobExecutionId = jobExecutionId;
		String currentPartitionName = partitionName;

		try {
			var stepContext = StepSynchronizationManager.getContext();
			if (stepContext != null) {
				StepExecution currentStepExecution = stepContext.getStepExecution();
				currentJobExecutionId = updateJobExecutionId(currentStepExecution);
				currentPartitionName = updatePartitionName(currentStepExecution);
			}
		} catch (Exception e) {
			logger.warn("[VDYP Skip Policy] Warning: Could not access step context: {}", e.getMessage());
		}

		return new ExecutionContext(currentJobExecutionId, currentPartitionName);
	}

	private Long updateJobExecutionId(StepExecution stepExecution) {
		return stepExecution.getJobExecutionId();
	}

	private String updatePartitionName(StepExecution stepExecution) {
		String retrievedPartitionName = stepExecution.getExecutionContext().getString("partitionName", UNKNOWN);
		partitionName = retrievedPartitionName;
		return retrievedPartitionName;
	}

	private void recordSkipMetrics(SkipContext skipContext, ExecutionContext executionContext) {
		if (metricsCollector != null && executionContext.currentJobExecutionId != null) {
			metricsCollector.recordSkip(
					executionContext.currentJobExecutionId, skipContext.recordId, skipContext.batchRecord,
					skipContext.throwable, executionContext.currentPartitionName, skipContext.lineNumber
			);
		}
	}

	private void logSkipDetails(Throwable t, String currentPartitionName) {
		String errorMessage = buildErrorMessage(t);
		logger.info("[{}] {}", currentPartitionName, errorMessage);
	}

	private String buildErrorMessage(Throwable t) {
		String errorMessage = "Skipping VDYP record due to error: " + t.getMessage();
		if (t instanceof FlatFileParseException ffpe) {
			errorMessage += String.format(" [Line: %d, Input: %s]", ffpe.getLineNumber(), ffpe.getInput());
		}
		return errorMessage;
	}

	private static class SkipContext {
		final Long recordId;
		final BatchRecord batchRecord;
		final Long lineNumber;
		final Throwable throwable;

		SkipContext(Long recordId, BatchRecord batchRecord, Long lineNumber, Throwable throwable) {
			this.recordId = recordId;
			this.batchRecord = batchRecord;
			this.lineNumber = lineNumber;
			this.throwable = throwable;
		}
	}

	private static class ExecutionContext {
		final Long currentJobExecutionId;
		final String currentPartitionName;

		ExecutionContext(Long jobExecutionId, String partitionName) {
			this.currentJobExecutionId = jobExecutionId;
			this.currentPartitionName = partitionName;
		}
	}

	/**
	 * Determine if an exception type should allow the record to be skipped.
	 */
	private boolean isSkippableException(Throwable t) {
		// Data parsing and format errors - always skippable
		if (t instanceof FlatFileParseException || t instanceof BindException || t instanceof NumberFormatException
				|| t instanceof IllegalArgumentException) {
			return true;
		}

		// Null pointer exceptions for missing data - skippable
		if (t instanceof NullPointerException) {
			return true;
		}

		// Runtime exceptions that indicate data quality issues - skippable
		if (t instanceof RuntimeException) {
			String message = t.getMessage() != null ? t.getMessage().toLowerCase() : "";

			// Skip if error message indicates data quality issues
			return message.contains("invalid") || message.contains("malformed") || message.contains("corrupt")
					|| message.contains("missing") || message.contains("empty") || message.contains("format");
		}

		// All other exceptions should be retried, not skipped
		return false;
	}

	/**
	 * Extract record ID from exception message for tracking.
	 */
	private Long extractRecordId(Throwable t) {
		if (t.getMessage() != null && t.getMessage().contains("ID ")) {
			try {
				String message = t.getMessage();
				int idIndex = message.indexOf("ID ") + 3;
				String idStr = message.substring(idIndex).split("\\D")[0];
				return Long.parseLong(idStr);
			} catch (Exception e) {
				// Unable to extract ID
			}
		}
		return null;
	}

	/**
	 * Extract record data from exception context.
	 */
	private BatchRecord extractRecord(Throwable t) {
		Long recordId = extractRecordId(t);
		if (recordId != null) {
			// Try to get cached record data first
			String threadName = Thread.currentThread().getName();
			BatchRecord cachedRecord = getCachedRecordData(recordId, threadName);
			if (cachedRecord != null) {
				return cachedRecord;
			}

			// Fallback: create a basic record with the extracted ID for tracking
			BatchRecord batchRecord = new BatchRecord();
			batchRecord.setId(recordId);
			return batchRecord;
		}
		return null;
	}

	/**
	 * Extract line number from file parsing exceptions.
	 */
	private Long extractLineNumber(Throwable t) {
		if (t instanceof FlatFileParseException) {
			return (long) ((FlatFileParseException) t).getLineNumber();
		}

		// For other exceptions, estimate line number based on record ID
		Long recordId = extractRecordId(t);
		if (recordId != null && recordId > 0) {
			return recordId + 1;
		}

		return null;
	}

	/**
	 * Store record data before skip processing for later retrieval.
	 */
	public static void cacheRecordData(Long recordId, BatchRecord batchRecord, String threadName) {
		if (recordId != null && batchRecord != null) {
			String key = recordId + "_" + threadName;
			recordDataCache.put(key, batchRecord);
		}
	}

	/**
	 * Retrieve cached record data for skip processing.
	 */
	private static BatchRecord getCachedRecordData(Long recordId, String threadName) {
		if (recordId != null && threadName != null) {
			String key = recordId + "_" + threadName;
			return recordDataCache.remove(key); // Remove after retrieval to prevent memory leaks
		}
		return null;
	}

}