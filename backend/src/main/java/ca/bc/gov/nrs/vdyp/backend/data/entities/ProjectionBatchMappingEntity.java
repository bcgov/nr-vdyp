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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "projection_batch_mapping")
@Getter
@Setter
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

}
