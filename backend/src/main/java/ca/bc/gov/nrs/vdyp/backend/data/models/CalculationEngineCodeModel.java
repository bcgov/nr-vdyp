package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class CalculationEngineCodeModel extends CodeTableModel {
	private String calculationEngineCode;

	@Override
	public String getCode() {
		return calculationEngineCode;
	}

	@Override
	public void setCode(String code) {
		this.calculationEngineCode = code;
	}

}
