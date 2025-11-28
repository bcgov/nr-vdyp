package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class ProjectionStatusCodeModel extends CodeTableModel {
	private String projectionStatusCode;

	@Override
	public String getCode() {
		return projectionStatusCode;
	}

	@Override
	public void setCode(String code) {
		this.projectionStatusCode = code;
	}
}
