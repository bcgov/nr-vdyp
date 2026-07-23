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
			String message, IOException cause, String featureId, boolean retryable, boolean skippable
	) {
		super(message, cause, featureId, retryable, skippable);
	}

	public static BatchResultStorageException handleResultStorageFailure(
			IOException cause, String errorDescription, String jobGuid, Long jobExecutionId, String featureId,
			Logger logger
	) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String featureContext = featureId != null ? String.format(", Record: %s", featureId) : "";

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d%s] %s. Exception type: %s, Root cause: %s", jobGuid, jobExecutionId,
				featureContext, errorDescription, exceptionType, rootCause
		);

		logger.error(contextualMessage, cause);
		boolean fatalStorageFailure = isOutOfSpace(cause);
		return new BatchResultStorageException(
				contextualMessage, cause, featureId, !fatalStorageFailure, !fatalStorageFailure
		);
	}

	public static BatchResultStorageException handleResultStorageFailure(
			IOException cause, String errorDescription, String jobGuid, Long jobExecutionId, Logger logger
	) {
		return handleResultStorageFailure(cause, errorDescription, jobGuid, jobExecutionId, null, logger);
	}

	private static boolean isOutOfSpace(IOException cause) {
		Throwable current = cause;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String lowerMessage = message.toLowerCase();
				if (lowerMessage.contains("no space left on device")
						|| lowerMessage.contains("not enough space on the disk")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
