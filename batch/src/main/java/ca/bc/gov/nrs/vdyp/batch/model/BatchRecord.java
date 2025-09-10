package ca.bc.gov.nrs.vdyp.batch.model;

/**
 * Simple record model for batch processing. This can be extended to match specific VDYP data formats.
 */
public class BatchRecord {

	private Long id;
	private String data;
	private String polygonId;
	private String layerId;
	private String projectionResult;

	public BatchRecord() {
	}

	public BatchRecord(Long id, String data) {
		this.id = id;
		this.data = data;
	}

	public BatchRecord(Long id, String data, String polygonId, String layerId) {
		this.id = id;
		this.data = data;
		this.polygonId = polygonId;
		this.layerId = layerId;
	}

	public BatchRecord(Long id, String data, String polygonId, String layerId, String projectionResult) {
		this.id = id;
		this.data = data;
		this.polygonId = polygonId;
		this.layerId = layerId;
		this.projectionResult = projectionResult;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getPolygonId() {
		return polygonId;
	}

	public void setPolygonId(String polygonId) {
		this.polygonId = polygonId;
	}

	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getProjectionResult() {
		return projectionResult;
	}

	public void setProjectionResult(String projectionResult) {
		this.projectionResult = projectionResult;
	}

	@Override
	public String toString() {
		return "BatchRecord{" + "id=" + id + ", data='" + data + '\'' + ", polygonId='" + polygonId + '\''
				+ ", layerId='" + layerId + '\'' + ", projectionResult='" + projectionResult + '\'' + '}';
	}
}