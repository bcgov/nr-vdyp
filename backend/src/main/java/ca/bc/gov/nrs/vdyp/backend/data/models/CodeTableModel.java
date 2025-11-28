package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public abstract class CodeTableModel {
	public abstract String getCode();

	public abstract void setCode(String code);

	private String description;
	private BigDecimal displayOrder;
	private LocalDate effectiveDate;
	private LocalDate expiryDate;
}
