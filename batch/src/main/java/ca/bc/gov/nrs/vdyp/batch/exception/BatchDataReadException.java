package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when reading partition data fails during batch processing.
 */
public class BatchDataReadException extends BatchException {

	private static final long serialVersionUID = 2894671293847561234L;

	private BatchDataReadException(
			String message, Throwable cause, String jobGuid, Long jobExecutionId, String featureId
	) {
		super(message, cause, jobGuid, jobExecutionId, featureId, false, false);
	}

	public static BatchDataReadException handleDataReadFailure(
			Exception cause, String errorDescription, String jobGuid, Long jobExecutionId, String featureId,
			String partitionName, Logger logger
	) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String featureContext = featureId != null ? String.format(", Record: %s", featureId) : "";

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d, Partition: %s%s] %s. Exception type: %s, Root cause: %s", jobGuid,
				jobExecutionId, partitionName, featureContext, errorDescription, exceptionType, rootCause
		);

		logger.error(contextualMessage, cause);
		return new BatchDataReadException(contextualMessage, cause, jobGuid, jobExecutionId, featureId);
	}

	public static BatchDataReadException handleDataReadFailure(
			Exception cause, String errorDescription, String jobGuid, Long jobExecutionId, String partitionName,
			Logger logger
	) {
		return handleDataReadFailure(cause, errorDescription, jobGuid, jobExecutionId, null, partitionName, logger);
	}
}
