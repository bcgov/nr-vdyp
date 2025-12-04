package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projection_status_code")
public class ProjectionStatusCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "projection_status_code", length = 10, nullable = false)
	private String projectionStatusCode;

	public String getCode() {
		return projectionStatusCode;
	}

	public void setCode(String code) {
		this.projectionStatusCode = code;
	}
}
