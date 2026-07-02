package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Model layer object to represent a Batch Failure Type. Equality and hashCode are fully defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")
@RegisterForReflection
public class BatchFailureTypeCodeModel extends CodeTableModel {
	public static final String INPUT = "INPUT";
	public static final String PROCESS = "PROCESS";
	public static final String OUTPUT = "OUTPUT";
	private String batchFailureTypeCode;

	@Override
	public String getCode() {
		return batchFailureTypeCode;
	}

	@Override
	public void setCode(String code) {
		this.batchFailureTypeCode = code;
	}

}
