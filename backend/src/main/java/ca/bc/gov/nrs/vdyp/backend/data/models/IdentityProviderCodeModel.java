package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Model layer object to represent an Identity Provider Code. Equality and hashCode are fully defined in the parent
 * class {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")

@RegisterForReflection
public class IdentityProviderCodeModel extends CodeTableModel {
	public static final String IDIR = "IDIR";
	public static final String BCEID = "BCEID";
	private String identityProviderCode;

	@Override
	public String getCode() {
		return identityProviderCode;
	}

	@Override
	public void setCode(String code) {
		this.identityProviderCode = code;
	}
}
