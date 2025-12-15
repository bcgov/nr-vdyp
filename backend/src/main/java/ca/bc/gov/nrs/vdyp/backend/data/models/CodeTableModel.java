package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.math.BigDecimal;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class CodeTableModel {
	public abstract String getCode();

	public abstract void setCode(String code);

	private String description;
	private BigDecimal displayOrder;

	public String getDescription() {
		return description;
	}

	public BigDecimal getDisplayOrder() {
		return displayOrder;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDisplayOrder(BigDecimal displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if ( (other instanceof CodeTableModel ctModel) && this.getClass() == ctModel.getClass()) {
			return Objects.equals(getCode(), ctModel.getCode());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getCode());
	}
}
