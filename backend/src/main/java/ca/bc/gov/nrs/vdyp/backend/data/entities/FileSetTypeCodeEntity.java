package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "file_set_type_code")
@Getter
@Setter
public class FileSetTypeCodeEntity extends PanacheEntityBase implements Auditable {
	@Id
	@Column(name = "file_set_type_code", length = 10, nullable = false)
	private String fileSetTypeCode;

	@Column(name = "description", length = 100, nullable = false)
	private String description;

	@Column(name = "display_order", length = 3)
	private BigDecimal displayOrder;

	@NotNull
	@Column(name = "effective_date")
	private LocalDate effectiveDate;

	@NotNull
	@Column(name = "expiry_date")
	private LocalDate expiryDate;

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
