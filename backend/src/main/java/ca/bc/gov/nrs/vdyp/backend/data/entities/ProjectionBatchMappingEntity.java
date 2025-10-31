package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import ca.bc.gov.nrs.vdyp.backend.data.Auditable;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditListener.class)
@Table(name = "projection_batch_mapping")
@Getter
@Setter
public class ProjectionBatchMappingEntity extends PanacheEntityBase implements Auditable {

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

	@NotNull
	@Column(name = "revision_count", length = 64, nullable = false)
	private BigDecimal revisionCount;

	@NotNull
	@Column(name = "create_user", length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(name = "create_date", nullable = false)
	private LocalDate createDate;

	@NotNull
	@Column(name = "update_user", length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(name = "update_date", nullable = false)
	private LocalDate updateDate;

	@Override
	public void incrementRevisionCount() {
		this.revisionCount = this.revisionCount.add(BigDecimal.ONE);
	}

	@Override
	public void setCreatedBy(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreatedDate(LocalDate date) {
		this.createDate = date;
	}

	@Override
	public void setLastModifiedBy(String user) {
		this.updateUser = user;
	}

	@Override
	public void setLastModifiedDate(LocalDate date) {
		this.updateDate = date;
	}

}
