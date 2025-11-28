package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity extends PanacheEntityBase {
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

	private String currentUser() {
		return "system";
	}

	private void incrementRevisionCount() {
		if (this.revisionCount == null) {
			this.revisionCount = BigDecimal.ZERO;
		}
		this.revisionCount = this.revisionCount.add(BigDecimal.ONE);
	}

	@PrePersist
	public void beforeInsert() {
		setCreateDate(LocalDate.now());
		setCreateUser(currentUser());
		setUpdateDate(LocalDate.now());
		setUpdateUser(currentUser());
		incrementRevisionCount();
	}

	@PreUpdate
	public void beforeUpdate() {
		setUpdateDate(LocalDate.now());
		setUpdateUser(currentUser());
		incrementRevisionCount();
	}
}
