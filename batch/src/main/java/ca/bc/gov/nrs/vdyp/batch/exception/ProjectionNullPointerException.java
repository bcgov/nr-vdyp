package ca.bc.gov.nrs.vdyp.batch.exception;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

/**
 * Handling for NPE from ProjectionRunner.run() - ex) when polygon object is null due to data quality issues
 */
public class ProjectionNullPointerException extends IOException {

	private static final long serialVersionUID = 3104617883593359188L;

	private final String jobGuid;
	private final Long jobExecutionId;
	private final String partitionName;
	private final int recordCount;
	private final List<String> featureIds;

	public ProjectionNullPointerException(
			String message, NullPointerException cause, String jobGuid, Long jobExecutionId, String partitionName,
			int recordCount, List<String> featureIds
	) {
		super(message, cause);
		this.jobGuid = jobGuid;
		this.jobExecutionId = jobExecutionId;
		this.partitionName = partitionName;
		this.recordCount = recordCount;
		this.featureIds = featureIds;
	}

	public String getJobGuid() {
		return jobGuid;
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
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

	public static ProjectionNullPointerException handleProjectionNullPointer(
			NullPointerException npe, List<BatchRecord> batchRecords, Long jobExecutionId, String jobGuid,
			String partitionName, Logger logger
	) {
		List<String> featureIds = batchRecords.stream().map(BatchRecord::getFeatureId).toList();

		String featureIdsPreview = featureIds.stream().limit(10).collect(Collectors.joining(", "));

		if (featureIds.size() > 10) {
			featureIdsPreview += String.format(" ... and %d more", featureIds.size() - 10);
		}

		String npeMessage = npe.getMessage() != null ? npe.getMessage() : "No message";
		String contextualMessage = String.format(
				"NullPointerException in projection "
						+ "JobGuid: %s, JobExeId: %d, Partition: %s, Records: %d, NPE message: %s. "
						+ "FEATURE_IDs in chunk: [%s]",
				jobGuid, jobExecutionId, partitionName, batchRecords.size(), npeMessage, featureIdsPreview
		);

		logger.error(contextualMessage);
		logger.error("NPE Stack Trace:", npe);
		logger.debug("All FEATURE_IDs in failed chunk ({}): {}", featureIds.size(), featureIds);

		return new ProjectionNullPointerException(
				contextualMessage, npe, jobGuid, jobExecutionId, partitionName, batchRecords.size(), featureIds
		);
	}
}
