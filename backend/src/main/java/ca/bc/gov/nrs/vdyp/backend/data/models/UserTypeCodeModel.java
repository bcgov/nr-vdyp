package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Model layer object to represent a User Type Status. Equality and hashCode are filly defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")

@RegisterForReflection
public class UserTypeCodeModel extends CodeTableModel {
	public static final String SYSTEM = "SYSTEM";
	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";
	private String userTypeCode;

	@Override
	public String getCode() {
		return userTypeCode;
	}

	@Override
	public void setCode(String code) {
		this.userTypeCode = code;
	}

	public boolean isSystemUser() {
		return SYSTEM.equalsIgnoreCase(this.userTypeCode);
	}

}
