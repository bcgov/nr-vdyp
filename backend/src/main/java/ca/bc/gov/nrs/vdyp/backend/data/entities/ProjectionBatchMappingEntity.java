package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.Date;
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
	@Column(nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionBatchMappingGUID;

	@Column(columnDefinition = "uuid")
	private UUID batchJobGUID;

	@OneToOne
	@JoinColumn(name = "projection_guid", referencedColumnName = "projection_guid")
	private ProjectionEntity projection;

	private Integer partitionCount;
	private Integer completedPartitionCount;
	private Integer errorCount;
	private Integer warningCount;

	@NotNull
	private Integer revisionCount;

	@NotNull
	@Column(length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(nullable = false)
	private Date createDate;

	@NotNull
	@Column(length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(nullable = false)
	private Date updateDate;

	@Override
	public void incrementRevisionCount() {
		this.revisionCount++;
	}

	@Override
	public void setCreatedBy(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreatedDate(Date date) {
		this.createDate = date;
	}

	@Override
	public void setLastModifiedBy(String user) {
		this.updateUser = user;
	}

	@Override
	public void setLastModifiedDate(Date date) {
		this.updateDate = date;
	}

}
