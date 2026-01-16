package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Model layer object to represent a Projection Status. Equality and hashCode are filly defined in the parent class
 * {@link CodeTableModel}. (explicitly calling the abstract getCode() method for the sake of comparison
 */
@SuppressWarnings("squid:S2160")
@RegisterForReflection
public class ProjectionStatusCodeModel extends CodeTableModel {
	public static final String DRAFT = "DRAFT";
	public static final String READY = "READY";
	public static final String RUNNING = "RUNNING";
	public static final String FAILED = "FAILED";

	private String projectionStatusCode;

	@Override
	public String getCode() {
		return projectionStatusCode;
	}

	@Override
	public void setCode(String code) {
		this.projectionStatusCode = code;
	}
}
