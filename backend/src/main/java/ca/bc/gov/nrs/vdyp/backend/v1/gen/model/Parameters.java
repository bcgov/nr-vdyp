/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 */

package ca.bc.gov.nrs.vdyp.backend.v1.gen.model;

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
	private String outputFormatText;

	public static final String JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS = "selectedExecutionOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	private List<String> selectedExecutionOptionsText = new ArrayList<>();

	public static final String JSON_PROPERTY_SELECTED_DEBUG_OPTIONS = "selectedDebugOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	private List<String> selectedDebugOptionsText = new ArrayList<>();

	public static final String JSON_PROPERTY_AGE_START = "ageStart";
	@JsonProperty(JSON_PROPERTY_AGE_START)
	private String ageStartText;

	public static final String JSON_PROPERTY_AGE_END = "ageEnd";
	@JsonProperty(JSON_PROPERTY_AGE_END)
	private String ageEndText;

	public static final String JSON_PROPERTY_YEAR_START = "yearStart";
	@JsonProperty(JSON_PROPERTY_YEAR_START)
	private String yearStartText;

	public static final String JSON_PROPERTY_YEAR_END = "yearEnd";
	@JsonProperty(JSON_PROPERTY_YEAR_END)
	private String yearEndText;

	public static final String JSON_PROPERTY_FORCE_YEAR = "forceYear";
	@JsonProperty(JSON_PROPERTY_FORCE_YEAR)
	private String forceYearText;

	public static final String JSON_PROPERTY_AGE_INCREMENT = "ageIncrement";
	@JsonProperty(JSON_PROPERTY_AGE_INCREMENT)
	private String ageIncrementText;

	public static final String JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE = "combineAgeYearRange";
	@JsonProperty(JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE)
	private String combineAgeYearRangeText;

	public static final String JSON_PROPERTY_PROGRESS_FREQUENCY = "progressFrequency";
	@JsonProperty(JSON_PROPERTY_PROGRESS_FREQUENCY)
	private String progressFrequencyText;

	public static final String JSON_PROPERTY_METADATA_TO_OUTPUT = "metadataToOutput";
	@JsonProperty(JSON_PROPERTY_METADATA_TO_OUTPUT)
	private String metadataToOutputText;

	public static final String JSON_PROPERTY_FILTERS = "filters";
	@JsonProperty(JSON_PROPERTY_FILTERS)
	// protected because this parameter requires no validation and therefore isn't in ValidatedParameters.
	// This is the only such parameter
	protected Filters filters;

	public static final String JSON_PROPERTY_UTILS = "utils";
	@JsonProperty(JSON_PROPERTY_UTILS)
	private List<UtilizationParameterText> utilsText = new ArrayList<>();

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
		 * @param value the text corresponding to a value of this enumeration
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
		 * @param value the text corresponding to a value of this enumeration
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
		 * @param value the text corresponding to a value of this enumeration
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
		 * @param value the text corresponding to a value of this enumeration
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
		 * @param value the text corresponding to a value of this enumeration
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

	public Parameters outputFormatText(String outputFormatText) {
		this.outputFormatText = outputFormatText;
		return this;
	}

	/**
	 * Identifies the output file format. Default: YieldTable
	 *
	 * @return outputFormatText
	 **/
	@JsonProperty(value = JSON_PROPERTY_OUTPUT_FORMAT)
	public String getOutputFormatText() {
		return outputFormatText;
	}

	public void setOutputFormatText(String outputFormatText) {
		this.outputFormatText = outputFormatText;
	}

	public Parameters selectedExecutionOptionsText(List<String> selectedExecutionOptionsText) {
		setSelectedExecutionOptionsText(selectedExecutionOptionsText);
		return this;
	}

	public Parameters addSelectedExecutionOptionsTextItem(String selectedExecutionOptionsTextItem) {
		this.selectedExecutionOptionsText.add(selectedExecutionOptionsTextItem);
		return this;
	}

	/**
	 * Get selectedExecutionOptionsText
	 *
	 * @return selectedExecutionOptionsText
	 **/
	@JsonProperty(value = JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	public List<String> getSelectedExecutionOptionsText() {
		assert selectedExecutionOptionsText != null;
		return selectedExecutionOptionsText;
	}

	public void setSelectedExecutionOptionsText(List<String> selectedExecutionOptionsText) {
		if (selectedExecutionOptionsText == null) {
			this.selectedExecutionOptionsText.clear();
		} else {
			this.selectedExecutionOptionsText = selectedExecutionOptionsText;
		}
	}

	public Parameters selectedDebugOptionsText(List<String> selectedDebugOptionsText) {
		setSelectedDebugOptionsText(selectedDebugOptionsText);
		return this;
	}

	public Parameters addSelectedDebugOptionsItemText(String selectedDebugOptionsItemText) {
		this.selectedDebugOptionsText.add(selectedDebugOptionsItemText);
		return this;
	}

	/**
	 * Get selectedDebugOptions
	 *
	 * @return selectedDebugOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	public List<String> getSelectedDebugOptionsText() {
		return selectedDebugOptionsText;
	}

	public void setSelectedDebugOptionsText(List<String> selectedDebugOptionsText) {
		if (selectedDebugOptionsText == null) {
			this.selectedDebugOptionsText.clear();
		} else {
			this.selectedDebugOptionsText = selectedDebugOptionsText;
		}
	}

	public Parameters ageStartText(String ageStartText) {
		this.ageStartText = ageStartText;
		return this;
	}

	/**
	 * The starting age value for the Age Range for generated yield tables.
	 *
	 * @return ageStart
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_START)
	public String getAgeStartText() {
		return ageStartText;
	}

	public void setAgeStartText(String ageStartText) {
		this.ageStartText = ageStartText;
	}

	public Parameters ageEndText(String ageEndText) {
		this.ageEndText = ageEndText;
		return this;
	}

	/**
	 * The ending age value for the Age Range for generated yield tables.
	 *
	 * @return ageEnd
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_END)
	public String getAgeEndText() {
		return ageEndText;
	}

	public void setAgeEndText(String ageEndText) {
		this.ageEndText = ageEndText;
	}

	public Parameters yearStartText(String yearStartText) {
		this.yearStartText = yearStartText;
		return this;
	}

	/**
	 * The starting year for the Year Range for generated yield tables.
	 *
	 * @return yearStart
	 **/
	@JsonProperty(value = JSON_PROPERTY_YEAR_START)
	public String getYearStartText() {
		return yearStartText;
	}

	public void setYearStartText(String yearStartText) {
		this.yearStartText = yearStartText;
	}

	public Parameters yearEndText(String yearEndText) {
		this.yearEndText = yearEndText;
		return this;
	}

	/**
	 * The ending year for the Year Range for generated yield tables.
	 *
	 * @return yearEnd
	 **/
	@JsonProperty(value = JSON_PROPERTY_YEAR_END)
	public String getYearEndText() {
		return yearEndText;
	}

	public void setYearEndText(String yearEndText) {
		this.yearEndText = yearEndText;
	}

	public Parameters forceYearText(String forceYearText) {
		this.forceYearText = forceYearText;
		return this;
	}

	/**
	 * Forces the inclusion of the specified calendar year in Yield tables.
	 *
	 * @return forceYear
	 **/
	@JsonProperty(value = JSON_PROPERTY_FORCE_YEAR)
	public String getForceYearText() {
		return forceYearText;
	}

	public void setForceYearText(String forceYearText) {
		this.forceYearText = forceYearText;
	}

	public Parameters ageIncrementText(String ageIncrementText) {
		this.ageIncrementText = ageIncrementText;
		return this;
	}

	/**
	 * The number of years to increment the current value for the Age and Year ranges.
	 *
	 * @return ageIncrement
	 **/
	@JsonProperty(value = JSON_PROPERTY_AGE_INCREMENT)
	public String getAgeIncrementText() {
		return ageIncrementText;
	}

	public void setAgeIncrement(String ageIncrementText) {
		this.ageIncrementText = ageIncrementText;
	}

	public Parameters combineAgeYearRangeText(String combineAgeYearRangeText) {
		this.combineAgeYearRangeText = combineAgeYearRangeText;
		return this;
	}

	/**
	 * Determines how the Age Range and Year Range are to be combined when producing yield tables.
	 *
	 * @return combineAgeYearRange
	 **/
	@JsonProperty(value = JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE)
	public String getCombineAgeYearRangeText() {
		return combineAgeYearRangeText;
	}

	public void setCombineAgeYearRangeText(String combineAgeYearRangeText) {
		this.combineAgeYearRangeText = combineAgeYearRangeText;
	}

	public Parameters progressFrequencyText(String progressFrequencyText) {
		this.progressFrequencyText = progressFrequencyText;
		return this;
	}

	/**
	 * Get progressFrequency
	 *
	 * @return progressFrequency
	 **/
	@JsonProperty(value = JSON_PROPERTY_PROGRESS_FREQUENCY)
	public String getProgressFrequencyText() {
		return progressFrequencyText;
	}

	public void setProgressFrequencyText(String progressFrequencyText) {
		this.progressFrequencyText = progressFrequencyText;
	}

	public Parameters metadataToOutputText(String metadataToOutputText) {
		this.metadataToOutputText = metadataToOutputText;
		return this;
	}

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 *
	 * @return metadataToOutput
	 **/
	@JsonProperty(value = JSON_PROPERTY_METADATA_TO_OUTPUT)
	public String getMetadataToOutputText() {
		return metadataToOutputText;
	}

	public void setMetadataToOutputText(String metadataToOutputText) {
		this.metadataToOutputText = metadataToOutputText;
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

	public Parameters utilsText(List<UtilizationParameterText> utilsText) {
		setUtilsText(utilsText);
		return this;
	}

	public Parameters addUtilsItemText(UtilizationParameterText utilsItemText) {
		this.utilsText.add(utilsItemText);
		return this;
	}

	/**
	 * Get utils
	 *
	 * @return utils
	 **/
	@JsonProperty(value = JSON_PROPERTY_UTILS)
	public List<UtilizationParameterText> getUtilsText() {
		return utilsText;
	}

	public void setUtilsText(List<UtilizationParameterText> utilsText) {
		if (utilsText == null) {
			this.utilsText.clear();
		} else {
			this.utilsText = utilsText;
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
		return Objects.equals(this.outputFormatText, parameters.outputFormatText)
				&& Objects.equals(this.selectedExecutionOptionsText, parameters.selectedExecutionOptionsText)
				&& Objects.equals(this.selectedDebugOptionsText, parameters.selectedDebugOptionsText)
				&& Objects.equals(this.ageStartText, parameters.ageStartText)
				&& Objects.equals(this.ageEndText, parameters.ageEndText)
				&& Objects.equals(this.yearStartText, parameters.yearStartText)
				&& Objects.equals(this.yearEndText, parameters.yearEndText)
				&& Objects.equals(this.forceYearText, parameters.forceYearText)
				&& Objects.equals(this.ageIncrementText, parameters.ageIncrementText)
				&& Objects.equals(this.combineAgeYearRangeText, parameters.combineAgeYearRangeText)
				&& Objects.equals(this.progressFrequencyText, parameters.progressFrequencyText)
				&& Objects.equals(this.metadataToOutputText, parameters.metadataToOutputText)
				&& Objects.equals(this.filters, parameters.filters)
				&& Objects.equals(this.utilsText, parameters.utilsText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				outputFormatText, selectedExecutionOptionsText, selectedDebugOptionsText, ageStartText, ageEndText,
				yearStartText, yearEndText, forceYearText, ageIncrementText, combineAgeYearRangeText,
				progressFrequencyText, metadataToOutputText, filters, utilsText
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Parameters {\n");

		sb.append("    outputFormatText: ").append(toIndentedString(outputFormatText)).append("\n");
		sb.append("    selectedExecutionOptionsText: ").append(toIndentedString(selectedExecutionOptionsText))
				.append("\n");
		sb.append("    selectedDebugOptions: ").append(toIndentedString(selectedDebugOptionsText)).append("\n");
		sb.append("    ageStart: ").append(toIndentedString(ageStartText)).append("\n");
		sb.append("    ageEnd: ").append(toIndentedString(ageEndText)).append("\n");
		sb.append("    yearStart: ").append(toIndentedString(yearStartText)).append("\n");
		sb.append("    yearEnd: ").append(toIndentedString(yearEndText)).append("\n");
		sb.append("    forceYear: ").append(toIndentedString(forceYearText)).append("\n");
		sb.append("    ageIncrement: ").append(toIndentedString(ageIncrementText)).append("\n");
		sb.append("    combineAgeYearRange: ").append(toIndentedString(combineAgeYearRangeText)).append("\n");
		sb.append("    progressFrequency: ").append(toIndentedString(progressFrequencyText)).append("\n");
		sb.append("    metadataToOutput: ").append(toIndentedString(metadataToOutputText)).append("\n");
		sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
		sb.append("    utils: ").append(toIndentedString(utilsText)).append("\n");
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
