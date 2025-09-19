package ca.bc.gov.nrs.vdyp.batch.model;

import java.util.List;

/**
 * VDYP-specific batch record model representing a complete polygon entity for VDYP projection.
 *
 * This model implements the FEATURE_ID-based processing strategy where each BatchRecord contains exactly one polygon
 * plus ALL its associated layers as a complete, atomic processing unit.
 */
public class BatchRecord {

	private String featureId; // business key
	private Polygon polygon;
	private List<Layer> layers;
	private String projectionResult;

	public BatchRecord() {
	}

	public BatchRecord(String featureId, Polygon polygon, List<Layer> layers) {
		this.featureId = featureId;
		this.polygon = polygon;
		this.layers = layers;
	}

	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public Polygon getPolygon() {
		return polygon;
	}

	public void setPolygon(Polygon polygon) {
		this.polygon = polygon;
	}

	public List<Layer> getLayers() {
		return layers;
	}

	public void setLayers(List<Layer> layers) {
		this.layers = layers;
	}

	public String getProjectionResult() {
		return projectionResult;
	}

	public void setProjectionResult(String projectionResult) {
		this.projectionResult = projectionResult;
	}

	@Override
	public String toString() {
		return "BatchRecord{" + "featureId='" + featureId + '\'' + ", polygon=" + polygon + ", layerCount="
				+ (layers != null ? layers.size() : 0) + ", projectionResult='" + projectionResult + '\'' + '}';
	}
}