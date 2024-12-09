package ca.bc.gov.nrs.vdyp.backend.v1;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ParameterDetailsMessage;
import ca.bc.gov.nrs.vdyp.backend.v1.model.ParameterDetailsMessageBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition(
		info = @org.eclipse.microprofile.openapi.annotations.info.Info(
				version = "1.0.0", title = "Variable Density Yield Projection", description = "API for the Variable Density Yield Projection service"

		)
)
@ApplicationScoped
public class VdypApplication {

	public List<ParameterDetailsMessage> getHelpMessages() {

		List<ParameterDetailsMessage> messageList = new ArrayList<>();

		/* cmdLineOpt_OUTPUT_FORMAT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"outputFormat", //
						"Output Data Format", //
						"YieldTable | CSVYieldTable | DCSV", //
						"Identifies the output file format. One of (YieldTable default): YieldTable, CSVYieldTable, DCSV", //
						"YieldTable"
				)
		);

		/* cmdLineOpt_BACK_GROW_FLAG */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Allow Back Grow", //
						"true if present", //
						"Enables or disables the use of the Back Grow feature of VDYP7.", //
						"false"
				)
		);

		/* cmdLineOpt_FORWARD_GROW_FLAG */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.forwardGrowEnabled", //
						"Allow Forward Grow", //
						"true if present", //
						"Enables or disables the use of the Forward Grow feature of VDYP7.", //
						"true"
				)
		);

		/* cmdLineOpt_DEBUG_INCLUDE_TIMESTAMPS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugTimestamps", //
						"Debug Log Include Timestamps", //
						"true if present", //
						"Includes or suppresses Debug Log File Timestamps.", //
						"false"
				)
		);

		/* cmdLineOpt_DEBUG_INCLUDE_ROUTINE_NAMES */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugRoutineNames", //
						"Debug Log Include Routine Names", //
						"true if present", //
						"Includes or suppresses Debug Log File Routine Names.", //
						"false"
				)
		);

		/* cmdLineOpt_DEBUG_LOG_ENTRY_EXIT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugEntryExit", //
						"Debug Log Entry/Exit", //
						"true if present", //
						"Includes or suppresses Debug Log Block Entry and Exit.", //
						"false"
				)
		);

		/* cmdLineOpt_DEBUG_INDENT_LOG_BLOCKS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugIndentBlocks", //
						"Debug Indent Log Blocks", //
						"true if present", //
						"Indents Logging Blocks as they are Entered and Exited.", //
						"false"
				)
		);

		/* cmdLineOpt_SPCS_UTIL_LEVEL */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"utils", //
						"Species Utilization Level", //
						"<x> = Excl | 4.0 | 7.5 | 12.5 | 17.5 | 22.5", //
						"Sets the Species code <x> to the specified utilization level for reporting purposes. Repeat for each species as required." //
								+ " If doIncludeProjectedMoFBiomass or doIncludeProjectedCFSBiomass is set, this value is ignored.", //
						"0.0 (invalid)."
				)
		);

		/* cmdLineOpt_START_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"ageStart", //
						"Start Age", //
						"<age>", //
						"The starting age value for the Age Range for generated yield tables. Either -9 (not specified) or in the range 0..600.", //
						"-9"
				)
		);

		/* cmdLineOpt_MIN_START_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"minAgeStart", //
						"Minimum Start Age", //
						"<age>", //
						"Sets the minimum value for the start age of the Age Range.", //
						"0"
				)
		);

		/* cmdLineOpt_MAX_START_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"maxAgeStart", //
						"Maximum Start Age", //
						"<age>", //
						"Sets the maximum value for the start age of the Age Range.", //
						"600"
				)
		);

		/* cmdLineOpt_END_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"ageEnd", //
						"End Age", //
						"<age>", //
						"The ending age value for the Age Range for generated yield tables. Either -9 (not specified) or in the range 1..1000", //
						"-9"
				)
		);

		/* cmdLineOpt_MIN_END_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"minAgeEnd", //
						"Minimum End Age", //
						"<age>", //
						"Sets the minimum value for the end age of the Age Range.", //
						"1"
				)
		);

		/* cmdLineOpt_MAX_END_AGE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"maxEndAge", //
						"Maximum End Age", //
						"<age>", //
						"Sets the maximum value for the end age of the Age Range.", //
						"1000"
				)
		);

		/* cmdLineOpt_START_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"yearStart", //
						"Start Year", //
						"<calendar year>", //
						"The starting year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250.", //
						"-9"
				)
		);

		/* cmdLineOpt_END_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"yearEnd", //
						"End Year", //
						"<calendar year>", //
						"The ending year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250.", //
						"true"
				)
		);

		/* cmdLineOpt_INCREMENT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Increment", //
						"<increment value>", //
						"The number of years to increment the current value for the Age and Year Ranges. Either -9 (not specified) or in the range 1..350.", //
						"1"
				)
		);

		/* cmdLineOpt_MIN_AGE_INC */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"minAgeIncrement", //
						"Minimum Age Increment", //
						"<increment>", //
						"Sets the minimum value for the Age and Year increment.", //
						"1"
				)
		);

		/* cmdLineOpt_MAX_AGE_INC */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"maxAgeIncrement", //
						"Maximum Age Increment", //
						"<increment>", //
						"Sets the maximum value for the Age and Year increment.", //
						"350"
				)
		);

		/* cmdLineOpt_FORCE_REF_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceReferenceYearInclusionInYieldTables", //
						"Force Reference Year Indicator", //
						"true if present", //
						"Enables or disables the forced inclusion of the Reference Year in Yield Tables.", //
						"false"
				)
		);

		/* cmdLineOpt_FORCE_CRNT_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceCurrentYearInclusionInYieldTables", //
						"Force Current Year Indicator", //
						"true if present", //
						"Enables or disables the forced inclusion of the Current Year in Yield Tables.", //
						"false"
				)
		);

		/* cmdLineOpt_FORCE_SPCL_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceCalendarYearInclusionInYieldTables", //
						"Force Calendar Year Indicator", //
						"<calendar year>", //
						"Forces the inclusion of the specified calendar year in Yield Tables.", //
						"-9 (do not force)"
				)
		);

		/* cmdLineOpt_INCLUDE_FILE_HEADER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeFileHeader", //
						"Include output file headers (default) or not", //
						"true if present", //
						"In file formats where a file header is optional, this option will display or suppress the file header.", //
						"true"
				)
		);

		/* cmdLineOpt_INCLUDE_METADATA */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"metadataToOutput", //
						"Metadata to Include (default: VERSION )", //
						"ALL | MAIN | VERSION | MIN_IDENT | NONE", //
						"Controls how much metadata is displayed in the Output and Error Logs.", //
						"VERSION"
				)
		);

		/* cmdLineOpt_INCLUDE_PROJECTION_MODE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectionModeInYieldTable", //
						"Include Projection Mode Indicator", //
						"true if present", //
						"If present, a column indicating how the yield table row was projected is included in Yield Tables.", //
						"true"
				)
		);

		/* cmdLineOpt_INCLUDE_AGE_ROWS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Include Age Rows Indicator", //
						"true if present", //
						"Includes or excludes age rows of the Age Range in the Yield Table.", //
						"true"
				)
		);

		/* cmdLineOpt_INCLUDE_YEAR_ROWS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeAgeRowsInYieldTable", //
						"Include Year Rows Indicator", //
						"true if present", //
						"If true, the year rows of the Year Range are included in the Yield Table.", //
						"true"
				)
		);

		/* cmdLineOpt_FILTER_FOR_MAINTAINER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.maintainer", //
						"Filter Polygons For Maintainer", //
						"<maintainer value>", //
						"Only those polygons with the specified maintainer will be considered for inclusion in the output.", //
						"(filter ignored)"
				)
		);

		/* cmdLineOpt_FILTER_FOR_MAPSHEET */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.mapsheet", //
						"Filter Polygons For Mapsheet", //
						"<mapsheet value>", //
						"Only those polygons with the specified mapsheet will be considered for inclusion in the output.", //
						"(filter ignored)"
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_NUM */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Filter Polygons for Polygon Number", //
						"<polygon number>", //
						"Only the polygon with the specified polygon number will be considered for inclusion in the output.", //
						"-9 (filter ignored)"
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_ID */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Filter Polygons for Polygon Identifier", //
						"<polygon id>", //
						"Only the polygon with the specified polygon id will be considered for inclusion in the output.", //
						"-9 (filter ignored)"
				)
		);

		/* cmdLineOpt_PROGRESS_FREQUENCY */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Progress Frequency Mode", //
						"NEVER | EACH_MAPSHEET | EACH_POLYGON | <number>", //
						"Identifies how often or when progress will be reported from the application.", //
						"EACH_POLYGON"
				)
		);

		/* cmdLineOpt_YIELD_TABLE_INC_POLY_ID */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludePolygonRecordIdInYieldTable", //
						"Include Polygon ID Indicator", //
						"true if present", //
						"Include the POLYGON_RCRD_ID in the header of yield tables.", //
						"false"
				)
		);

		/* cmdLineOpt_ALLOW_BA_TPH_SUBSTITUTION */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doAllowBasalAreaAndTreesPerHectareValueSubstitution", //
						"Allow Supplied BA/TPH to be used as Projected", //
						"true if present", //
						"If present, the substitution of Supplied BA/TPH as Projected Values is allowed.", //
						"true"
				)
		);

		/* cmdLineOpt_SECONDARY_SPCS_HEIGHT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeSecondarySpeciesDominantHeightInYieldTable", //
						"Display secondary species height in yield tables.", //
						"true if present", //
						"Display/Suppress the Secondary Species Dominant Height column in Yield Tables.", //
						"false"
				)
		);

		/* cmdLineOpt_PROJECTED_BY_POLYGON */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doSummarizeProjectionByPolygon", //
						"Projection summarized by polygon", //
						"true if present", //
						"If present, projected values are summarized at the polygon level.", //
						"false"
				)
		);

		/* cmdLineOpt_PROJECTED_BY_LAYER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doSummarizeProjectionByLayer", //
						"Projection summarized by layer", //
						"true if present", //
						"If present, projected values are summarized at the layer level.", //
						"true"
				)
		);

		/* cmdLineOpt_PROJECTED_BY_SPECIES */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeSpeciesProjection", //
						"Projection produced by Species", //
						"true if present", //
						"If present, projected values are produced for each species.", //
						"false"
				)
		);

		/* cmdLineOpt_PROJECTED_MOF_VOLUME */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedMoFVolumes", //
						"Include MoF Projected Volumes", //
						"true if present", //
						"Indicate whether MoF projected volumes are included in the output.", //
						"true"
				)
		);

		/* cmdLineOpt_PROJECTED_MOF_BIOMASS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedMoFBiomass", //
						"Include Projected MoF Biomass", //
						"true if present", //
						"Indicate whether projected MoF biomass is included in the output.", //
						"false"
				)
		);

		/* cmdLineOpt_PROJECTED_CFS_BIOMASS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedCFSBiomass", //
						"Include Projected CFS Biomass", //
						"true if present", //
						"Indicate whether projected CFS biomass is included in the output.", //
						"false"
				)
		);

		/* cmdLineOpt_YLDTBL_COLUMN_HEADERS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeColumnHeadersInYieldTable", //
						"Include Formatted Yield Table Column Headers", //
						"true if present", //
						"Indicate whether formatted yield tables will include column headers or not.", //
						"true"
				)
		);

		return messageList;
	}
}
