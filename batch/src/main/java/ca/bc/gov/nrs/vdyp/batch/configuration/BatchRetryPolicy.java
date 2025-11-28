package ca.bc.gov.nrs.vdyp.batch.configuration;

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

/**
 * Retry doesn't track individual records because it is designed for transient errors (network failures, DB timeouts,
 * temporary I/O issues) and transient errors are not associated with specific data records.
 */
public class BatchRetryPolicy extends SimpleRetryPolicy {

	private static final long serialVersionUID = 430066847026367457L;

	private static final Logger logger = LoggerFactory.getLogger(BatchRetryPolicy.class);

	private final long backOffPeriod;
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;
	private transient BatchMetricsCollector metricsCollector;

	public BatchRetryPolicy(int maxAttempts, long backOffPeriod) {
		super(maxAttempts, createRetryableExceptions());
		this.backOffPeriod = backOffPeriod;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
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

		if (lastThrowable != null) {
			int attemptCount = context.getRetryCount();
			String currentPartitionName = getCurrentPartitionName();

			if (canRetry) {
				logRetryAttempt(lastThrowable, attemptCount, currentPartitionName);
				recordRetryMetric(lastThrowable, attemptCount, false, currentPartitionName);
			} else {
				logMaxAttemptsReached(lastThrowable, attemptCount, currentPartitionName);
			}
		}

		applyBackoffDelay(canRetry);
		return canRetry;
	}

	private String getCurrentPartitionName() {
		try {
			var stepContext = StepSynchronizationManager.getContext();
			if (stepContext != null) {
				StepExecution currentStepExecution = stepContext.getStepExecution();
				return currentStepExecution.getExecutionContext()
						.getString(BatchConstants.Partition.NAME, partitionName);
			}
		} catch (Exception e) {
			logger.debug("Could not access current step context, using stored partition name: {}", e.getMessage());
		}
		return partitionName;
	}

	private void logRetryAttempt(Throwable throwable, int attemptCount, String currentPartitionName) {
		logger.warn(
				"[{}] VDYP Retry attempt {} of {} due to transient error: {} - {}", currentPartitionName, attemptCount,
				getMaxAttempts(), throwable.getClass().getSimpleName(), throwable.getMessage()
		);
	}

	private void logMaxAttemptsReached(Throwable throwable, int attemptCount, String currentPartitionName) {
		logger.error(
				"[{}] Max retry attempts ({}) reached for transient error: {} - {}. Will be skipped.",
				currentPartitionName, attemptCount, throwable.getClass().getSimpleName(), throwable.getMessage()
		);
	}

	private void
			recordRetryMetric(Throwable throwable, int attemptCount, boolean successful, String currentPartitionName) {
		if (metricsCollector != null && jobExecutionId != null && jobGuid != null) {
			metricsCollector.recordRetryAttempt(
					jobExecutionId, jobGuid, attemptCount, throwable, successful, currentPartitionName
			);
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

	public void setMetricsCollector(BatchMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}
}
