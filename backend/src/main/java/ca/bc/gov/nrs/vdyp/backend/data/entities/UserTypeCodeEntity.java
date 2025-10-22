package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.Date;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import ca.bc.gov.nrs.vdyp.backend.data.Auditable;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditListener.class)
@Table(name = "user_type_code")
@Getter
@Setter
public class UserTypeCodeEntity extends PanacheEntityBase implements Auditable {
	@Id
	@Column(length = 10, nullable = false)
	private String userTypeCode;

	@Column(length = 100, nullable = false)
	private String description;

	@Column(length = 3)
	private Integer displayOrder;

	@NotNull
	private Date effectiveDate;

	@NotNull
	private Date expiryDate;

	@NotNull
	@Column(name = "revision_count", columnDefinition = "numeric(10) default '0'")
	private Integer revisionCount;

	@NotNull
	@Column(name = "create_user", length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(name = "create_date", nullable = false)
	private Date createDate;

	@NotNull
	@Column(name = "update_user", length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(name = "update_date", nullable = false)
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
