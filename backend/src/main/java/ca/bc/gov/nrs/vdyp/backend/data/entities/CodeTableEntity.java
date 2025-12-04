package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

@MappedSuperclass
public abstract class CodeTableEntity extends AuditableEntity {

	public abstract String getCode();

	public abstract void setCode(String code);

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

	public String getDescription() {
		return description;
	}

	public BigDecimal getDisplayOrder() {
		return displayOrder;
	}

	public LocalDate getEffectiveDate() {
		return effectiveDate;
	}

	public LocalDate getExpiryDate() {
		return expiryDate;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDisplayOrder(BigDecimal displayOrder) {
		this.displayOrder = displayOrder;
	}

	public void setEffectiveDate(LocalDate effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public void setExpiryDate(LocalDate expiryDate) {
		this.expiryDate = expiryDate;
	}
}
