package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public abstract class CodeTableModel {
	public abstract String getCode();

	public abstract void setCode(String code);

	private String description;
	private BigDecimal displayOrder;
	private LocalDate effectiveDate;
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

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if ( (other instanceof CodeTableModel ctModel) && this.getClass() == ctModel.getClass()) {
			return StringUtils.equals(getCode(), ctModel.getCode());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getCode());
	}
}
