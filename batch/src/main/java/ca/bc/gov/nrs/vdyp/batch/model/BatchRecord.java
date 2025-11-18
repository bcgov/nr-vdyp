package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

// MDJ: Like BatchMetrics, this class is not implemented correctly. Its one use in the code is in ChunkBasedPolygonReader, 
// which creates a instance and then calls six set methods. 
public class BatchRecord {

	private String featureId; // business key

	private String rawPolygonData;
	private List<String> rawLayerData;
	private String polygonHeader;
	private String layerHeader;

	private String partitionName;

	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public String getRawPolygonData() {
		return rawPolygonData;
	}

	public void setRawPolygonData(String rawPolygonData) {
		this.rawPolygonData = rawPolygonData;
	}

	public List<String> getRawLayerData() {
		return rawLayerData;
	}

	public void setRawLayerData(List<String> rawLayerData) {
		this.rawLayerData = rawLayerData;
	}

	public String getPolygonHeader() {
		return polygonHeader;
	}

	public void setPolygonHeader(String polygonHeader) {
		this.polygonHeader = polygonHeader;
	}

	public String getLayerHeader() {
		return layerHeader;
	}

	public void setLayerHeader(String layerHeader) {
		this.layerHeader = layerHeader;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public void setPartitionName(String partitionName) {
		this.partitionName = partitionName;
	}

	@Override
	public String toString() {
		return "BatchRecord{" + "featureId='" + featureId + '\'' + ", rawPolygonData='" + rawPolygonData + '\''
				+ ", rawLayerData=" + rawLayerData + ", polygonHeader='" + polygonHeader + '\'' + ", layerHeader='"
				+ layerHeader + '\'' + ", partitionName='" + partitionName + '\'' + '}';
	}
}
