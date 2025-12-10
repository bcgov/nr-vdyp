package ca.bc.gov.nrs.vdyp.batch.exception;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;

/**
 * Exception thrown when VDYP projection operations fail.
 */
public class BatchProjectionException extends BatchException {

	private static final long serialVersionUID = 3104617883593359188L;

	private BatchProjectionException(String message, Throwable cause, String featureId) {
		super(message, cause, featureId, false, true);
	}

	public static BatchProjectionException handleProjectionFailure(
			Exception cause, BatchChunkMetadata chunkMetadata, String jobGuid, Long jobExecutionId,
			String partitionName, Logger logger
	) {
		int startIndex = chunkMetadata.getStartIndex();
		int recordCount = chunkMetadata.getRecordCount();

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d, Partition: %s] VDYP projection failed for chunk (startIndex=%d, recordCount=%d). Exception: %s, Message: %s",
				jobGuid, jobExecutionId, partitionName, startIndex, recordCount, cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message"
		);

		logger.error(contextualMessage, cause);

		return new BatchProjectionException(contextualMessage, cause, null);
	}
}
