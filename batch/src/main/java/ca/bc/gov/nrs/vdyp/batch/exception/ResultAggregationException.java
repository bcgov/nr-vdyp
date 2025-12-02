package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when result aggregation fails.
 */
public class ResultAggregationException extends BatchException {

	private static final long serialVersionUID = -4895498760794332393L;

	private ResultAggregationException(String message, Throwable cause, String jobGuid, Long jobExecutionId) {
		super(message, cause, jobGuid, jobExecutionId, null, true, false);
	}

	public static ResultAggregationException handleResultAggregationFailure(
			Exception cause, String errorDescription, String jobGuid, Long jobExecutionId, Logger logger
	) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d] %s. Exception type: %s, Root cause: %s", jobGuid, jobExecutionId,
				errorDescription, exceptionType, rootCause
		);

		logger.error(contextualMessage, cause);
		return new ResultAggregationException(contextualMessage, cause, jobGuid, jobExecutionId);
	}
}
