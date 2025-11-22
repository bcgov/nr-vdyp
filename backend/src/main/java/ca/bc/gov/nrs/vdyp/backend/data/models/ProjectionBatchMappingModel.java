package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class ProjectionBatchMappingModel {
	private String projectionBatchMappingGUID;
	private String batchJobGUID;
	private ProjectionModel projection;
	private Integer partitionCount;
	private Integer completedPartitionCount;
	private Integer errorCount;
	private Integer warningCount;

}
