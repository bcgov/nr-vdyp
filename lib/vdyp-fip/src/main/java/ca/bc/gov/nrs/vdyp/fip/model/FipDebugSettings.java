package ca.bc.gov.nrs.vdyp.fip.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class FipDebugSettings extends DebugSettings {

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

	/**
	 * Disable limit on basal area
	 */
	public boolean getNoBasalAreaLimit() {
		return (Boolean) this.getProcessedValue(NO_BA_LIMIT);
	}

	/**
	 * Disable limit on quadratic mean diameter
	 */
	public boolean getNoQuadraticMeanDiameterLimit() {
		return (Boolean) this.getProcessedValue(NO_DQ_LIMIT);
	}

}
