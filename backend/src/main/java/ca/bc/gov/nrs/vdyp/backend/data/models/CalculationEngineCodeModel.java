package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class CalculationEngineCodeModel {
	private String calculationEngineCode;
	private String description;
	private BigDecimal displayOrder;
	private Date effectiveDate;
	private Date expiryDate;

}
