package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class BatchRecord {

	@NonNull
	private final String featureId; // business key
	@NonNull
	private final String rawPolygonData;
	@NonNull
	private final List<String> rawLayerData;
	@Nullable
	private final String polygonHeader;
	@Nullable
	private final String layerHeader;
	@NonNull
	private final String partitionName;

	public BatchRecord(
			@NonNull String featureId, @NonNull String rawPolygonData, @NonNull List<String> rawLayerData,
			@Nullable String polygonHeader, @Nullable String layerHeader, @NonNull String partitionName
	) {
		this.featureId = featureId;
		this.rawPolygonData = rawPolygonData;
		this.rawLayerData = rawLayerData;
		this.polygonHeader = polygonHeader;
		this.layerHeader = layerHeader;
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

	public String getPolygonHeader() {
		return polygonHeader;
	}

	public String getLayerHeader() {
		return layerHeader;
	}

	public String getPartitionName() {
		return partitionName;
	}

	@Override
	public String toString() {
		return "BatchRecord{" + "featureId='" + featureId + '\'' + ", rawPolygonData='" + rawPolygonData + '\''
				+ ", rawLayerData=" + rawLayerData + ", polygonHeader='" + polygonHeader + '\'' + ", layerHeader='"
				+ layerHeader + '\'' + ", partitionName='" + partitionName + '\'' + '}';
	}
}
