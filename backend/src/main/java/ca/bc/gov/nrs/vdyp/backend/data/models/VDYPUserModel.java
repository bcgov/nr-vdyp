package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class VDYPUserModel {
	private String vdypUserGUID;
	private String oidcGUID;
	private UserTypeCodeModel userTypeCode;
	private String firstName;
	private String lastName;
}
