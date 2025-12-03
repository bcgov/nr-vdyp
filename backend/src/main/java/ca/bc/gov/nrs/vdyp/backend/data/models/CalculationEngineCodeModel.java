package ca.bc.gov.nrs.vdyp.backend.data.models;

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
