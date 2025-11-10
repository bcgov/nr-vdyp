package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class BatchException extends RuntimeException {

	public BatchException(String message) {
		super(message);
	}

	public BatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public BatchException(Throwable cause) {
		super(cause);
	}

	public static BatchException
			handleException(Object context, Exception cause, String errorDescription, Logger logger) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String contextualMessage = context != null ? String.format(
				"%s: %s. Exception type: %s, Root cause: %s", errorDescription, context, exceptionType, rootCause
		) : String.format("%s. Exception type: %s, Root cause: %s", errorDescription, exceptionType, rootCause);

		logger.error(contextualMessage, cause);
		return new BatchException(contextualMessage, cause);
	}

	public static BatchException handleProjectionFailure(
			String jobGuid, Long jobExecutionId, String partitionName, int recordCount, Exception cause,
			String errorDescription, Logger logger
	) {
		String context = String.format(
				"jobGuid=%s, jobExecutionId=%d, partition=%s, records=%d", jobGuid, jobExecutionId, partitionName,
				recordCount
		);
		return handleException(context, cause, errorDescription, logger);
	}
}
