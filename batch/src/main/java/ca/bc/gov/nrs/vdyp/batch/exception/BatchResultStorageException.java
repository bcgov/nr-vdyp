package ca.bc.gov.nrs.vdyp.batch.exception;

import java.io.IOException;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when batch result storage operations fail during batch processing.
 */
public class BatchResultStorageException extends BatchException {

	private static final long serialVersionUID = 2563311795099971052L;

	private BatchResultStorageException(
			String message, IOException cause, String jobGuid, Long jobExecutionId, String featureId
	) {
		super(message, cause, jobGuid, jobExecutionId, featureId, true, true);
	}

	public static BatchResultStorageException handleResultStorageFailure(
			Object context, IOException cause, String errorDescription, String jobGuid, Long jobExecutionId,
			String featureId, Logger logger
	) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String featureContext = featureId != null ? String.format(", Record: %s", featureId) : "";

		String contextualMessage = context != null
				? String.format(
						"[GUID: %s, EXEID: %d%s] %s: %s. Exception type: %s, Root cause: %s", jobGuid, jobExecutionId,
						featureContext, errorDescription, context, exceptionType, rootCause
				)
				: String.format(
						"[GUID: %s, EXEID: %d%s] %s. Exception type: %s, Root cause: %s", jobGuid, jobExecutionId,
						featureContext, errorDescription, exceptionType, rootCause
				);

		logger.error(contextualMessage, cause);
		return new BatchResultStorageException(contextualMessage, cause, jobGuid, jobExecutionId, featureId);
	}

	public static BatchResultStorageException handleResultStorageFailure(
			Object context, IOException cause, String errorDescription, String jobGuid, Long jobExecutionId,
			Logger logger
	) {
		return handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, null, logger);
	}
}
