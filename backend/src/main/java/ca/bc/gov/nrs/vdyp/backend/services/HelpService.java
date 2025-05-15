package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ParameterDetailsMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.HelpResource;
import ca.bc.gov.nrs.vdyp.backend.utils.ParameterDetailsMessageBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class HelpService {

	private static final Logger logger = LoggerFactory.getLogger(HelpService.class);

	public HelpResource helpGet(UriInfo uriInfo, SecurityContext securityContext) {

		logger.info("<helpGet");

		List<ParameterDetailsMessage> messageList = new ArrayList<>();

		/* cmdLineOpt_OUTPUT_FORMAT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"outputFormat", //
						"Output Data Format", //
						"YieldTable | CSVYieldTable | DCSV | PLOTSY", //
						"Identifies the output file format. One of: YieldTable, CSVYieldTable, DCSV, PLOTSY", //
						ValidatedParameters.DEFAULT.getOutputFormat().getValue()
				)
		);

		/* cmdLineOpt_BACK_GROW_FLAG */
		addYesNoExecutionMessage(
				messageList, //
				"backGrowEnabled", //
				"Back Grow", //
				"the Back Grow feature of VDYP", //
				Parameters.ExecutionOption.BACK_GROW_ENABLED
		);

		/* cmdLineOpt_FORWARD_GROW_FLAG */
		addYesNoExecutionMessage(
				messageList, //
				"forwardGrowEnabled", //
				"Forward Grow", //
				"the Forward Grow feature of VDYP", //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED
		);

		/* cmdLineOpt_DEBUG_INCLUDE_TIMESTAMPS */
		addYesNoDebugMessage(
				messageList, //
				"doIncludeDebugTimestamps", //
				"including timestamps in Debug Log", //
				"the inclusion of timestamps in the Debug Log file", //
				Parameters.DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS
		);

		/* cmdLineOpt_DEBUG_INCLUDE_ROUTINE_NAMES */
		addYesNoDebugMessage(
				messageList, //
				"doIncludeDebugRoutineNames", //
				"including routine names in Debug Log", //
				"the inclusion of routine names in Debug Log file", //
				Parameters.DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES
		);

		/* cmdLineOpt_DEBUG_LOG_ENTRY_EXIT */
		addYesNoDebugMessage(
				messageList, //
				"doIncludeDebugEntryExit", //
				"logging debug log block entry and exit", //
				"the inclusion of Debug Log block entry and exit", //
				Parameters.DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT
		);

		/* cmdLineOpt_DEBUG_INDENT_LOG_BLOCKS */
		addYesNoDebugMessage(
				messageList, //
				"doIncludeDebugIndentBlocks", //
				"log block indentation", //
				"the indentation of log blocks in the Debug Log as they are entered and exited", //
				Parameters.DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS
		);

		/* cmdLineOpt_SPCS_UTIL_LEVEL */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"utils", //
						"Species Utilization Level", //
						"<x> = Excl | 4.0 | 7.5 | 12.5 | 17.5 | 22.5", //
						"Sets the Species code <x> to the specified utilization level for reporting purposes. Repeat for each species as required"//
								+ "If doIncludeProjectedMoFBiomass or doIncludeProjectedCFSBiomass is set, this value is ignored", //
						"0.0 (invalid)"
				)
		);

		/* cmdLineOpt_START_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"ageStart", //
						"Start Age", //
						"<age>", //
						"The starting age value for the Age Range for generated yield tables. Either -9 (not specified) or in the range 0..600", //
						"none"
				)
		);

		/* cmdLineOpt_END_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"ageEnd", //
						"End Age", //
						"<age>", //
						"The ending age value for the Age Range for generated yield tables. Either -9 (not specified) or in the range 1..1000", //
						"none"
				)
		);

		/* cmdLineOpt_START_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"yearStart", //
						"Start Year", //
						"<calendar year>", //
						"The starting year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250", //
						"none"
				)
		);

		/* cmdLineOpt_END_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"yearEnd", //
						"End Year", //
						"<calendar year>", //
						"The ending year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250", //
						"none"
				)
		);

		/* cmdLineOpt_INCREMENT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"ageIncrement", //
						"Increment", //
						"<increment value>", //
						"The number of years to increment the current value for the Age and Year Ranges. Either -9 (not specified) or in the range 1..350", //
						"none"
				)
		);

		/* cmdLineOpt_FORCE_REF_YEAR */
		addYesNoExecutionMessage(
				messageList, //
				"doForceReferenceYearInclusionInYieldTables", //
				"forcing reference year into yield table", //
				"the forced inclusion of a polygon's reference year into yield tables", //
				Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
		);

		/* cmdLineOpt_FORCE_CRNT_YEAR */
		addYesNoExecutionMessage(
				messageList, //
				"doForceCurrentYearInclusionInYieldTables", //
				"forcing the current year into yield table", //
				"the forced inclusion of the current year into yield tables", //
				Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
		);

		/* cmdLineOpt_FORCE_SPCL_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"forceYear", //
						"Force Calendar Year Indicator", //
						"<calendar year>", //
						"Forces the inclusion of the specified calendar year in yield tables", //
						"none"
				)
		);

		/* cmdLineOpt_INCLUDE_FILE_HEADER */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeFileHeader", //
				"output file headers (default) in yield tables", //
				"display of file headers in yield table file formats for which a file header is optional", //
				Parameters.ExecutionOption.DO_INCLUDE_FILE_HEADER
		);

		/* cmdLineOpt_INCLUDE_METADATA */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"metadataToOutput", //
						"Metadata to Include (default: VERSION )", //
						"ALL | MAIN | VERSION | MIN_IDENT | NONE", //
						"Controls how much metadata is displayed in the Output and Error Logs", //
						ValidatedParameters.DEFAULT.getMetadataToOutput().getValue()
				)
		);

		/* cmdLineOpt_INCLUDE_PROJECTION_MODE */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeProjectionModeInYieldTable", //
				"including projection mode method in the yield table", //
				"the inclusion of a yield table column indicating how the row was projected", //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE
		);

		/* cmdLineOpt_INCLUDE_AGE_ROWS */
		addYesNoExecutionMessage(
				messageList, "backGrowEnabled", //
				"including the age information in the yield tables", //
				"the inclusion of age range data in the yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE
		);

		/* cmdLineOpt_INCLUDE_YEAR_ROWS */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeAgeRowsInYieldTable", //
				"including the year information in the yield tables", //
				"the inclusion of year range data in the yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE
		);

		/* cmdLineOpt_FILTER_FOR_MAINTAINER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.maintainer", //
						"filter by maintainer", //
						"<maintainer value>", //
						"Only those polygons with the specified maintainer will be considered for inclusion in the output", //
						ValidatedParameters.DEFAULT.getFilters().getMaintainer() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getMaintainer()
				)
		);

		/* cmdLineOpt_FILTER_FOR_MAPSHEET */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.mapsheet", //
						"filter by mapsheet", //
						"<mapsheet value>", //
						"Only those polygons with the specified mapsheet will be considered for inclusion in the output", //
						ValidatedParameters.DEFAULT.getFilters().getMapsheet() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getMapsheet()
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_NUM */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.polygon", //
						"filter by polygon number", //
						"<polygon number>", //
						"Only the polygon with the specified polygon number will be considered for inclusion in the output", //
						ValidatedParameters.DEFAULT.getFilters().getPolygon() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getPolygon()
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_ID */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.polygonId", //
						"filter by polygon id", //
						"<polygon id>", //
						"Only the polygon with the specified polygon id will be considered for inclusion in the output", //
						ValidatedParameters.DEFAULT.getFilters().getPolygonId() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getPolygonId()
				)
		);

		/* cmdLineOpt_PROGRESS_FREQUENCY */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"progressFrequency", //
						"Progress Frequency Mode", //
						"NEVER | EACH_MAPSHEET | EACH_POLYGON | <number>", //
						"Identifies how often or when progress will be reported from the application", //
						ValidatedParameters.DEFAULT.getProgressFrequency().toString()
				)
		);

		/* cmdLineOpt_YIELD_TABLE_INC_POLY_ID */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludePolygonRecordIdInYieldTable", //
				"including polygon ids", //
				"the inclusion of polygon ids in yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE
		);

		/* cmdLineOpt_ALLOW_BA_TPH_SUBSTITUTION */
		addYesNoExecutionMessage(
				messageList, //
				"doAllowBasalAreaAndTreesPerHectareValueSubstitution", //
				"supplied BA/TPH to be used as Projected", //
				"the substitution of Supplied BA/TPH as Projected Values", //
				Parameters.ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION
		);

		/* cmdLineOpt_SECONDARY_SPCS_HEIGHT */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeSecondarySpeciesDominantHeightInYieldTable", //
				"displaying secondary species height in yield tables", //
				"the display of the dominant height of a layer's secondary species in yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
		);

		/* cmdLineOpt_PROJECTED_BY_POLYGON */
		addYesNoExecutionMessage(
				messageList, //
				"doSummarizeProjectionByPolygon", //
				"including a projection summary per polygon", //
				"the inclusion of a summary at the polygon level of the projected values", //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		);

		/* cmdLineOpt_PROJECTED_BY_LAYER */
		addYesNoExecutionMessage(
				messageList, //
				"doSummarizeProjectionByLayer", //
				"including a projection summary per layer", //
				"the inclusion of a summary at the layer level of the projected values in yield tables", //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER
		);

		/* cmdLineOpt_PROJECTED_BY_SPECIES */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeSpeciesProjection", //
				"including species projection values", //
				"the presentation of projected values for each species in yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION
		);

		/* cmdLineOpt_PROJECTED_MOF_VOLUME */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeProjectedMoFVolumes", //
				"including MoF Projected Volumes", //
				"the inclusion of MoF projected volumes in the yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES
		);

		/* cmdLineOpt_PROJECTED_MOF_BIOMASS */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeProjectedMoFBiomass", //
				"including projected MoF biomass", //
				"the inclusion of projected MoF biomass in the yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS
		);

		/* cmdLineOpt_PROJECTED_CFS_BIOMASS */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeProjectedCFSBiomass", //
				"including projected CFS biomass", //
				"the inclusion of projected CFS biomass in the yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS
		);

		/* cmdLineOpt_YLDTBL_COLUMN_HEADERS */
		addYesNoExecutionMessage(
				messageList, //
				"doIncludeColumnHeadersInYieldTable", //
				"including yield table column headers", //
				"the inclusion of column headers in formatted yield tables", //
				Parameters.ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE
		);

		/* Additional Parameters supported by VDYP8 */

		addYesNoExecutionMessage(
				messageList, //
				"doEnableProgressLogging", //
				"progress logging", //
				"the logging of progress messages during projection", //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING
		);

		addYesNoExecutionMessage(
				messageList, //
				"doEnableErrorLogging", //
				"error logging", //
				"the logging of error messages during projection", //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING
		);

		addYesNoExecutionMessage(
				messageList, "doEnableDebugLogging", //
				"debug logging", //
				"the logging of debug messages during projection", //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING
		);

		addYesNoExecutionMessage(
				messageList, //
				"doIncludeProjectionFiles", //
				"including projection results in output", //
				"the inclusion in the response of the output of all invocations of BACK and FORWARD during the projection run", //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES
		);

		addYesNoExecutionMessage(
				messageList, //
				"doDelayExecutionFolderDeletion", //
				"retain execution folder", //
				"retain the execution folder for " + ProjectionContext.EXECUTION_FOLDER_RETENTION_TIME_m + " minutes ", //
				Parameters.ExecutionOption.DO_DELAY_EXECUTION_FOLDER_DELETION
		);

		messageList.sort(new Comparator<ParameterDetailsMessage>() {

			@Override
			public int compare(ParameterDetailsMessage o1, ParameterDetailsMessage o2) {
				return o1.getField().compareTo(o2.getField());
			}
		});

		logger.info(">helpGet");

		return HelpResource.of(uriInfo, messageList);
	}

	private void addYesNoExecutionMessage(
			List<ParameterDetailsMessage> messageList, String field, String shortDescription, String longDescription,
			Parameters.ExecutionOption executionOption
	) {

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions." + field, //
						"Allow " + shortDescription, //
						"Enables " + longDescription, //
						ValidatedParameters.DEFAULT.containsOption(executionOption) ? "Selected" : "Not Selected"
				)
		);

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"excludedExecutionOptions." + field, //
						"Disallow " + shortDescription, //
						"Disables " + longDescription, //
						ValidatedParameters.DEFAULT.containsOption(executionOption) ? "Not Excluded" : "Excluded"
				)
		);
	}

	private void addYesNoDebugMessage(
			List<ParameterDetailsMessage> messageList, String field, String shortDescription, String longDescription,
			Parameters.DebugOption debugOption
	) {

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions." + field, //
						"Allow " + shortDescription, //
						"Enables " + longDescription, //
						ValidatedParameters.DEFAULT.getSelectedDebugOptions().contains(debugOption) ? "Selected"
								: "Not Selected"
				)
		);

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"excludedDebugOptions." + field, //
						"Disallow " + shortDescription, //
						"Disables " + longDescription, //
						ValidatedParameters.DEFAULT.getSelectedDebugOptions().contains(debugOption) ? "Not Excluded"
								: "Excluded"
				)
		);
	}
}
