package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Model layer object to represent a File Set Type. Equality and hashCode are filly defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")
@RegisterForReflection
public class FileSetTypeCodeModel extends CodeTableModel {
	public static final String POLYGON = "POLYGON";
	public static final String LAYER = "LAYER";
	public static final String RESULTS = "RESULTS";
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
