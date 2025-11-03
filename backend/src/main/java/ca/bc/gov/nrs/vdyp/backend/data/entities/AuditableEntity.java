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

	public void incrementRevisionCount() {
		this.revisionCount = revisionCount.add(BigDecimal.ONE);
	}

	public void setCreatedBy(String createUser) {
		this.createUser = createUser;
	}

	public void setCreatedDate(LocalDate date) {
		this.createDate = date;
	}

	public void setLastModifiedBy(String user) {
		this.updateUser = user;
	}

	public void setLastModifiedDate(LocalDate date) {
		this.updateDate = date;
	}

	private String currentUser() {
		return "system";
	}

	@PrePersist
	public void beforeInsert() {
		setCreatedDate(LocalDate.now());
		setCreatedBy(currentUser());
	}

	@PreUpdate
	public void beforeUpdate() {
		setLastModifiedDate(LocalDate.now());
		setLastModifiedBy(currentUser());
		incrementRevisionCount();
	}
}
