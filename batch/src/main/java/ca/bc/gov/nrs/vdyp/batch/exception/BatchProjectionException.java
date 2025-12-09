package ca.bc.gov.nrs.vdyp.batch.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

/**
 * Exception thrown when VDYP projection operations fail.
 */
public class BatchProjectionException extends BatchException {

	private static final long serialVersionUID = 3104617883593359188L;

	private BatchProjectionException(String message, Throwable cause, String featureId) {
		super(message, cause, featureId, false, true);
	}

	public static BatchProjectionException handleProjectionFailure(
			Exception cause, List<BatchRecord> batchRecords, String jobGuid, Long jobExecutionId, String partitionName,
			Logger logger
	) {
		List<String> featureIds = batchRecords.stream().map(BatchRecord::getFeatureId).toList();

		String featureIdsPreview = featureIds.stream().limit(5).collect(Collectors.joining(", "));
		if (featureIds.size() > 5) {
			featureIdsPreview += String.format(" and %d more", featureIds.size() - 5);
		}

		String contextualMessage = String.format(
				"[GUID: %s, EXEID: %d, Partition: %s] VDYP projection failed for %d records. Exception: %s, Message: %s, FEATURE_IDs: [%s]",
				jobGuid, jobExecutionId, partitionName, batchRecords.size(), cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message", featureIdsPreview
		);

		logger.error(contextualMessage, cause);

		// Use first feature ID for skip tracking
		String firstFeatureId = !featureIds.isEmpty() ? featureIds.get(0) : null;

		return new BatchProjectionException(contextualMessage, cause, firstFeatureId);
	}
}
