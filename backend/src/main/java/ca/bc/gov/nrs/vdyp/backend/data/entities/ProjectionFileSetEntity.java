package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import ca.bc.gov.nrs.vdyp.backend.data.Auditable;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditListener.class)
@Table(name = "projection_file_set")
@Getter
@Setter
public class ProjectionFileSetEntity extends PanacheEntityBase implements Auditable {
	@Id
	@GeneratedValue
	@Column(name = "projection_file_set_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionFileSetGUID;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "file_set_type_code", referencedColumnName = "file_set_type_code")
	private FileSetTypeCodeEntity fileSetTypeCode;

	@Column(name = "file_set_name", length = 4000)
	private String fileSetName;

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
