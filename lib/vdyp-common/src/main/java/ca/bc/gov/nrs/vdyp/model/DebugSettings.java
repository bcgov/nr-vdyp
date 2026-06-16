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
public interface DebugSettings {

	public static final int MAX_DEBUG_SETTINGS = 25;

	public static final int MATH77_MESSAGE_LEVEL = 5;
	public static final int UPPER_BOUNDS_MODE = 4;
	public static final int SPECIES_GROUP_PREFERENCE_MODE = 22;

	/**
	 * Return the value of the debug variable with setting number <code>settingNumber</code>. This is a <b>one-based</b>
	 * value.
	 *
	 * @param settingNumber the number of the debug variable whose value is to be returned.
	 * @return as described.
	 */
	public int getValue(int settingNumber);

	/**
	 * Return the value of the debug variable with setting number <code>settingNumber</code>. This is a <b>one-based</b>
	 * value.
	 *
	 * @param settingNumber the number of the debug variable whose value is to be returned.
	 * @return as described.
	 */
	public Object getProcessedValue(int settingNumber);

	/**
	 * For testing purposes sometimes it's useful to change the value of a debug setting. It is not expected that this
	 * method would be used for any other purpose.
	 *
	 * @param settingNumber the variable to change
	 * @param value         the new value the variable is to have
	 */
	public void setValue(int settingNumber, int value);

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

		private static final Logger logger = LoggerFactory.getLogger(SpeciesGroupPreference.class);

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
	public SpeciesGroupPreference getSpeciesGroupPreference();

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

		private static final Logger logger = LoggerFactory.getLogger(Math77MessagesLevel.class);

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
	public Math77MessagesLevel getMath77MessagesLevel();

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

		private static final Logger logger = LoggerFactory.getLogger(UpperBoundsMode.class);

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
	public UpperBoundsMode getUpperBoundsMode();

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
	public static <T> T logDefault(Logger logger, String name, int value, T defaultValue) {
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
	public static <T> T logDefaultForUnknown(Logger logger, String name, int value, T defaultValue) {
		logger.atWarn().setMessage("Unknown {} of {}, using default of {}").addArgument(name).addArgument(value)
				.addArgument(defaultValue);
		return defaultValue;
	}
}
