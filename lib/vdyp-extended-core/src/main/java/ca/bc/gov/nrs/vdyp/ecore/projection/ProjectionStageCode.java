package ca.bc.gov.nrs.vdyp.ecore.projection;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public enum ProjectionStageCode {
	Initial, Adjust, Forward, Back;

	public final boolean isProjectionState() {
		return this == Forward || this == Back;
	}

	public static ProjectionStageCode of(VdypApplication app) {
		if (app instanceof FipStart || app instanceof VriStart) {
			return Initial;
		} else if (app instanceof VdypForwardApplication) {
			return Forward;
// TODO: once these applications are implemented...
//		} else if (app instanceof VdypAdjustApplication) {
//			return Adjust;
//		} else if (app instanceof VdypBackApplication) {
//			return Back;
		} else {
			throw new IllegalStateException("Unrecognized application type " + app.getClass().getSimpleName());
		}
	}
}
