package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class BatchSkipPolicy implements SkipPolicy {

	private static final Logger logger = LoggerFactory.getLogger(BatchSkipPolicy.class);

	private final long maxSkipCount;
	private final BatchMetricsCollector metricsCollector;

	// Step execution context - initialized once in beforeStep() and never modified thereafter.
	// Note: These fields cannot be made final due to Spring Batch's @BeforeStep contract.
	// Each worker step gets its own instance via @StepScope, so beforeStep() is called only once per instance.
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	public BatchSkipPolicy(long maxSkipCount, BatchMetricsCollector metricsCollector) {
		this.maxSkipCount = maxSkipCount;
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
	public boolean shouldSkip(@NonNull Throwable t, long skipCount) throws SkipLimitExceededException {
		validateSkipLimit(skipCount, t);

		boolean shouldSkip = false;

		if (t instanceof BatchException batchException) {
			shouldSkip = batchException.isSkippable();
		}

		logger.info(
				"[VDYP Skip Policy] Exception: {}, Skippable: {}, Current Skipped count: {}",
				t.getClass().getSimpleName(), shouldSkip, skipCount
		);

		if (shouldSkip) {
			processSkippableException(t);
		}

		return shouldSkip;
	}

	private void validateSkipLimit(long skipCount, Throwable t) throws SkipLimitExceededException {
		if (skipCount >= maxSkipCount) {
			// SkipLimitExceededException is required by Spring Batch's SkipPolicy contract.
			// This framework exception signals to Spring Batch that skip limit has been exceeded
			// and the job should fail.
			throw new SkipLimitExceededException((int) maxSkipCount, t);
		}
	}

	private void processSkippableException(Throwable t) {
		SkipContext skipContext = createSkipContext(t);
		SkipExecutionSnapshot executionSnapshot = getCurrentExecutionSnapshot();

		recordSkipMetricsSafely(skipContext, executionSnapshot);

		logger.info(
				"[{}] {}", executionSnapshot.currentPartitionName,
				"Skipping VDYP record due to error: " + t.getMessage()
		);
	}

	private SkipContext createSkipContext(Throwable t) {
		String featureId = null;

		if (t instanceof BatchException batchException) {
			featureId = batchException.getFeatureId();
		}
		return new SkipContext(featureId, t);
	}

	private SkipExecutionSnapshot getCurrentExecutionSnapshot() {
		Long currentJobExecutionId = jobExecutionId;
		String currentJobGuid = jobGuid;
		String currentPartitionName = partitionName;

		try {
			var stepContext = StepSynchronizationManager.getContext();
			if (stepContext != null) {
				StepExecution currentStepExecution = stepContext.getStepExecution();
				currentJobExecutionId = currentStepExecution.getJobExecutionId();
				currentJobGuid = currentStepExecution.getJobExecution().getJobParameters()
						.getString(BatchConstants.Job.GUID);
				currentPartitionName = currentStepExecution.getExecutionContext()
						.getString(BatchConstants.Partition.NAME, partitionName);
			}
		} catch (Exception e) {
			logger.warn("[VDYP Skip Policy] Warning: Could not access step context: {}", e.getMessage());
		}

		return new SkipExecutionSnapshot(currentJobExecutionId, currentJobGuid, currentPartitionName);
	}

	/**
	 * Records skip metrics, catching any BatchException to prevent metrics failures from affecting skip logic.
	 */
	private void recordSkipMetricsSafely(SkipContext skipContext, SkipExecutionSnapshot executionSnapshot) {
		if (metricsCollector != null && executionSnapshot.currentJobExecutionId != null) {
			try {
				metricsCollector.recordSkip(
						executionSnapshot.currentJobExecutionId, executionSnapshot.currentJobGuid,
						skipContext.featureId, skipContext.throwable, executionSnapshot.currentPartitionName
				);
			} catch (BatchException e) {
				logger.warn("Failed to record skip metric: {}", e.getMessage());
			}
		}
	}

	private static class SkipContext {
		final String featureId;
		final Throwable throwable;

		SkipContext(String featureId, Throwable throwable) {
			this.featureId = featureId;
			this.throwable = throwable;
		}
	}

	/**
	 * Immutable snapshot of execution context at the time of skip processing. Captures job and partition information
	 * atomically to ensure consistency when recording skip metrics.
	 */
	private static class SkipExecutionSnapshot {
		final Long currentJobExecutionId;
		final String currentJobGuid;
		final String currentPartitionName;

		SkipExecutionSnapshot(Long jobExecutionId, String jobGuid, String partitionName) {
			this.currentJobExecutionId = jobExecutionId;
			this.currentJobGuid = jobGuid;
			this.currentPartitionName = partitionName;
		}
	}
}
