package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

/**
 * Exception thrown when batch metrics operations fail.
 */
public class BatchMetricsException extends BatchException {

	private static final long serialVersionUID = -8234567890123456789L;

	private BatchMetricsException(String message, String jobGuid, Long jobExecutionId) {
		super(message, null, jobGuid, jobExecutionId, null, false, false);
	}

	public static BatchMetricsException
			handleMetricsFailure(String errorDescription, String jobGuid, Long jobExecutionId, Logger logger) {
		String contextualMessage = String
				.format("[GUID: %s, EXEID: %d] Metrics error: %s", jobGuid, jobExecutionId, errorDescription);

		logger.error(contextualMessage);
		return new BatchMetricsException(contextualMessage, jobGuid, jobExecutionId);
	}

	public static BatchMetricsException handleMetricsFailure(String errorDescription, String jobGuid, Logger logger) {
		String contextualMessage = String.format("[GUID: %s] Metrics error: %s", jobGuid, errorDescription);

		logger.error(contextualMessage);
		return new BatchMetricsException(contextualMessage, jobGuid, null);
	}

	public static BatchMetricsException handleMetricsFailure(String errorDescription, Logger logger) {
		String contextualMessage = String.format("Metrics error: %s", errorDescription);

		logger.error(contextualMessage);
		return new BatchMetricsException(contextualMessage, null, null);
	}
}
