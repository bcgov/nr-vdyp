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
		return handleProjectionFailure(cause, chunkMetadata, jobGuid, jobExecutionId, partitionName, null, logger);
	}

	/**
	 * @param firstFeatureId the FEATURE_ID of the first polygon in the chunk, so the skip can be traced back to the
	 *                       affected data
	 */
	public static BatchProjectionException handleProjectionFailure(
			Exception cause, BatchChunkMetadata chunkMetadata, String jobGuid, Long jobExecutionId,
			String partitionName, String firstFeatureId, Logger logger
	) {
		long polygonStartByte = chunkMetadata.getPolygonStartByte();
		int polygonRecordCount = chunkMetadata.getPolygonRecordCount();

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d, Partition: %s] VDYP projection failed for chunk of %d polygon(s) starting at feature ID %s (polygonStartByte=%d). Exception: %s, Message: %s",
				jobGuid, jobExecutionId, partitionName, polygonRecordCount,
				firstFeatureId != null ? firstFeatureId : "unknown", polygonStartByte, cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message"
		);

		logger.error(contextualMessage, cause);

		return new BatchProjectionException(contextualMessage, cause, firstFeatureId);
	}
}
