package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.lang.NonNull;
import org.springframework.validation.BindException;

import ca.bc.gov.nrs.vdyp.batch.exception.ProjectionNullPointerException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class BatchSkipPolicy implements SkipPolicy {

	private static final Logger logger = LoggerFactory.getLogger(BatchSkipPolicy.class);

	private final long maxSkipCount;
	private Long jobExecutionId;
	private String jobGuid;
	private String partitionName;

	private final BatchMetricsCollector metricsCollector;

	public BatchSkipPolicy(long maxSkipCount, BatchMetricsCollector metricsCollector) {
		this.maxSkipCount = maxSkipCount;
		this.metricsCollector = metricsCollector;
	}

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
		this.partitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
	}

	@Override
	public boolean shouldSkip(@NonNull Throwable t, long skipCount) throws SkipLimitExceededException {
		validateSkipLimit(skipCount, t);

		boolean shouldSkip = isSkippableException(t);

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
			throw new SkipLimitExceededException((int) maxSkipCount, t);
		}
	}

	private void processSkippableException(Throwable t) {
		SkipContext skipContext = createSkipContext(t);
		ExecutionContext executionContext = getCurrentExecutionContext();

		recordSkipMetrics(skipContext, executionContext);
		logSkipDetails(t, executionContext.currentPartitionName);
	}

	private SkipContext createSkipContext(Throwable t) {
		String featureId = extractFeatureId(t);

		return new SkipContext(featureId, t);
	}

	private ExecutionContext getCurrentExecutionContext() {
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
				currentPartitionName = updatePartitionName(currentStepExecution);
			}
		} catch (Exception e) {
			logger.warn("[VDYP Skip Policy] Warning: Could not access step context: {}", e.getMessage());
		}

		return new ExecutionContext(currentJobExecutionId, currentJobGuid, currentPartitionName);
	}

	private String updatePartitionName(StepExecution stepExecution) {
		String retrievedPartitionName = stepExecution.getExecutionContext().getString(BatchConstants.Partition.NAME);
		partitionName = retrievedPartitionName;
		return retrievedPartitionName;
	}

	private void recordSkipMetrics(SkipContext skipContext, ExecutionContext executionContext) {
		if (metricsCollector != null && executionContext.currentJobExecutionId != null) {
			metricsCollector.recordSkip(
					executionContext.currentJobExecutionId, executionContext.currentJobGuid, skipContext.featureId,
					skipContext.throwable, executionContext.currentPartitionName
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
		final String featureId;
		final Throwable throwable;

		SkipContext(String featureId, Throwable throwable) {
			this.featureId = featureId;
			this.throwable = throwable;
		}
	}

	private static class ExecutionContext {
		final Long currentJobExecutionId;
		final String currentJobGuid;
		final String currentPartitionName;

		ExecutionContext(Long jobExecutionId, String jobGuid, String partitionName) {
			this.currentJobExecutionId = jobExecutionId;
			this.currentJobGuid = jobGuid;
			this.currentPartitionName = partitionName;
		}
	}

	/**
	 * Determine if an exception type should allow the record to be skipped.
	 */
	private boolean isSkippableException(Throwable t) {
		return isParseOrValidationException(t) || t instanceof NullPointerException || isSkippableIOException(t)
				|| isDataQualityRuntimeException(t);
	}

	/**
	 * Check if exception is a parsing or validation error (always skippable).
	 */
	private boolean isParseOrValidationException(Throwable t) {
		return t instanceof FlatFileParseException || t instanceof BindException || t instanceof NumberFormatException
				|| t instanceof IllegalArgumentException;
	}

	/**
	 * Check if IOException wraps NPE or indicates data quality issues (skippable).
	 */
	private boolean isSkippableIOException(Throwable t) {
		if (! (t instanceof java.io.IOException)) {
			return false;
		}

		if (t instanceof ProjectionNullPointerException) {
			return true;
		}

		String message = t.getMessage() != null ? t.getMessage().toLowerCase() : "";

		if (containsDataQualityKeywords(message)) {
			return true;
		}

		// Fallback: Check if IOException has NPE as root cause
		return t.getCause() instanceof NullPointerException;
	}

	/**
	 * Check if RuntimeException message indicates data quality issues (skippable).
	 */
	private boolean isDataQualityRuntimeException(Throwable t) {
		if (! (t instanceof RuntimeException)) {
			return false;
		}

		String message = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
		return containsDataQualityKeywords(message) || message.contains("format");
	}

	/**
	 * Check if message contains common data quality issue keywords.
	 */
	private boolean containsDataQualityKeywords(String message) {
		return message.contains("invalid") || message.contains("malformed") || message.contains("corrupt")
				|| message.contains("missing") || message.contains("empty");
	}

	private String extractFeatureId(Throwable t) {
		// FIXME VDYP-839
		if (t.getMessage() != null && t.getMessage().contains("ID ")) {
			try {
				String message = t.getMessage();
				int idIndex = message.indexOf("ID ") + 3;
				return message.substring(idIndex).split("\\D")[0];
			} catch (Exception e) {
				// Unable to extract ID
			}
		}
		return null;
	}
}
