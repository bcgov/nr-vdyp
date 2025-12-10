package ca.bc.gov.nrs.vdyp.batch.model;

import org.springframework.lang.NonNull;

/**
 * Metadata for a chunk of records to be processed. Instead of loading entire CSV data into memory, this class holds
 * only the information needed to locate and stream the data from partition files.
 */
public class BatchChunkMetadata {

	@NonNull
	private final String partitionName;
	@NonNull
	private final String jobBaseDir;
	private final int startIndex;
	private final int recordCount;

	public BatchChunkMetadata(
			@NonNull String partitionName, @NonNull String jobBaseDir, int startIndex, int recordCount
	) {
		this.partitionName = partitionName;
		this.jobBaseDir = jobBaseDir;
		this.startIndex = startIndex;
		this.recordCount = recordCount;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public String getJobBaseDir() {
		return jobBaseDir;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getRecordCount() {
		return recordCount;
	}

	@Override
	public String toString() {
		return "ChunkMetadata{" + "partitionName='" + partitionName + '\'' + ", jobBaseDir='" + jobBaseDir + '\''
				+ ", startIndex=" + startIndex + ", recordCount=" + recordCount + '}';
	}
}
