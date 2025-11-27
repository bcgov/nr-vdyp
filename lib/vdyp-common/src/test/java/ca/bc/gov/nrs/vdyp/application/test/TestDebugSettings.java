package ca.bc.gov.nrs.vdyp.application.test;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class TestDebugSettings extends DebugSettings<TestDebugSettings.Vars> {
	public static enum Vars implements DebugSettings.Vars {
		VAR1, VAR2
	}

	public TestDebugSettings() {
		super();
	}

	public TestDebugSettings(Integer[] settings) {
		super(settings);
	}

}
