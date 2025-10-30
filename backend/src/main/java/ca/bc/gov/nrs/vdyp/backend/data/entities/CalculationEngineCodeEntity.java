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
@Table(name = "calculation_engine_code")
@Getter
@Setter
public class CalculationEngineCodeEntity extends PanacheEntityBase implements Auditable {
	@Id
	@Column(name = "calculation_engine_code", length = 10, nullable = false)
	private String calculationEngineCode;

	@Column(length = 100, nullable = false)
	private String description;

	@Column(length = 3)
	private Integer displayOrder;

	@NotNull
	private Date effectiveDate;

	@NotNull
	private Date expiryDate;

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
