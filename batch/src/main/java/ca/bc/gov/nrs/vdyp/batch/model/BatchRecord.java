package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

import org.springframework.lang.NonNull;

/**
 * Represents a batch processing record containing raw CSV data for a single polygon and its associated layers.
 */
public class BatchRecord {

	@NonNull
	private final String featureId; // business key
	@NonNull
	private final String rawPolygonData;
	@NonNull
	private final List<String> rawLayerData;
	@NonNull
	private final String partitionName;

	public BatchRecord(
			@NonNull String featureId, @NonNull String rawPolygonData, @NonNull List<String> rawLayerData,
			@NonNull String partitionName
	) {
		this.featureId = featureId;
		this.rawPolygonData = rawPolygonData;
		this.rawLayerData = rawLayerData;
		this.partitionName = partitionName;
	}

	public String getFeatureId() {
		return featureId;
	}

	public String getRawPolygonData() {
		return rawPolygonData;
	}

	public List<String> getRawLayerData() {
		return rawLayerData;
	}

	public String getPartitionName() {
		return partitionName;
	}

	@Override
	public String toString() {
		return "BatchRecord{" + "featureId='" + featureId + '\'' + ", rawPolygonData='" + rawPolygonData + '\''
				+ ", rawLayerData=" + rawLayerData + ", partitionName='" + partitionName + '\'' + '}';
	}
}
