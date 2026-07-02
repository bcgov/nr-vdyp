package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_failure_type_code")
public class BatchFailureTypeCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "batch_failure_type_code", length = 10, nullable = false)
	private String batchFailureTypeCode;

	public String getCode() {
		return batchFailureTypeCode;
	}

	public void setCode(String code) {
		this.batchFailureTypeCode = code;
	}
}
