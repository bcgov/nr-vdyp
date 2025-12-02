package ca.bc.gov.nrs.vdyp.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the debug flag values provided in key 199 of the control files.
 * <p>
 * This class accepts an array of Integers (if null, or the default constructor is used, an empty integer array is
 * used). The array cannot have more than 25 elements, but may have fewer.
 * <p>
 * The class provides one operation: <code>debugValue</code>, which takes the number of debug setting whose value is
 * needed. In keeping with the original FORTRAN code, these values are <b>one-based</b> - so, debug setting "5" is
 * actually stored at <code>settings[4]</code>.
 * <p>
 * It is an error to supply a setting number above 25. If 25 or less but higher than the number of elements in the
 * array, the default value of "0" is returned. Otherwise, the value is read from the array.
 * <p>
 * Instances of class are typically wrapped by an application-specific class that understands the settings in use by the
 * application.
 * <p>
 * See IPSJF155, appendix IX, details.
 */
public class DebugSettings {

	private static final Logger logger = LoggerFactory.getLogger(DebugSettings.class);

	public static final int MAX_DEBUG_SETTINGS = 25;
	private static final int DEFAULT_DEBUG_SETTING = 0;

	public static final int MATH77_MESSAGE_LEVEL = 5;
	public static final int UPPER_BOUNDS_MODE = 4;
	public static final int SPECIES_GROUP_PREFERENCE_MODE = 22;

	private final int[] settings;

	private final Object[] cache;

	private void validateSettingNumber(int settingNumber) {
		if (settingNumber < 1 || settingNumber > MAX_DEBUG_SETTINGS) {
			throw new IllegalArgumentException(
					"Debug setting number " + settingNumber + " is out of range -" + " must be between 1 and "
							+ MAX_DEBUG_SETTINGS
			);
		}
	}

	/**
	 * Create a DebugSettings instance from the given values. If <code>settings</code> is null, this is equivalent to
	 * calling DebugSettings(new Integer[0]). If an entry in <code>settings</code> is null, the default value (0) is
	 * assigned to that variable. If more than <code>MAX_DEBUG_SETTINGS</code> are supplied, those in excess are
	 * ignored.
	 */
	public DebugSettings(Integer[] settings) {

		if (settings == null) {
			settings = new Integer[0];
		}

		if (settings.length > MAX_DEBUG_SETTINGS) {
			throw new IllegalArgumentException(
					"Debug settings array has length " + settings.length + ", which exceeds the maximum length of "
							+ MAX_DEBUG_SETTINGS
			);
		}

		this.settings = new int[MAX_DEBUG_SETTINGS];
		this.cache = new Object[MAX_DEBUG_SETTINGS];

		for (int i = 0; i < MAX_DEBUG_SETTINGS; i++) {
			if (i < settings.length && settings[i] != null) {
				this.settings[i] = settings[i];
			} else {
				this.settings[i] = DEFAULT_DEBUG_SETTING;
			}
			this.update(i + 1);
		}
	}

	/**
	 * Processes an integer setting value
	 *
	 * @param settingNumber The setting number
	 * @param value         The integer value of the setting
	 * @return The processed value of the setting.
	 */
	protected Object process(int settingNumber, int value) {
		switch (settingNumber) {
		case MATH77_MESSAGE_LEVEL:
			return Math77MessagesLevel.fromIndex(value);
		case UPPER_BOUNDS_MODE:
			return UpperBoundsMode.fromIndex(value);
		case SPECIES_GROUP_PREFERENCE_MODE:
			return SpeciesGroupPreference.fromIndex(value);
		default:
			return value;
		}
	}

	/**
	 * Update an entry in the cache
	 */
	protected final void update(int settingNumber) {
		int value = this.getValue(settingNumber);
		this.cache[settingNumber - 1] = process(settingNumber, value);
	}

	/**
	 * Create a DebugSettings instance with all settings set to zero.
	 */
	public DebugSettings() {
		this(new Integer[0]);
	}

	/**
	 * Return the value of the debug variable with setting number <code>settingNumber</code>. This is a <b>one-based</b>
	 * value.
	 *
	 * @param settingNumber the number of the debug variable whose value is to be returned.
	 * @return as described.
	 */
	public int getValue(int settingNumber) {
		validateSettingNumber(settingNumber);

		return settings[settingNumber - 1];
	}

	/**
	 * Return the value of the debug variable with setting number <code>settingNumber</code>. This is a <b>one-based</b>
	 * value.
	 *
	 * @param settingNumber the number of the debug variable whose value is to be returned.
	 * @return as described.
	 */
	public Object getProcessedValue(int settingNumber) {
		validateSettingNumber(settingNumber);

		return cache[settingNumber - 1];
	}

	/**
	 * For testing purposes sometimes it's useful to change the value of a debug setting. It is not expected that this
	 * method would be used for any other purpose.
	 *
	 * @param settingNumber the variable to change
	 * @param value         the new value the variable is to have
	 */
	public void setValue(int settingNumber, int value) {
		logger.atWarn().addArgument(settingNumber).addArgument(getValue(settingNumber)).addArgument(value)
				.setMessage("Changing debug setting {} from {} to {}.  This should only happen when debugging/testing.")
				.log();
		doSetValue(settingNumber, value);
	}

	private void doSetValue(int settingNumber, int value) {
		validateSettingNumber(settingNumber);
		this.settings[settingNumber - 1] = value;
		this.update(settingNumber);
	}

	/**
	 * The mode to determine primary species
	 */
	public enum SpeciesGroupPreference {
		/**
		 * Default behavior
		 */
		DEFAULT,
		/**
		 * Use preferred as primary if ba >.9995 of other.
		 */
		USE_PREFERRED_WITHIN_TOLERANCE;

		static SpeciesGroupPreference fromIndex(int value) {
			switch (value) {
			case 0:
				return SpeciesGroupPreference.DEFAULT;
			case 1:
				return SpeciesGroupPreference.USE_PREFERRED_WITHIN_TOLERANCE;
			default:
				return logDefaultForUnknown(
						logger, "species group preference mode", value, SpeciesGroupPreference.DEFAULT
				);
			}
		}
	}

	/**
	 * Get the mode to determine primary species
	 */
	public SpeciesGroupPreference getSpeciesGroupPreference() {
		// The same across all apps
		return (SpeciesGroupPreference) getProcessedValue(SPECIES_GROUP_PREFERENCE_MODE);
	}

	/**
	 * The message level for the Math77 library.
	 */
	public enum Math77MessagesLevel {
		/**
		 * Don't display any messages
		 */
		NONE,
		/**
		 * Display some messages
		 */
		SOME,
		/**
		 * Display all messages
		 */
		ALL;

		public static Math77MessagesLevel fromIndex(int value) {
			switch (value) {
			case 0:
				return Math77MessagesLevel.NONE;
			case 1:
				return Math77MessagesLevel.SOME;
			case 2:
				return Math77MessagesLevel.ALL;
			default:
				return logDefaultForUnknown(logger, "MATH77 message level", value, Math77MessagesLevel.NONE);
			}

		}
	}

	/**
	 * Get the message level for the Math77 library.
	 */
	public Math77MessagesLevel getMath77MessagesLevel() {

		// The same across all apps. Some config files for FIPStart have comments saying it's 9 but they seem to be
		// wrong.
		return (Math77MessagesLevel) getProcessedValue(MATH77_MESSAGE_LEVEL);
	}

	/**
	 * Upper bounds mode
	 */
	public enum UpperBoundsMode {

		/**
		 * {@link ControlKey.BA_DQ_UPPER_BOUNDS} categorized by {@link VdypLayer.empiricalRelationshipParameterIndex}
		 */
		MODE_1,
		/**
		 * {@link ControlKey.UPPER_BA_BY_CI_S0_P} categorized by region and leading species
		 */
		MODE_2;

		public static UpperBoundsMode fromIndex(int value) {
			switch (value) {
			case 0:
				return logDefault(logger, "upper bounds mode", value, UpperBoundsMode.MODE_2);
			case 1:
				return UpperBoundsMode.MODE_1;
			case 2:
				return UpperBoundsMode.MODE_2;
			default:
				return logDefaultForUnknown(logger, "upper bounds mode", value, UpperBoundsMode.MODE_2);
			}

		}
	}

	/**
	 * Upper bounds mode
	 */
	public UpperBoundsMode getUpperBoundsMode() {
		// The same for FIPStart and Forward, unused elsewhere
		return (UpperBoundsMode) this.getProcessedValue(UPPER_BOUNDS_MODE);
	}

	/**
	 * Log a message when applying a default value and return that value.
	 *
	 * @param <T>          type of the value
	 * @param logger       the logger to add the message to
	 * @param name         the name of the property being filled
	 * @param value        the value being interpreted as null
	 * @param defaultValue the value to use as a default
	 * @return defaultValue
	 */
	protected static <T> T logDefault(Logger logger, String name, int value, T defaultValue) {
		logger.atInfo().setMessage("No {} specified, using default of {}").addArgument(name).addArgument(defaultValue)
				.log();
		return defaultValue;
	}

	/**
	 * Log a message when applying a default value and return that value.
	 *
	 * @param <T>          type of the value
	 * @param logger       the logger to add the message to
	 * @param name         the name of the property being filled
	 * @param value        unknown value
	 * @param defaultValue the value to use as a default
	 * @return defaultValue
	 */
	protected static <T> T logDefaultForUnknown(Logger logger, String name, int value, T defaultValue) {
		logger.atWarn().setMessage("Unknown {} of {}, using default of {}").addArgument(name).addArgument(value)
				.addArgument(defaultValue);
		return defaultValue;
	}
}
