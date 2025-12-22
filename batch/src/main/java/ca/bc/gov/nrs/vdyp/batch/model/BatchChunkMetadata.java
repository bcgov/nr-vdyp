package ca.bc.gov.nrs.vdyp.batch.model;

import org.springframework.lang.NonNull;

/**
 * Metadata for a chunk of records to be processed. Instead of loading entire CSV data into memory, this class holds
 * only the information needed to locate and stream the data from partition files.
 *
 * Uses byte offsets for efficient FileChannel-based streaming instead of loading entire chunks into memory.
 */
public class BatchChunkMetadata {

	@NonNull
	private final String partitionName;
	@NonNull
	private final String jobBaseDir;

	// Polygon file metadata
	private final long polygonStartByte; // Start byte offset of polygon data records in this chunk
	private final int polygonRecordCount; // Number of polygon data records in this chunk

	// Layer file metadata
	private final long layerStartByte; // Start byte offset of layer data records in this chunk
	private final int layerRecordCount; // Number of layer data records matching polygon feature IDs in this chunk

	public BatchChunkMetadata(
			@NonNull String partitionName, @NonNull String jobBaseDir, long polygonStartByte, int polygonRecordCount,
			long layerStartByte, int layerRecordCount
	) {
		this.partitionName = partitionName;
		this.jobBaseDir = jobBaseDir;
		this.polygonStartByte = polygonStartByte;
		this.polygonRecordCount = polygonRecordCount;
		this.layerStartByte = layerStartByte;
		this.layerRecordCount = layerRecordCount;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public String getJobBaseDir() {
		return jobBaseDir;
	}

	public long getPolygonStartByte() {
		return polygonStartByte;
	}

	public int getPolygonRecordCount() {
		return polygonRecordCount;
	}

	public long getLayerStartByte() {
		return layerStartByte;
	}

	public int getLayerRecordCount() {
		return layerRecordCount;
	}

	@Override
	public String toString() {
		return "ChunkMetadata{" + "partitionName='" + partitionName + '\'' + ", jobBaseDir='" + jobBaseDir + '\''
				+ ", polygonStartByte=" + polygonStartByte + ", polygonRecordCount=" + polygonRecordCount
				+ ", layerStartByte=" + layerStartByte + ", layerRecordCount=" + layerRecordCount + '}';
	}
}
