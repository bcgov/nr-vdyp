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

	private final String partitionName;
	private final int recordCount;
	private final List<String> featureIds;

	private BatchProjectionException(
			String message, Throwable cause, String jobGuid, Long jobExecutionId, String featureId,
			ProjectionContext context
	) {
		super(message, cause, jobGuid, jobExecutionId, featureId, false, true);
		this.partitionName = context.partitionName;
		this.recordCount = context.recordCount;
		this.featureIds = context.featureIds;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public List<String> getFeatureIds() {
		return featureIds;
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

		ProjectionContext context = new ProjectionContext(partitionName, batchRecords.size(), featureIds);

		return new BatchProjectionException(contextualMessage, cause, jobGuid, jobExecutionId, firstFeatureId, context);
	}

	public static class ProjectionContext {

		private final String partitionName;
		private final int recordCount;
		private final List<String> featureIds;

		public ProjectionContext(String partitionName, int recordCount, List<String> featureIds) {
			this.partitionName = partitionName;
			this.recordCount = recordCount;
			this.featureIds = featureIds;
		}

		public String getPartitionName() {
			return partitionName;
		}

		public int getRecordCount() {
			return recordCount;
		}

		public List<String> getFeatureIds() {
			return featureIds;
		}
	}
}
