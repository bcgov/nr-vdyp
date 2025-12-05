package ca.bc.gov.nrs.vdyp.backend.data.models;

/**
 * Model layer object to represent a Calculation Engine. Equality and hashCode are filly defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")
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
