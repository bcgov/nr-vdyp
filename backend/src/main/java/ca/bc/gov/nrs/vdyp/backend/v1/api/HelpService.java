package ca.bc.gov.nrs.vdyp.backend.v1.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.NotFoundException;
import ca.bc.gov.nrs.vdyp.backend.v1.api.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ParameterDetailsMessage;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.responses.HelpResource;
import ca.bc.gov.nrs.vdyp.backend.v1.model.ParameterDetailsMessageBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class HelpService {

	private static final Logger logger = LoggerFactory.getLogger(HelpService.class);

	public HelpResource helpGet(UriInfo uriInfo, SecurityContext securityContext) throws NotFoundException {

		logger.info("<helpGet");

		List<ParameterDetailsMessage> messageList = new ArrayList<>();

		/* cmdLineOpt_OUTPUT_FORMAT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"outputFormat", //
						"Output Data Format", //
						"YieldTable | CSVYieldTable | DCSV", //
						"Identifies the output file format. One of (YieldTable default): YieldTable, CSVYieldTable, DCSV", //
						ValidatedParameters.DEFAULT.getOutputFormat().name()
				)
		);

		/* cmdLineOpt_BACK_GROW_FLAG */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Allow Back Grow", //
						"true if present", //
						"Enables or disables the use of the Back Grow feature of VDYP7.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.BACK_GROW_ENABLED)
						)
				)
		);

		/* cmdLineOpt_FORWARD_GROW_FLAG */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.forwardGrowEnabled", //
						"Allow Forward Grow", //
						"true if present", //
						"Enables or disables the use of the Forward Grow feature of VDYP7.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.FORWARD_GROW_ENABLED)
						)
				)
		);

		/* cmdLineOpt_DEBUG_INCLUDE_TIMESTAMPS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugTimestamps", //
						"Debug Log Include Timestamps", //
						"true if present", //
						"Includes or suppresses Debug Log File Timestamps.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedDebugOptions()
										.contains(Parameters.SelectedDebugOptionsEnum.DO_INCLUDE_DEBUG_TIMESTAMPS)
						)
				)
		);

		/* cmdLineOpt_DEBUG_INCLUDE_ROUTINE_NAMES */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugRoutineNames", //
						"Debug Log Include Routine Names", //
						"true if present", //
						"Includes or suppresses Debug Log File Routine Names.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedDebugOptions()
										.contains(Parameters.SelectedDebugOptionsEnum.DO_INCLUDE_DEBUG_ROUTINE_NAMES)
						)
				)
		);

		/* cmdLineOpt_DEBUG_LOG_ENTRY_EXIT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugEntryExit", //
						"Debug Log Entry/Exit", //
						"true if present", //
						"Includes or suppresses Debug Log Block Entry and Exit.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedDebugOptions()
										.contains(Parameters.SelectedDebugOptionsEnum.DO_INCLUDE_DEBUG_ENTRY_EXIT)
						)
				)
		);

		/* cmdLineOpt_DEBUG_INDENT_LOG_BLOCKS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedDebugOptions.doIncludeDebugIndentBlocks", //
						"Debug Indent Log Blocks", //
						"true if present", //
						"Indents Logging Blocks as they are Entered and Exited.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedDebugOptions()
										.contains(Parameters.SelectedDebugOptionsEnum.DO_INCLUDE_DEBUG_INDENT_BLOCKS)
						)
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
						"The starting year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250.", //
						"none"
				)
		);

		/* cmdLineOpt_END_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"yearEnd", //
						"End Year", //
						"<calendar year>", //
						"The ending year for the Year Range for generated yield tables. Either -9 (not specified) or in the range 1400..3250.", //
						"none"
				)
		);

		/* cmdLineOpt_INCREMENT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Increment", //
						"<increment value>", //
						"The number of years to increment the current value for the Age and Year Ranges. Either -9 (not specified) or in the range 1..350.", //
						"none"
				)
		);

		/* cmdLineOpt_FORCE_REF_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceReferenceYearInclusionInYieldTables", //
						"Force Reference Year Indicator", //
						"true if present", //
						"Enables or disables the forced inclusion of the Reference Year in Yield Tables.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
								)
						)
				)
		);

		/* cmdLineOpt_FORCE_CRNT_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceCurrentYearInclusionInYieldTables", //
						"Force Current Year Indicator", //
						"true if present", //
						"Enables or disables the forced inclusion of the Current Year in Yield Tables.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
								)
						)
				)
		);

		/* cmdLineOpt_FORCE_SPCL_YEAR */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doForceCalendarYearInclusionInYieldTables", //
						"Force Calendar Year Indicator", //
						"<calendar year>", //
						"Forces the inclusion of the specified calendar year in Yield Tables.", //
						"none"
				)
		);

		/* cmdLineOpt_INCLUDE_FILE_HEADER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeFileHeader", //
						"Include output file headers (default) or not", //
						"true if present", //
						"In file formats where a file header is optional, this option will display or suppress the file header.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_FILE_HEADER)
						)
				)
		);

		/* cmdLineOpt_INCLUDE_METADATA */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"metadataToOutput", //
						"Metadata to Include (default: VERSION )", //
						"ALL | MAIN | VERSION | MIN_IDENT | NONE", //
						"Controls how much metadata is displayed in the Output and Error Logs.", //
						ValidatedParameters.DEFAULT.getMetadataToOutputText()
				)
		);

		/* cmdLineOpt_INCLUDE_PROJECTION_MODE */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectionModeInYieldTable", //
						"Include Projection Mode Indicator", //
						"true if present", //
						"If present, a column indicating how the yield table row was projected is included in Yield Tables.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE
								)
						)
				)
		);

		/* cmdLineOpt_INCLUDE_AGE_ROWS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Include Age Rows Indicator", //
						"true if present", //
						"Includes or excludes age rows of the Age Range in the Yield Table.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE
								)
						)
				)
		);

		/* cmdLineOpt_INCLUDE_YEAR_ROWS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeAgeRowsInYieldTable", //
						"Include Year Rows Indicator", //
						"true if present", //
						"If true, the year rows of the Year Range are included in the Yield Table.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE
								)
						)
				)
		);

		/* cmdLineOpt_FILTER_FOR_MAINTAINER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.maintainer", //
						"Filter Polygons For Maintainer", //
						"<maintainer value>", //
						"Only those polygons with the specified maintainer will be considered for inclusion in the output.", //
						ValidatedParameters.DEFAULT.getFilters().getMaintainer() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getMaintainer()
				)
		);

		/* cmdLineOpt_FILTER_FOR_MAPSHEET */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"filters.mapsheet", //
						"Filter Polygons For Mapsheet", //
						"<mapsheet value>", //
						"Only those polygons with the specified mapsheet will be considered for inclusion in the output.", //
						ValidatedParameters.DEFAULT.getFilters().getMapsheet() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getMapsheet()
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_NUM */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Filter Polygons for Polygon Number", //
						"<polygon number>", //
						"Only the polygon with the specified polygon number will be considered for inclusion in the output.", //
						ValidatedParameters.DEFAULT.getFilters().getPolygon() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getPolygon()
				)
		);

		/* cmdLineOpt_FILTER_FOR_POLYGON_ID */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Filter Polygons for Polygon Identifier", //
						"<polygon id>", //
						"Only the polygon with the specified polygon id will be considered for inclusion in the output.", //
						ValidatedParameters.DEFAULT.getFilters().getPolygonId() == null ? "not filtered by this value"
								: ValidatedParameters.DEFAULT.getFilters().getPolygonId()
				)
		);

		/* cmdLineOpt_PROGRESS_FREQUENCY */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.backGrowEnabled", //
						"Progress Frequency Mode", //
						"NEVER | EACH_MAPSHEET | EACH_POLYGON | <number>", //
						"Identifies how often or when progress will be reported from the application.", //
						ValidatedParameters.DEFAULT.getProgressFrequency().toString()
				)
		);

		/* cmdLineOpt_YIELD_TABLE_INC_POLY_ID */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludePolygonRecordIdInYieldTable", //
						"Include Polygon ID Indicator", //
						"true if present", //
						"Include the POLYGON_RCRD_ID in the header of yield tables.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE
								)
						)
				)
		);

		/* cmdLineOpt_ALLOW_BA_TPH_SUBSTITUTION */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doAllowBasalAreaAndTreesPerHectareValueSubstitution", //
						"Allow Supplied BA/TPH to be used as Projected", //
						"true if present", //
						"If present, the substitution of Supplied BA/TPH as Projected Values is allowed.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION
								)
						)
				)
		);

		/* cmdLineOpt_SECONDARY_SPCS_HEIGHT */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeSecondarySpeciesDominantHeightInYieldTable", //
						"Display secondary species height in yield tables.", //
						"true if present", //
						"Display/Suppress the Secondary Species Dominant Height column in Yield Tables.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
								)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_BY_POLYGON */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doSummarizeProjectionByPolygon", //
						"Projection summarized by polygon", //
						"true if present", //
						"If present, projected values are summarized at the polygon level.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_SUMMARIZE_PROJECTION_BY_POLYGON
								)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_BY_LAYER */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doSummarizeProjectionByLayer", //
						"Projection summarized by layer", //
						"true if present", //
						"If present, projected values are summarized at the layer level.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_SUMMARIZE_PROJECTION_BY_LAYER
								)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_BY_SPECIES */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeSpeciesProjection", //
						"Projection produced by Species", //
						"true if present", //
						"If present, projected values are produced for each species.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_SPECIES_PROJECTION)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_MOF_VOLUME */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedMoFVolumes", //
						"Include MoF Projected Volumes", //
						"true if present", //
						"Indicate whether MoF projected volumes are included in the output.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_MOF_VOLUMES
								)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_MOF_BIOMASS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedMoFBiomass", //
						"Include Projected MoF Biomass", //
						"true if present", //
						"Indicate whether projected MoF biomass is included in the output.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_MOF_BIOMASS
								)
						)
				)
		);

		/* cmdLineOpt_PROJECTED_CFS_BIOMASS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeProjectedCFSBiomass", //
						"Include Projected CFS Biomass", //
						"true if present", //
						"Indicate whether projected CFS biomass is included in the output.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_CFS_BIOMASS
								)
						)
				)
		);

		/* cmdLineOpt_YLDTBL_COLUMN_HEADERS */
		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doIncludeColumnHeadersInYieldTable", //
						"Include Formatted Yield Table Column Headers", //
						"true if present", //
						"Indicate whether formatted yield tables will include column headers or not.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions().contains(
										Parameters.SelectedExecutionOptionsEnum.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE
								)
						)
				)
		);

		/* Additional Parameters supported by VDYP8 */

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doEnableProgressLogging", //
						"Enable Progress logging", //
						"true if present", //
						"Enables or disables the logging of progress messages during projections.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.DO_ENABLE_DEBUG_LOGGING)
						)
				)
		);

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doEnableErrorLogging", //
						"Enable Error logging", //
						"true if present", //
						"Enables or disables the logging of error messages during projections.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.DO_ENABLE_ERROR_LOGGING)
						)
				)
		);

		messageList.add(
				ParameterDetailsMessageBuilder.build(
						"selectedExecutionOptions.doEnableDebugLogging", //
						"Enable Debug logging", //
						"true if present", //
						"Enables or disables the logging of debug messages during projections.", //
						Boolean.toString(
								ValidatedParameters.DEFAULT.getSelectedExecutionOptions()
										.contains(Parameters.SelectedExecutionOptionsEnum.DO_ENABLE_PROGRESS_LOGGING)
						)
				)
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
}
