package ca.bc.gov.nrs.vdyp.backend.data.models;

public class ProjectionBatchMappingModel {
	private String projectionBatchMappingGUID;
	private String batchJobGUID;
	private ProjectionModel projection;
	private Integer partitionCount;
	private Integer completedPartitionCount;
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

	public Integer getPartitionCount() {
		return partitionCount;
	}

	public Integer getCompletedPartitionCount() {
		return completedPartitionCount;
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

	public void setPartitionCount(Integer partitionCount) {
		this.partitionCount = partitionCount;
	}

	public void setCompletedPartitionCount(Integer completedPartitionCount) {
		this.completedPartitionCount = completedPartitionCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
	}

	public void setWarningCount(Integer warningCount) {
		this.warningCount = warningCount;
	}
}
