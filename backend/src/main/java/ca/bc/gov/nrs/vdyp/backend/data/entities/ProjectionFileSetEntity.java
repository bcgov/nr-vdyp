package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.Date;
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
	@JoinColumn(referencedColumnName = "file_set_type_code")
	private FileSetTypeCodeEntity fileSetTypeCode;

	@Column(length = 4000)
	private String fileSetName;

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
