package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class FileSetTypeCodeModel extends CodeTableModel {
	private String fileSetTypeCode;

	@Override
	public String getCode() {
		return fileSetTypeCode;
	}

	@Override
	public void setCode(String code) {
		this.fileSetTypeCode = code;
	}

}
