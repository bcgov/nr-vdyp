package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when batch configuration or initialization operations fail.
 */
public class BatchConfigurationException extends BatchException {

	private static final long serialVersionUID = 1234567890123456789L;

	private BatchConfigurationException(String message, Throwable cause) {
		super(message, cause, null, false, false);
	}

	public static BatchConfigurationException handleConfigurationFailure(
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
		return new BatchConfigurationException(contextualMessage, cause);
	}
}
