package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projection_batch_mapping")
public class ProjectionBatchMappingEntity extends AuditableEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "projection_batch_mapping_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionBatchMappingGUID;

	@Column(name = "batch_job_guid", columnDefinition = "uuid")
	private UUID batchJobGUID;

	@OneToOne
	@JoinColumn(name = "projection_guid", referencedColumnName = "projection_guid")
	private ProjectionEntity projection;

	@Column(name = "partition_count")
	private Integer partitionCount;
	@Column(name = "completed_partition_count")
	private Integer completedPartitionCount;
	@Column(name = "error_count")
	private Integer errorCount;
	@Column(name = "warning_count")
	private Integer warningCount;

	public UUID getProjectionBatchMappingGUID() {
		return projectionBatchMappingGUID;
	}

	public UUID getBatchJobGUID() {
		return batchJobGUID;
	}

	public ProjectionEntity getProjection() {
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

	public void setProjectionBatchMappingGUID(UUID projectionBatchMappingGUID) {
		this.projectionBatchMappingGUID = projectionBatchMappingGUID;
	}

	public void setBatchJobGUID(UUID batchJobGUID) {
		this.batchJobGUID = batchJobGUID;
	}

	public void setProjection(ProjectionEntity projection) {
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
