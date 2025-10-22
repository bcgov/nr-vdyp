package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.util.Date;

import lombok.Data;

@Data
public class ProjectionStatusCodeModel {
	public String projectionStatusCode;
	public String description;
	public Integer displayOrder;
	public Date effectiveDate;
	public Date expiryDate;
}
