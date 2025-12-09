package ca.bc.gov.nrs.vdyp.backend.data.models;

public class ProjectionBatchMappingModel {
	private String projectionBatchMappingGUID;
	private String batchJobGUID;
	private ProjectionModel projection;
	private Integer polygonCount;
	private Integer completedPolygonCount;
	private Integer errorCount;
	private Integer warningCount;

	public String getProjectionBatchMappingGUID() {
		return projectionBatchMappingGUID;
	}

	public String getBatchJobGUID() {
		return batchJobGUID;
	}

	public ProjectionModel getProjection() {
		return projection;
	}

	public Integer getPolygonCount() {
		return polygonCount;
	}

	public Integer getCompletedPolygonCount() {
		return completedPolygonCount;
	}

	public Integer getErrorCount() {
		return errorCount;
	}

	public Integer getWarningCount() {
		return warningCount;
	}

	public void setProjectionBatchMappingGUID(String projectionBatchMappingGUID) {
		this.projectionBatchMappingGUID = projectionBatchMappingGUID;
	}

	public void setBatchJobGUID(String batchJobGUID) {
		this.batchJobGUID = batchJobGUID;
	}

	public void setProjection(ProjectionModel projection) {
		this.projection = projection;
	}

	public void setPolygonCount(Integer polygonCount) {
		this.polygonCount = polygonCount;
	}

	public void setCompletedPolygonCount(Integer completedPolygonCount) {
		this.completedPolygonCount = completedPolygonCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
	}

	public void setWarningCount(Integer warningCount) {
		this.warningCount = warningCount;
	}
}
