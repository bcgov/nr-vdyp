package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "calculation_engine_code", schema = "\"app-vdyp\"")
@Getter
@Setter
public class CalculationEngineCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "calculation_engine_code", length = 10, nullable = false)
	private String calculationEngineCode;

}
