package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchRetryPolicy extends SimpleRetryPolicy {

	private static final Logger logger = LoggerFactory.getLogger(BatchRetryPolicy.class);

	private final long backOffPeriod;
	private Long jobExecutionId;
	private String partitionName;
	private transient BatchMetricsCollector metricsCollector;

	// Thread-safe retry state tracking
	private final ConcurrentHashMap<String, RetryInfo> retryInfoMap = new ConcurrentHashMap<>();

	// To track retry state for individual records
	private static class RetryInfo {
		BatchRecord batchRecord;
		Long recordId;
		int attemptCount = 0;
		Throwable lastError;
		boolean successful = false;
	}

	public BatchRetryPolicy(int maxAttempts, long backOffPeriod) {
		super(maxAttempts, createRetryableExceptions());
		this.backOffPeriod = backOffPeriod;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.partitionName = stepExecution.getExecutionContext()
				.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN);
	}

	private static Map<Class<? extends Throwable>, Boolean> createRetryableExceptions() {
		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
		retryableExceptions.put(IOException.class, true);
		retryableExceptions.put(TransientDataAccessException.class, true);
		return retryableExceptions;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		boolean canRetry = super.canRetry(context);
		Throwable lastThrowable = context.getLastThrowable();

		if (lastThrowable != null && lastThrowable.getMessage() != null) {
			processThrowable(lastThrowable, canRetry, context);
		}

		applyBackoffDelay(canRetry);
		return canRetry;
	}

	private void processThrowable(Throwable lastThrowable, boolean canRetry, RetryContext context) {
		String errorMessage = lastThrowable.getMessage();
		Long recordId = extractRecordId(errorMessage);
		String retryKey = createRetryKey(recordId);

		if (canRetry) {
			processRetryableException(lastThrowable, recordId, retryKey, context);
		} else {
			processNonRetryableException(lastThrowable);
		}
	}

	private void
			processRetryableException(Throwable lastThrowable, Long recordId, String retryKey, RetryContext context) {
		RetryInfo retryInfo = updateRetryInfo(retryKey, recordId, lastThrowable);
		ExecutionContext executionContext = getCurrentExecutionContext();

		recordRetryAttempt(retryInfo, lastThrowable, executionContext);
		logRetryAttempt(retryInfo, recordId, lastThrowable, executionContext.currentPartitionName);

		if (isMaxAttemptsReached(context)) {
			handleMaxAttemptsReached(retryInfo, recordId, retryKey, executionContext.currentPartitionName);
		}
	}

	private void processNonRetryableException(Throwable lastThrowable) {
		logger.info(
				"[VDYP Retry Policy] Non-retryable exception: {} - {} (will be skipped)",
				lastThrowable.getClass().getSimpleName(), lastThrowable.getMessage()
		);
	}

	private RetryInfo updateRetryInfo(String retryKey, Long recordId, Throwable lastThrowable) {
		RetryInfo retryInfo = retryInfoMap.computeIfAbsent(retryKey, k -> new RetryInfo());
		retryInfo.recordId = recordId;
		retryInfo.lastError = lastThrowable;
		retryInfo.attemptCount = retryInfo.attemptCount + 1;

		logger.debug(
				"[VDYP Retry Policy] canRetry called: true, Exception: {}, Retry count: {}",
				lastThrowable.getClass().getSimpleName(), retryInfo.attemptCount
		);

		return retryInfo;
	}

	private ExecutionContext getCurrentExecutionContext() {
		Long currentJobExecutionId = jobExecutionId;
		String currentPartitionName = partitionName;

		try {
			var stepContext = StepSynchronizationManager.getContext();
			if (stepContext != null) {
				StepExecution currentStepExecution = stepContext.getStepExecution();
				currentJobExecutionId = currentStepExecution.getJobExecutionId();
				currentPartitionName = currentStepExecution.getExecutionContext()
						.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN);
			}
		} catch (Exception e) {
			logger.warn("[VDYP Retry Policy] Warning: Could not access step context in canRetry: {}", e.getMessage());
		}

		return new ExecutionContext(currentJobExecutionId, currentPartitionName);
	}

	private void recordRetryAttempt(RetryInfo retryInfo, Throwable lastThrowable, ExecutionContext executionContext) {
		if (metricsCollector != null && executionContext.currentJobExecutionId != null) {
			metricsCollector.recordRetryAttempt(
					executionContext.currentJobExecutionId, retryInfo.recordId, retryInfo.batchRecord,
					retryInfo.attemptCount, lastThrowable, false, executionContext.currentPartitionName
			);
		}
	}

	private void logRetryAttempt(RetryInfo retryInfo, Long recordId, Throwable lastThrowable, String partitionName) {
		logger.info(
				"[{}] VDYP Retry attempt {} of {} for record ID {} (stored: {}). Error: {} - {}", partitionName,
				retryInfo.attemptCount, getMaxAttempts(), recordId,
				retryInfo.recordId != null ? retryInfo.recordId : -1, lastThrowable.getClass().getSimpleName(),
				retryInfo.lastError != null ? retryInfo.lastError.getMessage() : "No stored error"
		);
	}

	private boolean isMaxAttemptsReached(RetryContext context) {
		return context.getRetryCount() >= getMaxAttempts();
	}

	private void handleMaxAttemptsReached(RetryInfo retryInfo, Long recordId, String retryKey, String partitionName) {
		retryInfo.successful = false;
		logger.warn(
				"[{}] Max retry attempts reached for record ID {}. Giving up. Final status: {}", partitionName,
				recordId, retryInfo.successful ? "Success" : "Failed"
		);
		retryInfoMap.remove(retryKey);
	}

	private void applyBackoffDelay(boolean canRetry) {
		if (canRetry && backOffPeriod > 0) {
			try {
				Thread.sleep(backOffPeriod);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
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

	public void registerRecord(Long recordId, BatchRecord batchRecord) {
		String retryKey = createRetryKey(recordId);
		RetryInfo retryInfo = retryInfoMap.computeIfAbsent(retryKey, k -> new RetryInfo());
		retryInfo.batchRecord = batchRecord;
		retryInfo.recordId = recordId;
	}

	public void onRetrySuccess(Long recordId, BatchRecord batchRecord) {
		String retryKey = createRetryKey(recordId);
		RetryInfo retryInfo = retryInfoMap.get(retryKey);

		if (retryInfo != null && retryInfo.attemptCount > 0) {
			updateSuccessfulRetryInfo(retryInfo);
			ExecutionContext executionContext = getCurrentExecutionContext();
			recordSuccessfulRetry(retryInfo, recordId, batchRecord, executionContext);
			logSuccessfulRetry(recordId, retryInfo.attemptCount, executionContext.currentPartitionName);
			retryInfoMap.remove(retryKey);
		}
	}

	private void updateSuccessfulRetryInfo(RetryInfo retryInfo) {
		retryInfo.successful = true;
		retryInfo.attemptCount = retryInfo.attemptCount + 1;
	}

	private void recordSuccessfulRetry(
			RetryInfo retryInfo, Long recordId, BatchRecord batchRecord, ExecutionContext executionContext
	) {
		if (metricsCollector != null && executionContext.currentJobExecutionId != null) {
			metricsCollector.recordRetryAttempt(
					executionContext.currentJobExecutionId, recordId, batchRecord, retryInfo.attemptCount, null, true,
					executionContext.currentPartitionName
			);
		}
	}

	private void logSuccessfulRetry(Long recordId, int attemptCount, String partitionName) {
		logger.info(
				"[{}] VDYP Record ID {} successfully processed after {} retry attempt(s)", partitionName, recordId,
				attemptCount
		);
	}

	private String createRetryKey(Long recordId) {
		return recordId + "_" + Thread.currentThread().getName();
	}

	/**
	 * Extract record ID from error message for retry tracking.
	 *
	 * Parses error messages to find record IDs when they follow the pattern "... record ID <number> ...". Used to link
	 * retry attempts to specific records.
	 */
	private Long extractRecordId(String errorMessage) {
		if (errorMessage == null) {
			return -1L;
		}

		try {
			if (errorMessage.contains("record ID ")) {
				String[] parts = errorMessage.split("record ID ");
				if (parts.length > 1) {
					String idPart = parts[1].trim();
					String[] tokens = idPart.split("\\s+");
					if (tokens.length > 0) {
						return Long.parseLong(tokens[0]);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failed to parse record ID from error message: {}", errorMessage);
		}

		return -1L;
	}

	public void setMetricsCollector(BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}
}
