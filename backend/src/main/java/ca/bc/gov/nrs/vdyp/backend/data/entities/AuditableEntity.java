package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;

@MappedSuperclass
public abstract class AuditableEntity extends PanacheEntityBase {
	@NotNull
	@Column(name = "revision_count", length = 64, nullable = false)
	private BigDecimal revisionCount;

	@NotNull
	@Column(name = "create_user", length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(name = "create_date", nullable = false)
	private OffsetDateTime createDate;

	@NotNull
	@Column(name = "update_user", length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(name = "update_date", nullable = false)
	private OffsetDateTime updateDate;

	private String currentUser() {
		return "system";
	}

	private void incrementRevisionCount() {
		if (this.revisionCount == null) {
			this.revisionCount = BigDecimal.ZERO;
		}
		this.revisionCount = this.revisionCount.add(BigDecimal.ONE);
	}

	public BigDecimal getRevisionCount() {
		return revisionCount;
	}

	public String getCreateUser() {
		return createUser;
	}

	public OffsetDateTime getCreateDate() {
		return createDate;
	}

	public String getUpdateUser() {
		return updateUser;
	}

	public OffsetDateTime getUpdateDate() {
		return updateDate;
	}

	private void setCreateDate(OffsetDateTime date) {
		this.createDate = date;
	}

	private void setCreateUser(String user) {
		this.createUser = user;
	}

	private void setUpdateDate(OffsetDateTime date) {
		this.updateDate = date;
	}

	private void setUpdateUser(String user) {
		this.updateUser = user;
	}

	@PrePersist
	public void beforeInsert() {
		setCreateDate(OffsetDateTime.now());
		setCreateUser(currentUser());
		setUpdateDate(OffsetDateTime.now());
		setUpdateUser(currentUser());
		incrementRevisionCount();
	}

	@PreUpdate
	public void beforeUpdate() {
		setUpdateDate(OffsetDateTime.now());
		setUpdateUser(currentUser());
		incrementRevisionCount();
	}

}
