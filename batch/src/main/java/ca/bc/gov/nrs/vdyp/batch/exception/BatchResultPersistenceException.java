package ca.bc.gov.nrs.vdyp.batch.exception;

import java.io.Serial;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Exception thrown when batch result storage operations fail during batch processing.
 */
public class BatchResultPersistenceException extends BatchException {

	@Serial
	private static final long serialVersionUID = 2563311795099971052L;

	private BatchResultPersistenceException(String message, Exception cause) {
		super(message, cause, "", false, false);
	}

	public static BatchResultPersistenceException handleResultPersistenceFailure(
			Exception cause, String errorDescription, String jobGuid, Long jobExecutionId, Logger logger
	) {
		String rootCause = cause.getMessage() != null ? cause.getMessage()
				: BatchConstants.ErrorMessage.NO_ERROR_MESSAGE;
		String exceptionType = cause.getClass().getSimpleName();

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d%s] Exception type: %s, Root cause: %s", jobGuid, jobExecutionId, errorDescription,
				exceptionType, rootCause
		);

		logger.error(contextualMessage, cause);
		return new BatchResultPersistenceException(contextualMessage, cause);
	}

}
