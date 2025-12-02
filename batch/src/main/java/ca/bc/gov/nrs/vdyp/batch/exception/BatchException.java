package ca.bc.gov.nrs.vdyp.batch.exception;

/**
 * Base checked exception for all batch processing operations.
 *
 * Exception Handling Strategy: - All checked exceptions from external components are wrapped as BatchException
 * subclasses - RuntimeExceptions are wrapped as BatchException only if they represent recoverable errors - Retryability
 * and skippability are determined at exception creation time, not at handling time - FeatureId and job execution
 * context are captured for traceability
 */
public class BatchException extends Exception {

	private static final long serialVersionUID = -2197452160716581586L;

	private final String jobGuid;
	private final Long jobExecutionId;
	private final String featureId;
	private final boolean retryable;
	private final boolean skippable;

	public BatchException(
			String message, Throwable cause, String jobGuid, Long jobExecutionId, String featureId, boolean retryable,
			boolean skippable
	) {
		super(message, cause);
		this.jobGuid = jobGuid;
		this.jobExecutionId = jobExecutionId;
		this.featureId = featureId;
		this.retryable = retryable;
		this.skippable = skippable;
	}

	public BatchException(String message) {
		this(message, null, null, null, null, false, false);
	}

	public String getJobGuid() {
		return jobGuid;
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
	}

	public String getFeatureId() {
		return featureId;
	}

	public boolean isRetryable() {
		return retryable;
	}

	public boolean isSkippable() {
		return skippable;
	}
}
