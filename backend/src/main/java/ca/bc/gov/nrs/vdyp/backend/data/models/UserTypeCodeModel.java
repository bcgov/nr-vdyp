package ca.bc.gov.nrs.vdyp.backend.data.models;

/**
 * Model layer object to represent a Projection Status. Equality and hashCode are filly defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")
public class UserTypeCodeModel extends CodeTableModel {
	private String userTypeCode;

	@Override
	public String getCode() {
		return userTypeCode;
	}

	@Override
	public void setCode(String code) {
		this.userTypeCode = code;
	}

}
