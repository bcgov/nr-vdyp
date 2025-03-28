/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 * The version of the OpenAPI document: 1.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package ca.bc.gov.nrs.vdyp.backend.v1.gen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;

/**
 * the parameters defining specifics of the run
 */

@JsonPropertyOrder(
	{ Parameters.JSON_PROPERTY_OUTPUT_FORMAT, Parameters.JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS,
			Parameters.JSON_PROPERTY_SELECTED_DEBUG_OPTIONS, Parameters.JSON_PROPERTY_AGE_START,
			Parameters.JSON_PROPERTY_MIN_AGE_START, Parameters.JSON_PROPERTY_MAX_AGE_START,
			Parameters.JSON_PROPERTY_AGE_END, Parameters.JSON_PROPERTY_MIN_AGE_END,
			Parameters.JSON_PROPERTY_MAX_AGE_END, Parameters.JSON_PROPERTY_YEAR_START,
			Parameters.JSON_PROPERTY_YEAR_END, Parameters.JSON_PROPERTY_FORCE_YEAR,
			Parameters.JSON_PROPERTY_AGE_INCREMENT, Parameters.JSON_PROPERTY_MIN_AGE_INCREMENT,
			Parameters.JSON_PROPERTY_MAX_AGE_INCREMENT, Parameters.JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE,
			Parameters.JSON_PROPERTY_PROGRESS_FREQUENCY, Parameters.JSON_PROPERTY_METADATA_TO_OUTPUT,
			Parameters.JSON_PROPERTY_FILTERS, Parameters.JSON_PROPERTY_UTILS }
)
@RegisterForReflection
public class Parameters {
	/**
	 * Identifies the output file format. Default: YieldTable
	 */
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

		@JsonCreator
		public static OutputFormatEnum fromValue(String value) {
			for (OutputFormatEnum b : OutputFormatEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public static final String JSON_PROPERTY_OUTPUT_FORMAT = "outputFormat";
	@JsonProperty(JSON_PROPERTY_OUTPUT_FORMAT)
	private OutputFormatEnum outputFormat;

	/**
	 * Gets or Sets selectedExecutionOptions
	 */
	public enum SelectedExecutionOptionsEnum {
		BACK_GROW_ENABLED("backGrowEnabled"),

		FORWARD_GROW_ENABLED("forwardGrowEnabled"),

		DO_SAVE_INTERMEDIATE_FILES("doSaveIntermediateFiles"),

		DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES("doForceReferenceYearInclusionInYieldTables"),

		DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES("doForceCurrentYearInclusionInYieldTables"),

		DO_FORCE_CALENDAR_YEAR_INCLUSION_IN_YIELD_TABLES("doForceCalendarYearInclusionInYieldTables"),

		DO_INCLUDE_FILE_HEADER("doIncludeFileHeader"),

		DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE("doIncludeProjectionModeInYieldTable"),

		DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE("doIncludeAgeRowsInYieldTable"),

		DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE("doIncludeYearRowsInYieldTable"),

		DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE("doIncludePolygonRecordIdInYieldTable"),

		DO_SUMMARIZE_PROJECTION_BY_POLYGON("doSummarizeProjectionByPolygon"),

		DO_SUMMARIZE_PROJECTION_BY_LAYER("doSummarizeProjectionByLayer"),

		DO_INCLUDE_SPECIES_PROJECTION("doIncludeSpeciesProjection"),

		DO_INCLUDE_PROJECTED_MOF_VOLUMES("doIncludeProjectedMOFVolumes"),

		DO_INCLUDE_PROJECTED_MOF_BIOMASS("doIncludeProjectedMOFBiomass"),

		DO_INCLUDE_PROJECTED_CFS_BIOMASS("doIncludeProjectedCFSBiomass"),

		DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE("doIncludeColumnHeadersInYieldTable"),

		DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION(
				"doAllowBasalAreaAndTreesPerHectareValueSubstitution"
		),

		DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE(
				"doIncludeSecondarySpeciesDominantHeightInYieldTable"
		),

		DO_ENABLE_PROGRESS_LOGGING("doEnableProgressLogging"),

		DO_ENABLE_ERROR_LOGGING("doEnableErrorLogging"),

		DO_ENABLE_DEBUG_LOGGING("doEnableDebugLogging");

		private String value;

		SelectedExecutionOptionsEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static SelectedExecutionOptionsEnum fromValue(String value) {
			for (SelectedExecutionOptionsEnum b : SelectedExecutionOptionsEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public static final String JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS = "selectedExecutionOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	private List<SelectedExecutionOptionsEnum> selectedExecutionOptions = new ArrayList<>();

	/**
	 * Gets or Sets selectedDebugOptions
	 */
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

		@JsonCreator
		public static SelectedDebugOptionsEnum fromValue(String value) {
			for (SelectedDebugOptionsEnum b : SelectedDebugOptionsEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public static final String JSON_PROPERTY_SELECTED_DEBUG_OPTIONS = "selectedDebugOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	private List<SelectedDebugOptionsEnum> selectedDebugOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_AGE_START = "ageStart";
	@JsonProperty(JSON_PROPERTY_AGE_START)
	private Integer ageStart;

	public static final String JSON_PROPERTY_MIN_AGE_START = "minAgeStart";
	@JsonProperty(JSON_PROPERTY_MIN_AGE_START)
	private Integer minAgeStart;

	public static final String JSON_PROPERTY_MAX_AGE_START = "maxAgeStart";
	@JsonProperty(JSON_PROPERTY_MAX_AGE_START)
	private Integer maxAgeStart;

	public static final String JSON_PROPERTY_AGE_END = "ageEnd";
	@JsonProperty(JSON_PROPERTY_AGE_END)
	private Integer ageEnd;

	public static final String JSON_PROPERTY_MIN_AGE_END = "minAgeEnd";
	@JsonProperty(JSON_PROPERTY_MIN_AGE_END)
	private Integer minAgeEnd;

	public static final String JSON_PROPERTY_MAX_AGE_END = "maxAgeEnd";
	@JsonProperty(JSON_PROPERTY_MAX_AGE_END)
	private Integer maxAgeEnd;

	public static final String JSON_PROPERTY_YEAR_START = "yearStart";
	@JsonProperty(JSON_PROPERTY_YEAR_START)
	private Integer yearStart;

	public static final String JSON_PROPERTY_YEAR_END = "yearEnd";
	@JsonProperty(JSON_PROPERTY_YEAR_END)
	private Integer yearEnd;

	public static final String JSON_PROPERTY_FORCE_YEAR = "forceYear";
	@JsonProperty(JSON_PROPERTY_FORCE_YEAR)
	private Integer forceYear;

	public static final String JSON_PROPERTY_AGE_INCREMENT = "ageIncrement";
	@JsonProperty(JSON_PROPERTY_AGE_INCREMENT)
	private Integer ageIncrement;

	public static final String JSON_PROPERTY_MIN_AGE_INCREMENT = "minAgeIncrement";
	@JsonProperty(JSON_PROPERTY_MIN_AGE_INCREMENT)
	private Integer minAgeIncrement;

	public static final String JSON_PROPERTY_MAX_AGE_INCREMENT = "maxAgeIncrement";
	@JsonProperty(JSON_PROPERTY_MAX_AGE_INCREMENT)
	private Integer maxAgeIncrement;

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

		@JsonCreator
		public static CombineAgeYearRangeEnum fromValue(String value) {
			for (CombineAgeYearRangeEnum b : CombineAgeYearRangeEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public static final String JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE = "combineAgeYearRange";
	@JsonProperty(JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE)
	private CombineAgeYearRangeEnum combineAgeYearRange;

	public static final String JSON_PROPERTY_PROGRESS_FREQUENCY = "progressFrequency";
	@JsonProperty(JSON_PROPERTY_PROGRESS_FREQUENCY)
	private ProgressFrequency progressFrequency;

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 */
	public enum MetadataToOutputEnum {
		ALL("ALL"),

		MAIN("MAIN"),

		VERSION("VERSION"),

		MIN_IDENT("MIN_IDENT"),

		NONE("NONE");

		private String value;

		MetadataToOutputEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static MetadataToOutputEnum fromValue(String value) {
			for (MetadataToOutputEnum b : MetadataToOutputEnum.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException("Unexpected value '" + value + "'");
		}
	}

	public static final String JSON_PROPERTY_METADATA_TO_OUTPUT = "metadataToOutput";
	@JsonProperty(JSON_PROPERTY_METADATA_TO_OUTPUT)
	private MetadataToOutputEnum metadataToOutput;

	public static final String JSON_PROPERTY_FILTERS = "filters";
	@JsonProperty(JSON_PROPERTY_FILTERS)
	private Filters filters;

	public static final String JSON_PROPERTY_UTILS = "utils";
	@JsonProperty(JSON_PROPERTY_UTILS)
	private List<@Valid UtilizationParameter> utils = new ArrayList<>();

	public Parameters outputFormat(OutputFormatEnum outputFormat) {
		this.outputFormat = outputFormat;
		return this;
	}

	/**
	 * Identifies the output file format. Default: YieldTable
	 *
	 * @return outputFormat
	 **/
	@JsonProperty(value = "outputFormat")

	public OutputFormatEnum getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(OutputFormatEnum outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Parameters selectedExecutionOptions(List<SelectedExecutionOptionsEnum> selectedExecutionOptions) {
		this.selectedExecutionOptions = selectedExecutionOptions;
		return this;
	}

	public Parameters addSelectedExecutionOptionsItem(SelectedExecutionOptionsEnum selectedExecutionOptionsItem) {
		if (this.selectedExecutionOptions == null) {
			this.selectedExecutionOptions = new ArrayList<>();
		}
		this.selectedExecutionOptions.add(selectedExecutionOptionsItem);
		return this;
	}

	/**
	 * Get selectedExecutionOptions
	 *
	 * @return selectedExecutionOptions
	 **/
	@JsonProperty(value = "selectedExecutionOptions")

	public List<SelectedExecutionOptionsEnum> getSelectedExecutionOptions() {
		return selectedExecutionOptions;
	}

	public void setSelectedExecutionOptions(List<SelectedExecutionOptionsEnum> selectedExecutionOptions) {
		this.selectedExecutionOptions = selectedExecutionOptions;
	}

	public Parameters selectedDebugOptions(List<SelectedDebugOptionsEnum> selectedDebugOptions) {
		this.selectedDebugOptions = selectedDebugOptions;
		return this;
	}

	public Parameters addSelectedDebugOptionsItem(SelectedDebugOptionsEnum selectedDebugOptionsItem) {
		if (this.selectedDebugOptions == null) {
			this.selectedDebugOptions = new ArrayList<>();
		}
		this.selectedDebugOptions.add(selectedDebugOptionsItem);
		return this;
	}

	/**
	 * Get selectedDebugOptions
	 *
	 * @return selectedDebugOptions
	 **/
	@JsonProperty(value = "selectedDebugOptions")

	public List<SelectedDebugOptionsEnum> getSelectedDebugOptions() {
		return selectedDebugOptions;
	}

	public void setSelectedDebugOptions(List<SelectedDebugOptionsEnum> selectedDebugOptions) {
		this.selectedDebugOptions = selectedDebugOptions;
	}

	public Parameters ageStart(Integer ageStart) {
		this.ageStart = ageStart;
		return this;
	}

	/**
	 * The starting age value for the Age Range for generated yield tables.
	 *
	 * @return ageStart
	 **/
	@JsonProperty(value = "ageStart")

	public Integer getAgeStart() {
		return ageStart;
	}

	public void setAgeStart(Integer ageStart) {
		this.ageStart = ageStart;
	}

	public Parameters minAgeStart(Integer minAgeStart) {
		this.minAgeStart = minAgeStart;
		return this;
	}

	/**
	 * The minimum value ageStart may have.
	 *
	 * @return minAgeStart
	 **/
	@JsonProperty(value = "minAgeStart")

	public Integer getMinAgeStart() {
		return minAgeStart;
	}

	public void setMinAgeStart(Integer minAgeStart) {
		this.minAgeStart = minAgeStart;
	}

	public Parameters maxAgeStart(Integer maxAgeStart) {
		this.maxAgeStart = maxAgeStart;
		return this;
	}

	/**
	 * The maximum value ageStart may have.
	 *
	 * @return maxAgeStart
	 **/
	@JsonProperty(value = "maxAgeStart")

	public Integer getMaxAgeStart() {
		return maxAgeStart;
	}

	public void setMaxAgeStart(Integer maxAgeStart) {
		this.maxAgeStart = maxAgeStart;
	}

	public Parameters ageEnd(Integer ageEnd) {
		this.ageEnd = ageEnd;
		return this;
	}

	/**
	 * The ending age value for the Age Range for generated yield tables.
	 *
	 * @return ageEnd
	 **/
	@JsonProperty(value = "ageEnd")

	public Integer getAgeEnd() {
		return ageEnd;
	}

	public void setAgeEnd(Integer ageEnd) {
		this.ageEnd = ageEnd;
	}

	public Parameters minAgeEnd(Integer minAgeEnd) {
		this.minAgeEnd = minAgeEnd;
		return this;
	}

	/**
	 * The minimum value ageEnd may have.
	 *
	 * @return minAgeEnd
	 **/
	@JsonProperty(value = "minAgeEnd")

	public Integer getMinAgeEnd() {
		return minAgeEnd;
	}

	public void setMinAgeEnd(Integer minAgeEnd) {
		this.minAgeEnd = minAgeEnd;
	}

	public Parameters maxAgeEnd(Integer maxAgeEnd) {
		this.maxAgeEnd = maxAgeEnd;
		return this;
	}

	/**
	 * The maximum value ageEnd may have.
	 *
	 * @return maxAgeEnd
	 **/
	@JsonProperty(value = "maxAgeEnd")

	public Integer getMaxAgeEnd() {
		return maxAgeEnd;
	}

	public void setMaxAgeEnd(Integer maxAgeEnd) {
		this.maxAgeEnd = maxAgeEnd;
	}

	public Parameters yearStart(Integer yearStart) {
		this.yearStart = yearStart;
		return this;
	}

	/**
	 * The starting year for the Year Range for generated yield tables.
	 *
	 * @return yearStart
	 **/
	@JsonProperty(value = "yearStart")

	public Integer getYearStart() {
		return yearStart;
	}

	public void setYearStart(Integer yearStart) {
		this.yearStart = yearStart;
	}

	public Parameters yearEnd(Integer yearEnd) {
		this.yearEnd = yearEnd;
		return this;
	}

	/**
	 * The ending year for the Year Range for generated yield tables.
	 *
	 * @return yearEnd
	 **/
	@JsonProperty(value = "yearEnd")

	public Integer getYearEnd() {
		return yearEnd;
	}

	public void setYearEnd(Integer yearEnd) {
		this.yearEnd = yearEnd;
	}

	public Parameters forceYear(Integer forceYear) {
		this.forceYear = forceYear;
		return this;
	}

	/**
	 * Forces the inclusion of the specified calendar year in Yield tables.
	 *
	 * @return forceYear
	 **/
	@JsonProperty(value = "forceYear")

	public Integer getForceYear() {
		return forceYear;
	}

	public void setForceYear(Integer forceYear) {
		this.forceYear = forceYear;
	}

	public Parameters ageIncrement(Integer ageIncrement) {
		this.ageIncrement = ageIncrement;
		return this;
	}

	/**
	 * The number of years to increment the current value for the Age and Year ranges.
	 *
	 * @return ageIncrement
	 **/
	@JsonProperty(value = "ageIncrement")

	public Integer getAgeIncrement() {
		return ageIncrement;
	}

	public void setAgeIncrement(Integer ageIncrement) {
		this.ageIncrement = ageIncrement;
	}

	public Parameters minAgeIncrement(Integer minAgeIncrement) {
		this.minAgeIncrement = minAgeIncrement;
		return this;
	}

	/**
	 * The minimum value ageIncrement may have.
	 *
	 * @return minAgeIncrement
	 **/
	@JsonProperty(value = "minAgeIncrement")

	public Integer getMinAgeIncrement() {
		return minAgeIncrement;
	}

	public void setMinAgeIncrement(Integer minAgeIncrement) {
		this.minAgeIncrement = minAgeIncrement;
	}

	public Parameters maxAgeIncrement(Integer maxAgeIncrement) {
		this.maxAgeIncrement = maxAgeIncrement;
		return this;
	}

	/**
	 * The maximum value ageIncrement may have.
	 *
	 * @return maxAgeIncrement
	 **/
	@JsonProperty(value = "maxAgeIncrement")

	public Integer getMaxAgeIncrement() {
		return maxAgeIncrement;
	}

	public void setMaxAgeIncrement(Integer maxAgeIncrement) {
		this.maxAgeIncrement = maxAgeIncrement;
	}

	public Parameters combineAgeYearRange(CombineAgeYearRangeEnum combineAgeYearRange) {
		this.combineAgeYearRange = combineAgeYearRange;
		return this;
	}

	/**
	 * Determines how the Age Range and Year Range are to be combined when producing yield tables.
	 *
	 * @return combineAgeYearRange
	 **/
	@JsonProperty(value = "combineAgeYearRange")

	public CombineAgeYearRangeEnum getCombineAgeYearRange() {
		return combineAgeYearRange;
	}

	public void setCombineAgeYearRange(CombineAgeYearRangeEnum combineAgeYearRange) {
		this.combineAgeYearRange = combineAgeYearRange;
	}

	public Parameters progressFrequency(ProgressFrequency progressFrequency) {
		this.progressFrequency = progressFrequency;
		return this;
	}

	/**
	 * Get progressFrequency
	 *
	 * @return progressFrequency
	 **/
	@JsonProperty(value = "progressFrequency")
	@Valid
	public ProgressFrequency getProgressFrequency() {
		return progressFrequency;
	}

	public void setProgressFrequency(ProgressFrequency progressFrequency) {
		this.progressFrequency = progressFrequency;
	}

	public Parameters metadataToOutput(MetadataToOutputEnum metadataToOutput) {
		this.metadataToOutput = metadataToOutput;
		return this;
	}

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 *
	 * @return metadataToOutput
	 **/
	@JsonProperty(value = "metadataToOutput")

	public MetadataToOutputEnum getMetadataToOutput() {
		return metadataToOutput;
	}

	public void setMetadataToOutput(MetadataToOutputEnum metadataToOutput) {
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
	@JsonProperty(value = "filters")
	@Valid
	public Filters getFilters() {
		return filters;
	}

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public Parameters utils(List<@Valid UtilizationParameter> utils) {
		this.utils = utils;
		return this;
	}

	public Parameters addUtilsItem(UtilizationParameter utilsItem) {
		if (this.utils == null) {
			this.utils = new ArrayList<>();
		}
		this.utils.add(utilsItem);
		return this;
	}

	/**
	 * Get utils
	 *
	 * @return utils
	 **/
	@JsonProperty(value = "utils")
	@Valid
	public List<@Valid UtilizationParameter> getUtils() {
		return utils;
	}

	public void setUtils(List<@Valid UtilizationParameter> utils) {
		this.utils = utils;
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
				&& Objects.equals(this.minAgeStart, parameters.minAgeStart)
				&& Objects.equals(this.maxAgeStart, parameters.maxAgeStart)
				&& Objects.equals(this.ageEnd, parameters.ageEnd)
				&& Objects.equals(this.minAgeEnd, parameters.minAgeEnd)
				&& Objects.equals(this.maxAgeEnd, parameters.maxAgeEnd)
				&& Objects.equals(this.yearStart, parameters.yearStart)
				&& Objects.equals(this.yearEnd, parameters.yearEnd)
				&& Objects.equals(this.forceYear, parameters.forceYear)
				&& Objects.equals(this.ageIncrement, parameters.ageIncrement)
				&& Objects.equals(this.minAgeIncrement, parameters.minAgeIncrement)
				&& Objects.equals(this.maxAgeIncrement, parameters.maxAgeIncrement)
				&& Objects.equals(this.combineAgeYearRange, parameters.combineAgeYearRange)
				&& Objects.equals(this.progressFrequency, parameters.progressFrequency)
				&& Objects.equals(this.metadataToOutput, parameters.metadataToOutput)
				&& Objects.equals(this.filters, parameters.filters) && Objects.equals(this.utils, parameters.utils);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				outputFormat, selectedExecutionOptions, selectedDebugOptions, ageStart, minAgeStart, maxAgeStart,
				ageEnd, minAgeEnd, maxAgeEnd, yearStart, yearEnd, forceYear, ageIncrement, minAgeIncrement,
				maxAgeIncrement, combineAgeYearRange, progressFrequency, metadataToOutput, filters, utils
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Parameters {\n");

		sb.append("    outputFormat: ").append(toIndentedString(outputFormat)).append("\n");
		sb.append("    selectedExecutionOptions: ").append(toIndentedString(selectedExecutionOptions)).append("\n");
		sb.append("    selectedDebugOptions: ").append(toIndentedString(selectedDebugOptions)).append("\n");
		sb.append("    ageStart: ").append(toIndentedString(ageStart)).append("\n");
		sb.append("    minAgeStart: ").append(toIndentedString(minAgeStart)).append("\n");
		sb.append("    maxAgeStart: ").append(toIndentedString(maxAgeStart)).append("\n");
		sb.append("    ageEnd: ").append(toIndentedString(ageEnd)).append("\n");
		sb.append("    minAgeEnd: ").append(toIndentedString(minAgeEnd)).append("\n");
		sb.append("    maxAgeEnd: ").append(toIndentedString(maxAgeEnd)).append("\n");
		sb.append("    yearStart: ").append(toIndentedString(yearStart)).append("\n");
		sb.append("    yearEnd: ").append(toIndentedString(yearEnd)).append("\n");
		sb.append("    forceYear: ").append(toIndentedString(forceYear)).append("\n");
		sb.append("    ageIncrement: ").append(toIndentedString(ageIncrement)).append("\n");
		sb.append("    minAgeIncrement: ").append(toIndentedString(minAgeIncrement)).append("\n");
		sb.append("    maxAgeIncrement: ").append(toIndentedString(maxAgeIncrement)).append("\n");
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
