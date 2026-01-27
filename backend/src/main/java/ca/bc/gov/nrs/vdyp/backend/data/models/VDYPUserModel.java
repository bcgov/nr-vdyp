package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class VDYPUserModel {
	private String vdypUserGUID;
	private String oidcGUID;
	private UserTypeCodeModel userTypeCode;
	private String firstName;
	private String lastName;

	public final String getVdypUserGUID() {
		return vdypUserGUID;
	}

	public String getOidcGUID() {
		return oidcGUID;
	}

	public UserTypeCodeModel getUserTypeCode() {
		return userTypeCode;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setVdypUserGUID(String vdypUserGUID) {
		this.vdypUserGUID = vdypUserGUID;
	}

	public void setOidcGUID(String oidcGUID) {
		this.oidcGUID = oidcGUID;
	}

	public void setUserTypeCode(UserTypeCodeModel userTypeCode) {
		this.userTypeCode = userTypeCode;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return obj instanceof VDYPUserModel model && model.vdypUserGUID != null
				&& model.vdypUserGUID.equals(this.vdypUserGUID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(vdypUserGUID);
	}

	public boolean isSystemUser() {
		return this.userTypeCode != null && userTypeCode.isSystemUser();
	}
}
