package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.BaseDebugSettings;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;

public class VriDebugSettings extends BaseDebugSettings implements NonFipDebugSettings {

	private static final Logger logger = LoggerFactory.getLogger(VriDebugSettings.class);

	public static final int MODE_1_ERRORS_FATAL = 1;
	public static final int MAX_BREAST_HEIGHT_AGE = 2;
	public static final int EXPAND_DQ_FOR_TPH_RECOVERY = 9;
	public static final float EXPAND_DQ_FOR_TPH_RECOVERY_DIVISOR = 100f;

	public static final float MAX_BREAST_HEIGHT_AGE_MULTIPLIER = 100f;

	public VriDebugSettings() {
		super();
	}

	public VriDebugSettings(Integer[] settings) {
		super(settings);
	}

	@Override
	protected Object process(int settingNumber, int value) {
		switch (settingNumber) {
		case MODE_1_ERRORS_FATAL:
			return value == 2; // Specifically uses 2 rather than non-zero like other booleans
		case MAX_BREAST_HEIGHT_AGE:
			return Optional.of(value).filter(x -> x > 0).map(x -> x * MAX_BREAST_HEIGHT_AGE_MULTIPLIER);
		case EXPAND_DQ_FOR_TPH_RECOVERY:
			return Optional.of(value).filter(x -> x > 0).map(x -> x / EXPAND_DQ_FOR_TPH_RECOVERY_DIVISOR);
		default:
			return super.process(settingNumber, value);
		}
	}

	/**
	 * Are BA and TPH errors in Mode 1 fatal
	 */
	public boolean getMode1ErrorsFatal() {
		return (Boolean) getProcessedValue(MODE_1_ERRORS_FATAL);
	}

	/**
	 * Factor to expand DQ to recover TPH
	 */
	@SuppressWarnings("unchecked")
	public Optional<Float> getExpandDiameterForTPHRecovery() {
		return (Optional<Float>) getProcessedValue(EXPAND_DQ_FOR_TPH_RECOVERY);
	}

	/**
	 * Get the maximum breast height age if there is a limit.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Optional<Float> getMaxBreastHeightAge() {
		return (Optional<Float>) getProcessedValue(MAX_BREAST_HEIGHT_AGE);
	}
}
