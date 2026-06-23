package ca.bc.gov.nrs.vdyp.fip.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.BaseDebugSettings;

public class FipDebugSettings extends BaseDebugSettings {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(FipDebugSettings.class);

	public static final int NO_BA_LIMIT = 1;
	public static final int NO_DQ_LIMIT = 2;

	public FipDebugSettings(Integer[] debugSettingsValues) {
		super(debugSettingsValues);
	}

	@Override
	protected Object process(int settingNumber, int value) {
		switch (settingNumber) {
		case NO_BA_LIMIT, NO_DQ_LIMIT:
			return value != 0;
		default:
			return super.process(settingNumber, value);
		}
	}

	@Override
	public boolean getNoBasalAreaLimit() {
		return (Boolean) this.getProcessedValue(NO_BA_LIMIT);
	}

	@Override
	public boolean getNoQuadraticMeanDiameterLimit() {
		return (Boolean) this.getProcessedValue(NO_DQ_LIMIT);
	}

}
