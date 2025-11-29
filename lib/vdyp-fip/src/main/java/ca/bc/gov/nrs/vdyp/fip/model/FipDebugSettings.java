package ca.bc.gov.nrs.vdyp.fip.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class FipDebugSettings extends DebugSettings {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(FipDebugSettings.class);

	public static final int NO_BA_LIMIT = 1;
	public static final int NO_DQ_LIMIT = 2;
	public static final int MATH77_MESSAGE_LEVEL = 9;

	public FipDebugSettings(Integer[] debugSettingsValues) {
		super(debugSettingsValues);
	}

	@Override
	protected Object process(int settingNumber, int value) {
		switch (settingNumber) {
		case MATH77_MESSAGE_LEVEL:
			return Math77MessagesLevel.fromIndex(value);
		case DebugSettings.MATH77_MESSAGE_LEVEL:
			return value;
		case NO_BA_LIMIT, NO_DQ_LIMIT:
			return value != 0;
		default:
			return super.process(settingNumber, value);
		}
	}

	/**
	 * Disable limit on basal area
	 */
	boolean getNoBasalAreaLimit() {
		return (Boolean) this.getProcessedValue(NO_BA_LIMIT);
	}

	/**
	 * Disable limit on quadratic mean diameter
	 */
	boolean getNoQuadraticMeanDiameterLimit() {
		return (Boolean) this.getProcessedValue(NO_DQ_LIMIT);
	}

	@Override
	public Math77MessagesLevel getMath77MessagesLevel() {
		return (Math77MessagesLevel) getProcessedValue(MATH77_MESSAGE_LEVEL);
	}

}
