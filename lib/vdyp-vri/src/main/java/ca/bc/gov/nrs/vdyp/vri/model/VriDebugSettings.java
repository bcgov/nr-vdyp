package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class VriDebugSettings extends DebugSettings {

	private static final Logger logger = LoggerFactory.getLogger(VriDebugSettings.class);

	public static final int MODE_1_ERRORS_FATAL = 1;
	public static final int NONPRIMARY_LOREY_HEIGHT_CALCULATION = 2;
	public static final int EXPAND_DQ_FOR_TPH_RECOVERY = 9;
	public static final float EXPAND_DQ_FOR_TPH_RECOVERY_DIVISOR = 100f;

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
		case NONPRIMARY_LOREY_HEIGHT_CALCULATION:
			return NonprimaryLoreyHeightMode.fromIndex(value);
		case EXPAND_DQ_FOR_TPH_RECOVERY:
			return Optional.of(value).filter(x -> x > 0).map(x -> x / EXPAND_DQ_FOR_TPH_RECOVERY_DIVISOR);
		default:
			return super.process(settingNumber, value);
		}
	}

	/**
	 * How to calculate lorey height for non-primary species
	 */
	public enum NonprimaryLoreyHeightMode {
		/**
		 * Its own dominant height
		 */
		SELF,
		/**
		 * Primary species dominant height
		 */
		PRIMARY;

		public static NonprimaryLoreyHeightMode fromIndex(int value) {
			switch (value) {
			case 0:
				return NonprimaryLoreyHeightMode.SELF;
			case 1:
				return NonprimaryLoreyHeightMode.PRIMARY;
			default:
				return logDefaultForUnknown(
						logger, "nonprimary lorey height mode", value, NonprimaryLoreyHeightMode.SELF
				);
			}

		}
	}

	/**
	 * Are BA and TPH errors in Mode 1 fatal
	 */
	public boolean getMode1ErrorsFatal() {
		return (Boolean) getProcessedValue(MODE_1_ERRORS_FATAL);
	}

	/**
	 * How to calculate lorey height for non-primary species
	 */
	public NonprimaryLoreyHeightMode getNonprimaryLoreyHeightMode() {
		return (NonprimaryLoreyHeightMode) getProcessedValue(NONPRIMARY_LOREY_HEIGHT_CALCULATION);
	}

	/**
	 * Factor to expand DQ to recover TPH
	 */
	@SuppressWarnings("unchecked")
	public Optional<Float> getExpandDiameterForTPHRecovery() {
		return (Optional<Float>) getProcessedValue(EXPAND_DQ_FOR_TPH_RECOVERY);
	}
}
