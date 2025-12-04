package ca.bc.gov.nrs.vdyp.backend.data.models;

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
