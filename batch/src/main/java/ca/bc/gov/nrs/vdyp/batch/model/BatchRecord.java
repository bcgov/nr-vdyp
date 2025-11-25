package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotNull;

public class BatchRecord {

	@NotNull
	private final String featureId; // business key
	@NotNull
	private final String rawPolygonData;
	@NotNull
	private final List<String> rawLayerData;
	@Nullable
	private final String polygonHeader;
	@Nullable
	private final String layerHeader;
	@NotNull
	private final String partitionName;

	/**
	 * Creates a new BatchRecord
	 *
	 * @param featureId      the unique feature identifier
	 * @param rawPolygonData the raw polygon CSV data
	 * @param rawLayerData   the list of raw layer CSV data lines
	 * @param polygonHeader  the polygon CSV header
	 * @param layerHeader    the layer CSV header
	 * @param partitionName  the partition name
	 */
	public BatchRecord(
			@NotNull String featureId, @NotNull String rawPolygonData, @NotNull List<String> rawLayerData,
			@Nullable String polygonHeader, @Nullable String layerHeader, @NotNull String partitionName
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
