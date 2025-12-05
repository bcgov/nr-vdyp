package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_type_code")
public class UserTypeCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "user_type_code", length = 10, nullable = false)
	private String userTypeCode;

	public String getCode() {
		return userTypeCode;
	}

	public void setCode(String code) {
		this.userTypeCode = code;
	}
}
