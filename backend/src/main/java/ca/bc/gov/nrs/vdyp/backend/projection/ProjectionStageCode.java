package ca.bc.gov.nrs.vdyp.backend.projection;

public enum ProjectionStageCode {
	Initial, Adjust, Forward, Back;

	public final boolean isProjectionState() {
		return this == Forward || this == Back;
	}
}
