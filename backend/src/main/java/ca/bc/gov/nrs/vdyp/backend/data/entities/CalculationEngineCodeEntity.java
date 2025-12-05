package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "calculation_engine_code", schema = "\"app-vdyp\"")
public class CalculationEngineCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "calculation_engine_code", length = 10, nullable = false)
	private String calculationEngineCode;

	public String getCode() {
		return calculationEngineCode;
	}

	public void setCode(String code) {
		this.calculationEngineCode = code;
	}
}
