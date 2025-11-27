package ca.bc.gov.nrs.vdyp.vri.model;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class VriDebugSettings extends DebugSettings<VriDebugSettings.Vars> {

	public enum Vars implements DebugSettings.Vars {

	}

	public VriDebugSettings() {
		super();
	}

	public VriDebugSettings(Integer[] settings) {
		super(settings);
	}

}
