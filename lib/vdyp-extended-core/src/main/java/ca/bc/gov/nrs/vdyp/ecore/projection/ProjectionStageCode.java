package ca.bc.gov.nrs.vdyp.ecore.projection;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

public enum ProjectionStageCode {
	Initial, Adjust, Forward, Back;

	public final boolean isProjectionState() {
		return this == Forward || this == Back;
	}

	public static ProjectionStageCode of(VdypApplication app) {
		return of(app.getId());
	}

	public static ProjectionStageCode of(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START, VRI_START:
			return Initial;
		case VDYP_FORWARD:
			return Forward;
		default:
			throw new IllegalStateException("Unrecognized application type " + app.getClass().getSimpleName());
		}
	}
}
