package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Retry policy for batch processing operations.
 *
 * Retry Strategy: Retry doesn't track individual records because it is designed for transient errors. Use
 * BatchException to determine retryability at exception creation time. The BatchRetryPolicy determines whether to retry
 * solely based on BatchException.isRetryable(). All exceptions must be wrapped in a BatchException; if they are not,
 * the operation will not be retried.
 */
public class BatchRetryPolicy extends SimpleRetryPolicy {

	private static final long serialVersionUID = 430066847026367457L;

	private static final Logger logger = LoggerFactory.getLogger(BatchRetryPolicy.class);

	private final long backOffPeriod;
	private final transient BatchMetricsCollector metricsCollector;

	// Step execution context - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's @BeforeStep contract.
	// Each worker step gets its own instance via @StepScope, so beforeStep() is called only once per instance.
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	public BatchRetryPolicy(int maxAttempts, long backOffPeriod, BatchMetricsCollector metricsCollector) {
		super(maxAttempts);
		this.backOffPeriod = backOffPeriod;
		this.metricsCollector = metricsCollector;
	}

	/**
	 * Automatically invoked by Spring Batch before step execution to initialize context. Called once per step instance
	 * (@StepScope ensures separate instances for each worker).
	 */
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
	}

	@Override
	public boolean canRetry(RetryContext context) {
		Throwable lastThrowable = context.getLastThrowable();

		if (lastThrowable == null) {
			return false;
		}

		if (lastThrowable instanceof BatchException batchException) {
			return handleBatchExceptionRetry(context, batchException);
		}

		// Non-BatchException: not retryable
		logger.warn(
				"[{}] Non-BatchException encountered: {} - {}. Not retryable.", getCurrentPartitionName(),
				lastThrowable.getClass().getSimpleName(), lastThrowable.getMessage()
		);
		return false;
	}

	/**
	 * Handles retry logic for BatchException using isRetryable() flag.
	 */
	private boolean handleBatchExceptionRetry(RetryContext context, BatchException batchException) {
		boolean isRetryable = batchException.isRetryable();
		boolean canRetryByCount = context.getRetryCount() < getMaxAttempts();
		boolean canRetry = isRetryable && canRetryByCount;

		int attemptCount = context.getRetryCount();
		String currentPartitionName = getCurrentPartitionName();

		if (canRetry) {
			logger.warn(
					"[{}] VDYP Retry attempt {} of {} due to transient error: {} - {}", currentPartitionName,
					attemptCount, getMaxAttempts(), batchException.getClass().getSimpleName(),
					batchException.getMessage()
			);

			recordRetryMetricSafely(batchException, attemptCount, false, currentPartitionName);
		} else if (isRetryable) {
			// Retryable but max attempts reached
			logger.error(
					"[{}] Max retry attempts ({}) reached for transient error: {} - {}. Will be skipped.",
					currentPartitionName, attemptCount, batchException.getClass().getSimpleName(),
					batchException.getMessage()
			);
		} else {
			// Not retryable at all
			logger.info(
					"[{}] Exception is not retryable: {} - {}", currentPartitionName,
					batchException.getClass().getSimpleName(), batchException.getMessage()
			);
		}

		applyBackoffDelay(canRetry);
		return canRetry;
	}

	private String getCurrentPartitionName() {
		var stepContext = StepSynchronizationManager.getContext();
		if (stepContext != null) {
			StepExecution currentStepExecution = stepContext.getStepExecution();
			return currentStepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME, partitionName);
		}
		return partitionName;
	}

	/**
	 * Records retry metric, catching any BatchException to prevent metrics failures from affecting retry logic.
	 */
	private void recordRetryMetricSafely(
			Throwable throwable, int attemptCount, boolean successful, String currentPartitionName
	) {
		if (metricsCollector != null && jobExecutionId != null && jobGuid != null) {
			try {
				metricsCollector.recordRetryAttempt(
						jobExecutionId, jobGuid, attemptCount, throwable, successful, currentPartitionName
				);
			} catch (BatchException e) {
				logger.warn("Failed to record retry metric: {}", e.getMessage());
			}
		}
	}

	private void applyBackoffDelay(boolean canRetry) {
		if (canRetry && backOffPeriod > 0) {
			try {
				Thread.sleep(backOffPeriod);
				logger.debug("Applied backoff delay of {} ms before retry", backOffPeriod);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Backoff delay interrupted", e);
			}
		}
	}
}
