/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 */

package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

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
public class Parameters {

	public static final String JSON_PROPERTY_OUTPUT_FORMAT = "outputFormat";
	@JsonProperty(JSON_PROPERTY_OUTPUT_FORMAT)
	private String outputFormat;

	public static final String JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS = "selectedExecutionOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	private List<String> selectedExecutionOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_EXCLUDED_EXECUTION_OPTIONS = "excludedExecutionOptions";
	@JsonProperty(JSON_PROPERTY_EXCLUDED_EXECUTION_OPTIONS)
	private List<String> excludedExecutionOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_SELECTED_DEBUG_OPTIONS = "selectedDebugOptions";
	@JsonProperty(JSON_PROPERTY_SELECTED_DEBUG_OPTIONS)
	private List<String> selectedDebugOptions = new ArrayList<>();

	public static final String JSON_PROPERTY_EXCLUDED_DEBUG_OPTIONS = "excludedDebugOptions";
	@JsonProperty(JSON_PROPERTY_EXCLUDED_DEBUG_OPTIONS)
	private List<String> excludedDebugOptions = new ArrayList<>();

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
	private String yearForcedIntoYieldTable;

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

	public static final String JSON_PROPERTY_REPORT_TITLE = "reportTitle";
	@JsonProperty(JSON_PROPERTY_REPORT_TITLE)
	private String reportTitle;

	public static final String JSON_PROPERTY_FILTERS = "filters";
	@JsonProperty(JSON_PROPERTY_FILTERS)
	public FilterParameters filters;

	public static final String JSON_PROPERTY_UTILS = "utils";
	@JsonProperty(JSON_PROPERTY_UTILS)
	private List<UtilizationParameter> utils = new ArrayList<>();

	public enum OutputFormat {
		TEXT_REPORT("TextReport", "YieldReport.txt"),

		YIELD_TABLE("YieldTable", "YieldTable.txt"),

		CSV_YIELD_TABLE("CSVYieldTable", "YieldTable.csv"),

		DCSV("DCSV", "YieldTable.csv"),

		PLOTSY("PLOTSY", "YieldTable.csv");

		private String value;
		private String yieldTableFileName;

		OutputFormat(String value, String yieldTableFileName) {
			this.value = value;
			this.yieldTableFileName = yieldTableFileName;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		@JsonValue
		public String getYieldTableFileName() {
			return yieldTableFileName;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static OutputFormat fromValue(String value) {
			for (OutputFormat b : OutputFormat.values()) {
				if (b.value.equalsIgnoreCase(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{0}\" is not a valid OutputFormat value", value)
			);
		}
	}

	public enum ExecutionOption {

		BACK_GROW_ENABLED("backGrowEnabled"), //
		FORWARD_GROW_ENABLED("forwardGrowEnabled"), //
		DO_SAVE_INTERMEDIATE_FILES("doSaveIntermediateFiles"), //
		DO_INCLUDE_PROJECTION_FILES("doIncludeProjectionFiles"), //
		DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES("doForceReferenceYearInclusionInYieldTables"), //
		DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES("doForceCurrentYearInclusionInYieldTables"), //
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
		DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION("doAllowBasalAreaAndTreesPerHectareValueSubstitution"), //
		DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE(
				"doIncludeSecondarySpeciesDominantHeightInYieldTable"
		), //
		DO_ENABLE_PROGRESS_LOGGING("doEnableProgressLogging"), //
		DO_ENABLE_ERROR_LOGGING("doEnableErrorLogging"), //
		DO_ENABLE_DEBUG_LOGGING("doEnableDebugLogging"), //
		DO_DELAY_EXECUTION_FOLDER_DELETION("doDelayExecutionFolderDeletion"), //
		ALLOW_AGGRESSIVE_VALUE_ESTIMATION("allowAggressiveValueEstimation"), //
		REPORT_INCLUDE_WHOLE_STEM_VOLUME("reportIncludeWholeStemVolume"), //
		REPORT_INCLUDE_CLOSE_UTILIZATION_VOLUME("reportIncludeCloseUtilizationVolume"), //
		REPORT_INCLUDE_NET_DECAY_VOLUME("reportIncludeNetDecayVolume"), //
		REPORT_INCLUDE_ND_WASTE_VOLUME("reportIncludeNDWasteVolume"), //
		REPORT_INCLUDE_ND_WAST_BRKG_VOLUME("reportIncludeNDWasteBrkgVolume"), //
		REPORT_INCLUDE_VOLUME_MAI("reportIncludeVolumeMAI"), //
		REPORT_INCLUDE_SPEC_COMP("reportIncludeSpeciesComp");

		private String value;

		ExecutionOption(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static ExecutionOption fromValue(String value) {
			for (ExecutionOption b : ExecutionOption.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{0}\" is not a valid ExecutionOptions value", value)
			);
		}
	}

	/**
	 * Determines how the Age Range and Year Range are to be combined when producing yield tables.
	 * <p>
	 * <b>BIG NOTE</b>: VDYP7 -only- supports <b>INTERSECT</b>. So that's what we do. TODO: support the other
	 * combination types.
	 */
	public enum AgeYearRangeCombinationKind {

		UNION("union"), INTERSECT("intersect"), DIFFERENCE("difference");

		private String value;

		AgeYearRangeCombinationKind(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static AgeYearRangeCombinationKind fromValue(String value) {
			for (AgeYearRangeCombinationKind b : AgeYearRangeCombinationKind.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{0}\" is not a valid CombineAgeYearRange value", value)
			);
		}
	}

	public enum DebugOption {
		DO_INCLUDE_DEBUG_TIMESTAMPS("doIncludeDebugTimestamps"),
		DO_INCLUDE_DEBUG_ROUTINE_NAMES("doIncludeDebugRoutineNames"),
		DO_INCLUDE_DEBUG_ENTRY_EXIT("doIncludeDebugEntryExit"),
		DO_INCLUDE_DEBUG_INDENT_BLOCKS("doIncludeDebugIndentBlocks");

		private String value;

		DebugOption(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static DebugOption fromValue(String value) {
			for (DebugOption b : DebugOption.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{0}\" is not a valid DebugOptions value", value)
			);
		}
	}

	/**
	 * Controls how much metadata is displayed in the Output and Error Logs.
	 */
	public enum MetadataToOutputDirective {
		ALL("ALL"), MAIN("MAIN"), VERSION("VERSION"), MIN_IDENT("MIN_IDENT"), NONE("NONE");

		private String value;

		MetadataToOutputDirective(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		/**
		 * Converts <code>value</code> in a value from this enumeration, throwing an
		 * <code>IllegalArgumentException</code> when there's no match.
		 *
		 * @param value the corresponding to a value of this enumeration
		 * @return the enumeration value
		 * @throws IllegalArgumentException when conversion cannot be performed
		 */
		@JsonCreator
		public static MetadataToOutputDirective fromValue(String value) {
			for (MetadataToOutputDirective b : MetadataToOutputDirective.values()) {
				if (b.value.equals(value)) {
					return b;
				}
			}
			throw new IllegalArgumentException(
					MessageFormat.format("\"{0}\" is not a valid MetadataToOutput value", value)
			);
		}
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

	public Parameters outputFormat(OutputFormat outputFormat) {
		setOutputFormat(outputFormat);
		return this;
	}

	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat.value;
	}

	public Parameters outputFormat(String outputFormatText) {
		setOutputFormat(outputFormatText);
		return this;
	}

	public void setOutputFormat(String outputFormatText) {
		this.outputFormat = outputFormatText;
	}

	/**
	 * Get selectedExecutionOptions
	 *
	 * @return selectedExecutionOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS)
	public List<String> getSelectedExecutionOptions() {
		Validate.notNull(
				selectedExecutionOptions,
				"Parameters.getSelectedExecutionOptions: selectedExecutionOptions must not be null"
		);
		return selectedExecutionOptions;
	}

	public Parameters selectedExecutionOptions(List<ExecutionOption> selectedExecutionOptions) {
		setSelectedExecutionOptions(selectedExecutionOptions);
		return this;
	}

	public Parameters addSelectedExecutionOptionsItem(ExecutionOption selectedExecutionOptionsItem) {
		this.selectedExecutionOptions.add(selectedExecutionOptionsItem.getValue());
		return this;
	}

	public void setSelectedExecutionOptions(List<ExecutionOption> selectedExecutionOptions) {
		if (selectedExecutionOptions == null) {
			this.selectedExecutionOptions.clear();
		} else {
			this.selectedExecutionOptions = new ArrayList<String>();
			selectedExecutionOptions.stream().forEach(o -> this.selectedExecutionOptions.add(o.getValue()));
		}
	}

	public Parameters addSelectedExecutionOptionsItem(String selectedExecutionOptionsItemText) {
		if (selectedExecutionOptionsItemText != null) {
			this.selectedExecutionOptions.add(selectedExecutionOptionsItemText);
		}
		return this;
	}

	/**
	 * Get excludedExecutionOptions
	 *
	 * @return excludedExecutionOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_EXCLUDED_EXECUTION_OPTIONS)
	public List<String> getExcludedExecutionOptions() {
		Validate.notNull(
				excludedExecutionOptions,
				"Parameters.getExcludedExecutionOptions: excludedExecutionOptions must not be null"
		);
		return excludedExecutionOptions;
	}

	public Parameters excludedExecutionOptions(List<ExecutionOption> excludedExecutionOptions) {
		setExcludedExecutionOptions(excludedExecutionOptions);
		return this;
	}

	public Parameters addExcludedExecutionOptionsItem(ExecutionOption excludedExecutionOptionsItem) {
		this.excludedExecutionOptions.add(excludedExecutionOptionsItem.getValue());
		return this;
	}

	public void setExcludedExecutionOptions(List<ExecutionOption> excludedExecutionOptions) {
		if (excludedExecutionOptions == null) {
			this.excludedExecutionOptions.clear();
		} else {
			this.excludedExecutionOptions = new ArrayList<String>();
			excludedExecutionOptions.stream().forEach(o -> this.excludedExecutionOptions.add(o.getValue()));
		}
	}

	public Parameters addExcludedExecutionOptionsItem(String excludedExecutionOptionsItemText) {
		if (excludedExecutionOptionsItemText != null) {
			this.excludedExecutionOptions.add(excludedExecutionOptionsItemText);
		}
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

	public void setSelectedDebugOptions(List<DebugOption> selectedDebugOptions) {
		if (selectedDebugOptions == null) {
			this.selectedDebugOptions = null;
		} else {
			this.selectedDebugOptions = new ArrayList<>();
			selectedDebugOptions.stream().forEach(o -> this.selectedDebugOptions.add(o.getValue()));
		}
	}

	public Parameters selectedDebugOptions(List<DebugOption> selectedDebugOptions) {
		setSelectedDebugOptions(selectedDebugOptions);
		return this;
	}

	public Parameters addSelectedDebugOptionsItem(DebugOption selectedDebugOptionsItem) {
		if (selectedDebugOptions == null) {
			selectedDebugOptions = new ArrayList<>();
		}
		this.selectedDebugOptions.add(selectedDebugOptionsItem.getValue());
		return this;
	}

	public Parameters addSelectedDebugOptionsItem(String selectedDebugOptionsItemText) {
		if (selectedDebugOptionsItemText != null) {
			this.selectedDebugOptions.add(selectedDebugOptionsItemText);
		}
		return this;
	}

	/**
	 * Get excludedDebugOptions
	 *
	 * @return excludedDebugOptions
	 **/
	@JsonProperty(value = JSON_PROPERTY_EXCLUDED_DEBUG_OPTIONS)
	public List<String> getExcludedDebugOptions() {
		return excludedDebugOptions;
	}

	public void setExcludedDebugOptions(List<DebugOption> excludedDebugOptions) {
		if (excludedDebugOptions == null) {
			this.excludedDebugOptions = null;
		} else {
			this.excludedDebugOptions = new ArrayList<>();
			excludedDebugOptions.stream().forEach(o -> this.excludedDebugOptions.add(o.getValue()));
		}
	}

	public Parameters excludedDebugOptions(List<DebugOption> excludedDebugOptions) {
		setExcludedDebugOptions(excludedDebugOptions);
		return this;
	}

	public Parameters addExcludedDebugOptionsItem(DebugOption excludedDebugOptionsItem) {
		if (excludedDebugOptions == null) {
			excludedDebugOptions = new ArrayList<>();
		}
		this.excludedDebugOptions.add(excludedDebugOptionsItem.getValue());
		return this;
	}

	public Parameters addExcludedDebugOptionsItem(String excludedDebugOptionsItemText) {
		if (excludedDebugOptionsItemText != null) {
			this.excludedDebugOptions.add(excludedDebugOptionsItemText);
		}
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

	public Parameters ageStart(Integer ageStart) {
		setAgeStart(ageStart);
		return this;
	}

	public void setAgeStart(Integer ageStart) {
		this.ageStart = ageStart == null ? null : ageStart.toString();
	}

	public Parameters ageStart(String ageStartText) {
		setAgeStart(ageStartText);
		return this;
	}

	public void setAgeStart(String ageStartText) {
		this.ageStart = ageStartText;
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

	public Parameters ageEnd(Integer ageEnd) {
		setAgeEnd(ageEnd);
		return this;
	}

	public void setAgeEnd(Integer ageEnd) {
		this.ageEnd = ageEnd == null ? null : ageEnd.toString();
	}

	public Parameters ageEnd(String ageEndText) {
		setAgeEnd(ageEndText);
		return this;
	}

	public void setAgeEnd(String ageEndText) {
		this.ageEnd = ageEndText;
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

	public Parameters yearStart(Integer yearStart) {
		setYearStart(yearStart);
		return this;
	}

	public void setYearStart(Integer yearStart) {
		this.yearStart = yearStart == null ? null : yearStart.toString();
	}

	public Parameters yearStart(String yearStartText) {
		setYearStart(yearStartText);
		return this;
	}

	public void setYearStart(String yearStartText) {
		this.yearStart = yearStartText;
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

	public Parameters yearEnd(Integer yearEnd) {
		setYearEnd(yearEnd);
		return this;
	}

	public void setYearEnd(Integer yearEnd) {
		this.yearEnd = yearEnd == null ? null : yearEnd.toString();
	}

	public Parameters yearEnd(String yearEndText) {
		setYearEnd(yearEndText);
		return this;
	}

	public void setYearEnd(String yearEndText) {
		this.yearEnd = yearEndText == null ? null : yearEndText.toString();
	}

	/**
	 * Forces the inclusion of the specified calendar year in Yield tables.
	 *
	 * @return forceYear
	 **/
	@JsonProperty(value = JSON_PROPERTY_FORCE_YEAR)
	public String getYearForcedIntoYieldTable() {
		return yearForcedIntoYieldTable;
	}

	public Parameters yearForcedIntoYieldTable(Integer yearForcedIntoYieldTable) {
		setYearForcedIntoYieldTable(yearForcedIntoYieldTable);
		return this;
	}

	public void setYearForcedIntoYieldTable(Integer yearForcedIntoYieldTable) {
		this.yearForcedIntoYieldTable = yearForcedIntoYieldTable == null ? null : yearForcedIntoYieldTable.toString();
	}

	public Parameters yearForcedIntoYieldTable(String yearForcedIntoYieldTableText) {
		setYearForcedIntoYieldTable(yearForcedIntoYieldTableText);
		return this;
	}

	public void setYearForcedIntoYieldTable(String yearForcedIntoYieldTableText) {
		this.yearForcedIntoYieldTable = yearForcedIntoYieldTableText;
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

	public Parameters ageIncrement(Integer ageIncrement) {
		setAgeIncrement(ageIncrement);
		return this;
	}

	public void setAgeIncrement(Integer ageIncrement) {
		this.ageIncrement = ageIncrement == null ? null : ageIncrement.toString();
	}

	public Parameters ageIncrement(String ageIncrementText) {
		setAgeIncrement(ageIncrementText);
		return this;
	}

	public void setAgeIncrement(String ageIncrementText) {
		this.ageIncrement = ageIncrementText;
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

	public Parameters combineAgeYearRange(AgeYearRangeCombinationKind combineAgeYearRange) {
		setCombineAgeYearRange(combineAgeYearRange);
		return this;
	}

	public void setCombineAgeYearRange(AgeYearRangeCombinationKind combineAgeYearRange) {
		this.combineAgeYearRange = combineAgeYearRange == null ? null : combineAgeYearRange.getValue();
	}

	public Parameters combineAgeYearRange(String combineAgeYearRangeText) {
		setCombineAgeYearRange(combineAgeYearRangeText);
		return this;
	}

	public void setCombineAgeYearRange(String combineAgeYearRangeText) {
		this.combineAgeYearRange = combineAgeYearRangeText;
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

	public Parameters progressFrequency(ProgressFrequency.FrequencyKind progressFrequency) {
		setProgressFrequency(progressFrequency);
		return this;
	}

	public void setProgressFrequency(ProgressFrequency.FrequencyKind progressFrequency) {
		if (progressFrequency != null) {
			this.progressFrequency = progressFrequency.getValue();
		}
	}

	public Parameters progressFrequency(Integer progressFrequency) {
		setProgressFrequency(progressFrequency);
		return this;
	}

	public void setProgressFrequency(Integer progressFrequency) {
		if (progressFrequency != null) {
			this.progressFrequency = progressFrequency.toString();
		}
	}

	public Parameters progressFrequency(String progressFrequency) {
		setProgressFrequency(progressFrequency);
		return this;
	}

	public void setProgressFrequency(String progressFrequency) {
		if (progressFrequency != null) {
			this.progressFrequency = progressFrequency;
		}
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

	public Parameters metadataToOutput(MetadataToOutputDirective metadataToOutput) {
		setMetadataToOutput(metadataToOutput);
		return this;
	}

	public void setMetadataToOutput(MetadataToOutputDirective metadataToOutput) {
		this.metadataToOutput = metadataToOutput == null ? null : metadataToOutput.getValue();
	}

	public Parameters metadataToOutput(String metadataToOutputText) {
		setMetadataToOutput(metadataToOutputText);
		return this;
	}

	public void setMetadataToOutput(String metadataToOutputText) {
		this.metadataToOutput = metadataToOutputText;
	}

	/**
	 * Get filters
	 *
	 * @return filters
	 **/
	@JsonProperty(value = JSON_PROPERTY_FILTERS)
	public FilterParameters getFilters() {
		return filters;
	}

	public Parameters filters(FilterParameters filters) {
		setFilters(filters);
		return this;
	}

	public void setFilters(FilterParameters filters) {
		this.filters = filters == null ? null : filters.copy();
	}

	/**
	 * Get utils
	 *
	 * @return utils
	 */
	@JsonProperty(value = JSON_PROPERTY_UTILS)
	public List<UtilizationParameter> getUtils() {
		return utils;
	}

	public Parameters utils(List<UtilizationParameter> utils) {
		setUtils(utils);
		return this;
	}

	public void setUtils(List<UtilizationParameter> utils) {
		if (utils == null) {
			this.utils = null;
		} else {
			this.utils = new ArrayList<>();
			utils.stream().forEach(
					u -> this.utils.add(
							new UtilizationParameter().speciesName(u.getSpeciesName())
									.utilizationClass(u.getUtilizationClass())
					)
			);
		}
	}

	public Parameters addUtilsItem(UtilizationParameter utilsItem) {
		if (utilsItem != null) {
			this.utils.add(utilsItem);
		}
		return this;
	}

	public String getReportTitle() {
		return reportTitle;
	}

	public Parameters reportTitle(String reportTitle) {
		setReportTitle(reportTitle);
		return this;
	}

	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
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
				&& Objects.equals(this.excludedExecutionOptions, parameters.excludedExecutionOptions)
				&& Objects.equals(this.selectedDebugOptions, parameters.selectedDebugOptions)
				&& Objects.equals(this.excludedDebugOptions, parameters.excludedDebugOptions)
				&& Objects.equals(this.ageStart, parameters.ageStart) && Objects.equals(this.ageEnd, parameters.ageEnd)
				&& Objects.equals(this.yearStart, parameters.yearStart)
				&& Objects.equals(this.yearEnd, parameters.yearEnd)
				&& Objects.equals(this.yearForcedIntoYieldTable, parameters.yearForcedIntoYieldTable)
				&& Objects.equals(this.ageIncrement, parameters.ageIncrement)
				&& Objects.equals(this.combineAgeYearRange, parameters.combineAgeYearRange)
				&& Objects.equals(this.progressFrequency, parameters.progressFrequency)
				&& Objects.equals(this.metadataToOutput, parameters.metadataToOutput)
				&& Objects.equals(this.filters, parameters.filters) && Objects.equals(this.utils, parameters.utils);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				outputFormat, selectedExecutionOptions, excludedExecutionOptions, selectedDebugOptions,
				excludedDebugOptions, ageStart, ageEnd, yearStart, yearEnd, yearForcedIntoYieldTable, ageIncrement,
				combineAgeYearRange, progressFrequency, metadataToOutput, filters, utils
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Parameters {\n");

		sb.append("    outputFormat: ").append(toIndentedString(outputFormat)).append("\n");
		sb.append("    selectedExecutionOptions: ").append(toIndentedString(selectedExecutionOptions)).append("\n");
		sb.append("    excludedExecutionOptions: ").append(toIndentedString(excludedExecutionOptions)).append("\n");
		sb.append("    selectedDebugOptions: ").append(toIndentedString(selectedDebugOptions)).append("\n");
		sb.append("    excludedDebugOptions: ").append(toIndentedString(excludedDebugOptions)).append("\n");
		sb.append("    ageStart: ").append(toIndentedString(ageStart)).append("\n");
		sb.append("    ageEnd: ").append(toIndentedString(ageEnd)).append("\n");
		sb.append("    yearStart: ").append(toIndentedString(yearStart)).append("\n");
		sb.append("    yearEnd: ").append(toIndentedString(yearEnd)).append("\n");
		sb.append("    forceYear: ").append(toIndentedString(yearForcedIntoYieldTable)).append("\n");
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
