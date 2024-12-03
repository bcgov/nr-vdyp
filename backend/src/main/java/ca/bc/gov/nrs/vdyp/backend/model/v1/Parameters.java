/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 */

package ca.bc.gov.nrs.vdyp.backend.model.v1;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * the parameters defining specifics of the run
 */

@JsonPropertyOrder(
	{ Parameters.JSON_PROPERTY_OUTPUT_FORMAT, Parameters.JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS,
			Parameters.JSON_PROPERTY_SELECTED_DEBUG_OPTIONS, Parameters.JSON_PROPERTY_AGE_START,
			Parameters.JSON_PROPERTY_AGE_END, Parameters.JSON_PROPERTY_YEAR_START, Parameters.JSON_PROPERTY_YEAR_END,
			Parameters.JSON_PROPERTY_FORCE_YEAR, Parameters.JSON_PROPERTY_AGE_INCREMENT,
			Parameters.JSON_PROPERTY_METADATA_TO_OUTPUT, Parameters.JSON_PROPERTY_FILTERS,
			Parameters.JSON_PROPERTY_UTILS }
)
@RegisterForReflection
public class Parameters {

	public static final int LEGACY_NULL_VALUE = -9;

	public static final String JSON_PROPERTY_OUTPUT_FORMAT = "outputFormat";
	@JsonProperty(JSON_PROPERTY_OUTPUT_FORMAT)
	private String outputFormat;

	public static final String JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS = "selectedExecutionOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	private List<String> selectedExecutionOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_SELECTED_DEBUG_OPTIONS = "selectedDebugOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	private List<String> selectedDebugOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_AGE_START = "ageStart";
	@JsonProperty(JSON_PROPERTY_AGE_START)
	private String ageStart;

	public static final String JSON_PROPERTY_AGE_END = "ageEnd";
	@JsonProperty(JSON_PROPERTY_AGE_END)
	private String ageEnd;

	public static final String JSON_PROPERTY_YEAR_START = "yearStart";
	@JsonProperty(JSON_PROPERTY_YEAR_START)
	private String yearStart;

	public static final String JSON_PROPERTY_YEAR_END = "yearEnd";
	@JsonProperty(JSON_PROPERTY_YEAR_END)
	private String yearEnd;

	public static final String JSON_PROPERTY_FORCE_YEAR = "forceYear";
	@JsonProperty(JSON_PROPERTY_FORCE_YEAR)
	private String forceYear;

	public static final String JSON_PROPERTY_AGE_INCREMENT = "ageIncrement";
	@JsonProperty(JSON_PROPERTY_AGE_INCREMENT)
	private String ageIncrement;

	public static final String JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE = "combineAgeYearRange";
	@JsonProperty(JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE)
	private String combineAgeYearRange;

	public static final String JSON_PROPERTY_PROGRESS_FREQUENCY = "progressFrequency";
	@JsonProperty(JSON_PROPERTY_PROGRESS_FREQUENCY)
	private String progressFrequency;

	public static final String JSON_PROPERTY_METADATA_TO_OUTPUT = "metadataToOutput";
	@JsonProperty(JSON_PROPERTY_METADATA_TO_OUTPUT)
	private String metadataToOutput;

	public static final String JSON_PROPERTY_FILTERS = "filters";
	@JsonProperty(JSON_PROPERTY_FILTERS)
	// protected because this parameter requires no validation and therefore isn't in ValidatedParameters.
	// This is the only such parameter
	protected Filters filters;

	public static final String JSON_PROPERTY_UTILS = "utils";
	@JsonProperty(JSON_PROPERTY_UTILS)
	private List<UtilizationParameter> utils = new ArrayList<>();

	public enum OutputFormatEnum {
		YIELD_TABLE("YieldTable"),

		CSV_YIELD_TABLE("CSVYieldTable"),

		DCSV("DCSV");

		private String value;

		OutputFormatEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the  corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static OutputFormatEnum fromValue(String value) {
			for (OutputFormatEnum b : OutputFormatEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(MessageFormat.format("\"{}\" is not a valid OutputFormat value", value));
		}
	}

	public enum SelectedExecutionOptionsEnum {

		BACK_GROW_ENABLED("backGrowEnabled"), //
		FORWARD_GROW_ENABLED("forwardGrowEnabled"), //
		DO_SAVE_INTERMEDIATE_FILES("doSaveIntermediateFiles"), //
		DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES("doForceReferenceYearInclusionInYieldTables"), //
		DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES("doForceCurrentYearInclusionInYieldTables"), //
		DO_FORCE_CALENDAR_YEAR_INCLUSION_IN_YIELD_TABLES("doForceCalendarYearInclusionInYieldTables"), //
		DO_INCLUDE_FILE_HEADER("doIncludeFileHeader"), //
		DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE("doIncludeProjectionModeInYieldTable"), //
		DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE("doIncludeAgeRowsInYieldTable"), //
		DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE("doIncludeYearRowsInYieldTable"), //
		DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE("doIncludePolygonRecordIdInYieldTable"), //
		DO_SUMMARIZE_PROJECTION_BY_POLYGON("doSummarizeProjectionByPolygon"), //
		DO_SUMMARIZE_PROJECTION_BY_LAYER("doSummarizeProjectionByLayer"), //
		DO_INCLUDE_SPECIES_PROJECTION("doIncludeSpeciesProjection"), //
		DO_INCLUDE_PROJECTED_MOF_VOLUMES("doIncludeProjectedMOFVolumes"), //
		DO_INCLUDE_PROJECTED_MOF_BIOMASS("doIncludeProjectedMOFBiomass"), //
		DO_INCLUDE_PROJECTED_CFS_BIOMASS("doIncludeProjectedCFSBiomass"), //
		DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE("doIncludeColumnHeadersInYieldTable"), //
		DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION(
				"doAllowBasalAreaAndTreesPerHectareValueSubstitution"
		), //
		DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE(
				"doIncludeSecondarySpeciesDominantHeightInYieldTable"
		), //
		DO_ENABLE_PROGRESS_LOGGING("doEnableProgressLogging"), //
		DO_ENABLE_ERROR_LOGGING("doEnableErrorLogging"), //
		DO_ENABLE_DEBUG_LOGGING("doEnableDebugLogging");

		private String value;

		SelectedExecutionOptionsEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the  corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static SelectedExecutionOptionsEnum fromValue(String value) {
			for (SelectedExecutionOptionsEnum b : SelectedExecutionOptionsEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{}\" is not a valid SelectedExecutionOptions value", value)
			);
		}
	}

	/**
	 * Determines how the Age Range and Year Range are to be combined when producing yield tables.
	 */
	public enum CombineAgeYearRangeEnum {
		UNION("union"),

		INTERSECT("intersect"),

		DIFFERENCE("difference");

		private String value;

		CombineAgeYearRangeEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the  corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static CombineAgeYearRangeEnum fromValue(String value) {
			for (CombineAgeYearRangeEnum b : CombineAgeYearRangeEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{}\" is not a valid CombineAgeYearRange value", value)
			);
		}
	}

	public enum SelectedDebugOptionsEnum {
		DO_INCLUDE_DEBUG_TIMESTAMPS("doIncludeDebugTimestamps"),
		DO_INCLUDE_DEBUG_ROUTINE_NAMES("doIncludeDebugRoutineNames"),
		DO_INCLUDE_DEBUG_ENTRY_EXIT("doIncludeDebugEntryExit"),
		DO_INCLUDE_DEBUG_INDENT_BLOCKS("doIncludeDebugIndentBlocks");

		private String value;

		SelectedDebugOptionsEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the  corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static SelectedDebugOptionsEnum fromValue(String value) {
			for (SelectedDebugOptionsEnum b : SelectedDebugOptionsEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{}\" is not a valid SelectedDebugOptions value", value)
			);
		}
	}

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 */
	public enum MetadataToOutputEnum {
		ALL("ALL"), MAIN("MAIN"), VERSION("VERSION"), MIN_IDENT("MIN_IDENT"), NONE("NONE");

		private String value;

		MetadataToOutputEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the  corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static MetadataToOutputEnum fromValue(String value) {
			for (MetadataToOutputEnum b : MetadataToOutputEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{}\" is not a valid MetadataToOutput value", value)
			);
		}
	}

	public Parameters outputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
		return this;
	}

	/**
	 * Identifies the output file format. Default: YieldTable
	 *
	 * @return outputFormat
	 **/
	@JsonProperty(value = JSON_PROPERTY_OUTPUT_FORMAT)
	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Parameters selectedExecutionOptions(List<String> selectedExecutionOptions) {
		setSelectedExecutionOptions(selectedExecutionOptions);
		return this;
	}

	public Parameters addSelectedExecutionOptionsItem(SelectedExecutionOptionsEnum selectedExecutionOptionsItem) {
		this.selectedExecutionOptions.add(selectedExecutionOptionsItem.name());
		return this;
	}

	/**
	 * Get selectedExecutionOptions
	 *
	 * @return selectedExecutionOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	public List<String> getSelectedExecutionOptions() {
		assert selectedExecutionOptions != null;
		return selectedExecutionOptions;
	}

	public void setSelectedExecutionOptions(List<String> selectedExecutionOptions) {
		if (selectedExecutionOptions == null) {
			this.selectedExecutionOptions.clear();
		} else {
			this.selectedExecutionOptions = selectedExecutionOptions;
		}
	}

	public Parameters selectedDebugOptions(List<String> selectedDebugOptions) {
		setSelectedDebugOptions(selectedDebugOptions);
		return this;
	}

	public Parameters addSelectedDebugOptionsItem(SelectedDebugOptionsEnum selectedDebugOptionsItem) {
		this.selectedDebugOptions.add(selectedDebugOptionsItem.name());
		return this;
	}

	/**
	 * Get selectedDebugOptions
	 *
	 * @return selectedDebugOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	public List<String> getSelectedDebugOptions() {
		return selectedDebugOptions;
	}

	public void setSelectedDebugOptions(List<String> selectedDebugOptions) {
		if (selectedDebugOptions == null) {
			this.selectedDebugOptions.clear();
		} else {
			this.selectedDebugOptions = selectedDebugOptions;
		}
	}

	public Parameters ageStart(String ageStart) {
		this.ageStart = ageStart;
		return this;
	}

	/**
	 * The starting age value for the Age Range for generated yield tables.
	 *
	 * @return ageStart
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_START)
	public String getAgeStart() {
		return ageStart;
	}

	public void setAgeStart(String ageStart) {
		this.ageStart = ageStart;
	}

	public Parameters ageEnd(String ageEnd) {
		this.ageEnd = ageEnd;
		return this;
	}

	/**
	 * The ending age value for the Age Range for generated yield tables.
	 *
	 * @return ageEnd
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_END)
	public String getAgeEnd() {
		return ageEnd;
	}

	public void setAgeEnd(String ageEnd) {
		this.ageEnd = ageEnd;
	}

	public Parameters yearStart(String yearStart) {
		this.yearStart = yearStart;
		return this;
	}

	/**
	 * The starting year for the Year Range for generated yield tables.
	 *
	 * @return yearStart
	 **/
	@JsonProperty(value = JSON_PROPERTY_YEAR_START)
	public String getYearStart() {
		return yearStart;
	}

	public void setYearStart(String yearStart) {
		this.yearStart = yearStart;
	}

	public Parameters yearEnd(String yearEnd) {
		this.yearEnd = yearEnd;
		return this;
	}

	/**
	 * The ending year for the Year Range for generated yield tables.
	 *
	 * @return yearEnd
	 **/
	@JsonProperty(value = JSON_PROPERTY_YEAR_END)
	public String getYearEnd() {
		return yearEnd;
	}

	public void setYearEnd(String yearEnd) {
		this.yearEnd = yearEnd;
	}

	public Parameters forceYear(String forceYear) {
		this.forceYear = forceYear;
		return this;
	}

	/**
	 * Forces the inclusion of the specified calendar year in Yield tables.
	 *
	 * @return forceYear
	 **/
	@JsonProperty(value = JSON_PROPERTY_FORCE_YEAR)
	public String getForceYear() {
		return forceYear;
	}

	public void setForceYear(String forceYear) {
		this.forceYear = forceYear;
	}

	public Parameters ageIncrement(String ageIncrement) {
		this.ageIncrement = ageIncrement;
		return this;
	}

	/**
	 * The number of years to increment the current value for the Age and Year ranges.
	 *
	 * @return ageIncrement
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_INCREMENT)
	public String getAgeIncrement() {
		return ageIncrement;
	}

	public void setAgeIncrement(String ageIncrement) {
		this.ageIncrement = ageIncrement;
	}

	public Parameters combineAgeYearRange(String combineAgeYearRange) {
		this.combineAgeYearRange = combineAgeYearRange;
		return this;
	}

	/**
	 * Determines how the Age Range and Year Range are to be combined when producing yield tables.
	 *
	 * @return combineAgeYearRange
	 **/
	@JsonProperty(value = JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE)
	public String getCombineAgeYearRange() {
		return combineAgeYearRange;
	}

	public void setCombineAgeYearRange(String combineAgeYearRange) {
		this.combineAgeYearRange = combineAgeYearRange;
	}

	public Parameters progressFrequency(String progressFrequency) {
		this.progressFrequency = progressFrequency;
		return this;
	}

	/**
	 * Get progressFrequency
	 *
	 * @return progressFrequency
	 **/
	@JsonProperty(value = JSON_PROPERTY_PROGRESS_FREQUENCY)
	public String getProgressFrequency() {
		return progressFrequency;
	}

	public void setProgressFrequency(String progressFrequency) {
		this.progressFrequency = progressFrequency;
	}

	public Parameters metadataToOutput(String metadataToOutput) {
		this.metadataToOutput = metadataToOutput;
		return this;
	}

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 *
	 * @return metadataToOutput
	 **/
	@JsonProperty(value = JSON_PROPERTY_METADATA_TO_OUTPUT)
	public String getMetadataToOutput() {
		return metadataToOutput;
	}

	public void setMetadataToOutput(String metadataToOutput) {
		this.metadataToOutput = metadataToOutput;
	}

	public Parameters filters(Filters filters) {
		this.filters = filters;
		return this;
	}

	/**
	 * Get filters
	 *
	 * @return filters
	 **/
	@JsonProperty(value = JSON_PROPERTY_FILTERS)
	public Filters getFilters() {
		return filters;
	}

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public Parameters utils(List<UtilizationParameter> utils) {
		setUtils(utils);
		return this;
	}

	public Parameters addUtilsItem(ValidatedUtilizationParameter utilsItem) {
		this.utils.add(
				new UtilizationParameter().speciesName(utilsItem.getSpeciesName())
						.value(utilsItem.getValue().name())
		);
		return this;
	}

	/**
	 * Get utils
	 *
	 * @return utils
	 **/
	@JsonProperty(value = JSON_PROPERTY_UTILS)
	public List<UtilizationParameter> getUtils() {
		return utils;
	}

	public void setUtils(List<UtilizationParameter> utils) {
		if (utils == null) {
			this.utils.clear();
		} else {
			this.utils = utils;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Parameters parameters = (Parameters) o;
		return Objects.equals(this.outputFormat, parameters.outputFormat)
				&& Objects.equals(this.selectedExecutionOptions, parameters.selectedExecutionOptions)
				&& Objects.equals(this.selectedDebugOptions, parameters.selectedDebugOptions)
				&& Objects.equals(this.ageStart, parameters.ageStart)
				&& Objects.equals(this.ageEnd, parameters.ageEnd)
				&& Objects.equals(this.yearStart, parameters.yearStart)
				&& Objects.equals(this.yearEnd, parameters.yearEnd)
				&& Objects.equals(this.forceYear, parameters.forceYear)
				&& Objects.equals(this.ageIncrement, parameters.ageIncrement)
				&& Objects.equals(this.combineAgeYearRange, parameters.combineAgeYearRange)
				&& Objects.equals(this.progressFrequency, parameters.progressFrequency)
				&& Objects.equals(this.metadataToOutput, parameters.metadataToOutput)
				&& Objects.equals(this.filters, parameters.filters)
				&& Objects.equals(this.utils, parameters.utils);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				outputFormat, selectedExecutionOptions, selectedDebugOptions, ageStart, ageEnd,
				yearStart, yearEnd, forceYear, ageIncrement, combineAgeYearRange,
				progressFrequency, metadataToOutput, filters, utils
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Parameters {\n");

		sb.append("    outputFormat: ").append(toIndentedString(outputFormat)).append("\n");
		sb.append("    selectedExecutionOptions: ").append(toIndentedString(selectedExecutionOptions))
				.append("\n");
		sb.append("    selectedDebugOptions: ").append(toIndentedString(selectedDebugOptions)).append("\n");
		sb.append("    ageStart: ").append(toIndentedString(ageStart)).append("\n");
		sb.append("    ageEnd: ").append(toIndentedString(ageEnd)).append("\n");
		sb.append("    yearStart: ").append(toIndentedString(yearStart)).append("\n");
		sb.append("    yearEnd: ").append(toIndentedString(yearEnd)).append("\n");
		sb.append("    forceYear: ").append(toIndentedString(forceYear)).append("\n");
		sb.append("    ageIncrement: ").append(toIndentedString(ageIncrement)).append("\n");
		sb.append("    combineAgeYearRange: ").append(toIndentedString(combineAgeYearRange)).append("\n");
		sb.append("    progressFrequency: ").append(toIndentedString(progressFrequency)).append("\n");
		sb.append("    metadataToOutput: ").append(toIndentedString(metadataToOutput)).append("\n");
		sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
		sb.append("    utils: ").append(toIndentedString(utils)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces (except the first line).
	 */
	private String toIndentedString(Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
