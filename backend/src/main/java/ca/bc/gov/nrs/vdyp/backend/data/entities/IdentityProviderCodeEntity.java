package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_provider_code")
public class IdentityProviderCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "identity_provider_code", length = 10, nullable = false)
	private String identityProviderCode;

	public String getCode() {
		return identityProviderCode;
	}

	public void setCode(String code) {
		this.identityProviderCode = code;
	}
}
