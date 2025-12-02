package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when CSV file partitioning operations fail during batch processing.
 */
public class BatchPartitionException extends BatchException {

	private static final long serialVersionUID = -5813242906871352881L;

	private BatchPartitionException(String message, Throwable cause, String jobGuid) {
		super(message, cause, jobGuid, null, null, false, false);
	}

	public static BatchPartitionException
			handlePartitionFailure(Exception cause, String errorDescription, String jobGuid, Logger logger) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String contextualMessage = String.format(
				"[GUID: %s] %s. Exception type: %s, Root cause: %s", jobGuid, errorDescription, exceptionType, rootCause
		);

		logger.error(contextualMessage, cause);
		return new BatchPartitionException(contextualMessage, cause, jobGuid);
	}

	public static BatchPartitionException
			handlePartitionFailure(String errorDescription, String jobGuid, Logger logger) {
		String contextualMessage = String.format("[GUID: %s] %s", jobGuid, errorDescription);

		logger.error(contextualMessage);
		return new BatchPartitionException(contextualMessage, null, jobGuid);
	}
}
