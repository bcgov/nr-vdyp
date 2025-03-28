package ca.bc.gov.nrs.vdyp.backend.v1.gen.model;

public enum ProjectionRequestKind {

	HCSV("hcsv"), ICSV("icsv"), SCSV("scsv"), DCSV("dcsv");

	private final String textRepresentation;

	ProjectionRequestKind(String textRepresentation) {
		this.textRepresentation = textRepresentation;
	}

	@Override
	public String toString() {
		return textRepresentation;
	}
}
