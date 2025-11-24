package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;
import java.util.Objects;

public class BatchRecord {

	private final String featureId; // business key

	private final String rawPolygonData;
	private final List<String> rawLayerData;
	private final String polygonHeader;
	private final String layerHeader;

	private final String partitionName;

	/**
	 * Creates a new BatchRecord
	 *
	 * @param featureId      the unique feature identifier
	 * @param rawPolygonData the raw polygon CSV data
	 * @param rawLayerData   the list of raw layer CSV data lines
	 * @param polygonHeader  the polygon CSV header (may be null)
	 * @param layerHeader    the layer CSV header (may be null)
	 * @param partitionName  the partition name
	 * @throws NullPointerException if featureId, rawPolygonData, rawLayerData, or partitionName is null
	 */
	public BatchRecord(
			String featureId, String rawPolygonData, List<String> rawLayerData, String polygonHeader,
			String layerHeader, String partitionName
	) {
		this.featureId = Objects.requireNonNull(featureId, "featureId must not be null");
		this.rawPolygonData = Objects.requireNonNull(rawPolygonData, "rawPolygonData must not be null");
		this.rawLayerData = Objects.requireNonNull(rawLayerData, "rawLayerData must not be null");
		this.polygonHeader = polygonHeader;
		this.layerHeader = layerHeader;
		this.partitionName = Objects.requireNonNull(partitionName, "partitionName must not be null");
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
