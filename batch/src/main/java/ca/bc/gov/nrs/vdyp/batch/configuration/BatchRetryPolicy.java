package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
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
	private BatchMetricsCollector metricsCollector;

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
		this.partitionName = stepExecution.getExecutionContext().getString("partitionName", "unknown");
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

		// Only process retry metrics if this exception is actually retryable
		if (lastThrowable != null && lastThrowable.getMessage() != null && canRetry) {
			String errorMessage = lastThrowable.getMessage();
			Long recordId = extractRecordId(errorMessage);
			String retryKey = createRetryKey(recordId);

			RetryInfo retryInfo = retryInfoMap.computeIfAbsent(retryKey, k -> new RetryInfo());
			retryInfo.recordId = recordId;
			retryInfo.lastError = lastThrowable;
			retryInfo.attemptCount = retryInfo.attemptCount + 1;

			logger.debug(
					"[VDYP Retry Policy] canRetry called: {}, Exception: {}, Retry count: {}", canRetry,
					lastThrowable.getClass().getSimpleName(), retryInfo.attemptCount
			);

			// Get current step execution context for metrics
			Long currentJobExecutionId = jobExecutionId;
			String currentPartitionName = partitionName;

			try {
				var stepContext = StepSynchronizationManager.getContext();
				if (stepContext != null) {
					StepExecution currentStepExecution = stepContext.getStepExecution();
					if (currentStepExecution != null) {
						Long retrievedJobId = currentStepExecution.getJobExecutionId();
						if (retrievedJobId != null) {
							currentJobExecutionId = retrievedJobId;
						}
						String retrievedPartitionName = currentStepExecution.getExecutionContext()
								.getString("partitionName", "unknown");
						if (retrievedPartitionName != null) {
							currentPartitionName = retrievedPartitionName;
						}
					}
				}
			} catch (Exception e) {
				logger.warn(
						"[VDYP Retry Policy] Warning: Could not access step context in canRetry: {}", e.getMessage()
				);
			}

			// Record retry attempt in metrics
			if (metricsCollector != null && currentJobExecutionId != null) {
				metricsCollector.recordRetryAttempt(
						currentJobExecutionId, recordId, retryInfo.batchRecord, retryInfo.attemptCount, lastThrowable,
						false, currentPartitionName
				);
			}

			// Log retry attempt with detailed info including stored retry state
			logger.info(
					"[{}] VDYP Retry attempt {} of {} for record ID {} (stored: {}). Error: {} - {}",
					currentPartitionName, retryInfo.attemptCount, getMaxAttempts(), recordId,
					retryInfo.recordId != null ? retryInfo.recordId : -1, lastThrowable.getClass().getSimpleName(),
					retryInfo.lastError != null ? retryInfo.lastError.getMessage() : "No stored error"
			);

			if (!canRetry) {
				// Mark as final failure
				retryInfo.successful = false;
				logger.warn(
						"[{}] Max retry attempts reached for record ID {}. Giving up. Final status: {}",
						currentPartitionName, recordId, retryInfo.successful ? "Success" : "Failed"
				);
				// Clean up
				retryInfoMap.remove(retryKey);
			}
		} else if (lastThrowable != null && !canRetry) {
			logger.info(
					"[VDYP Retry Policy] Non-retryable exception: {} - {} (will be skipped)",
					lastThrowable.getClass().getSimpleName(), lastThrowable.getMessage()
			);
		}

		// Apply backoff delay if retry is allowed
		if (canRetry && backOffPeriod > 0) {
			try {
				Thread.sleep(backOffPeriod);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		return canRetry;
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
			retryInfo.successful = true;
			// Increment attempt count for the final successful attempt
			retryInfo.attemptCount = retryInfo.attemptCount + 1;

			// Get current step execution context for metrics
			Long currentJobExecutionId = jobExecutionId;
			String currentPartitionName = partitionName;

			try {
				var retryContext = StepSynchronizationManager.getContext();
				if (retryContext != null) {
					StepExecution currentStepExecution = retryContext.getStepExecution();
					if (currentStepExecution != null) {
						Long retrievedJobId = currentStepExecution.getJobExecutionId();
						if (retrievedJobId != null) {
							currentJobExecutionId = retrievedJobId;
						}
						String retrievedPartitionName = currentStepExecution.getExecutionContext()
								.getString("partitionName", "unknown");
						if (retrievedPartitionName != null) {
							currentPartitionName = retrievedPartitionName;
						}
					}
				}
			} catch (Exception e) {
				logger.warn(
						"[VDYP Retry Policy] Warning: Could not access step context in onRetrySuccess: {}",
						e.getMessage()
				);
			}

			// Record successful retry in metrics
			if (metricsCollector != null && currentJobExecutionId != null) {
				metricsCollector.recordRetryAttempt(
						currentJobExecutionId, recordId, batchRecord, retryInfo.attemptCount, null, true,
						currentPartitionName
				);
			}

			logger.info(
					"[{}] VDYP Record ID {} successfully processed after {} retry attempt(s)", currentPartitionName,
					recordId, retryInfo.attemptCount
			);

			// Clean up
			retryInfoMap.remove(retryKey);
		}
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

	public static void cleanupRetryTracking() {
	}
}