package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class UserTypeCodeModel {
	private String userTypeCode;
	private String description;
	private BigDecimal displayOrder;
	private LocalDate effectiveDate;
	private LocalDate expiryDate;
}
