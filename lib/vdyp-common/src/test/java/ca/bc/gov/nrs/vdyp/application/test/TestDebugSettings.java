package ca.bc.gov.nrs.vdyp.application.test;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.BaseDebugSettings;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;

public class TestDebugSettings extends BaseDebugSettings implements NonFipDebugSettings {

	public TestDebugSettings() {
		super();
	}

	public TestDebugSettings(Integer[] settings) {
		super(settings);
	}

	@Override
	public Optional<Float> getMaxBreastHeightAge() {
		return Optional.empty();
	}

	@Override
	public Optional<Float> getExpandDiameterForTPHRecovery() {
		return Optional.empty();
	}

	@Override
	public boolean getMode1ErrorsFatal() {
		return false;
	}

}
