package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.StandYieldCalculationException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.StandYieldMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationClassSet;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.FailedToGrowYoungStandException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZoneMethods;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

public class YieldTable implements Closeable {

	public enum Category {
		LAYER_MOFVOLUMES, LAYER_MOFBIOMASS, SPECIES_MOFVOLUME, SPECIES_MOFBIOMASS, CFSBIOMASS, PROJECTION_MODE,
		POLYGON_ID, NONE
	}

	private static final Logger logger = LoggerFactory.getLogger(YieldTable.class);

	private final ProjectionContext context;
	private final ValidatedParameters params;

	private YieldTableWriter<? extends YieldTableRowBean> writer;
	private Path yieldTableFilePath;

	private int nextYieldTableNumber = 1;

	private YieldTable(ProjectionContext context) throws YieldTableGenerationException {
		this.context = context;
		this.params = context.getParams();
		this.writer = buildYieldTableWriter(params.getOutputFormat());
	}

	public static YieldTable of(ProjectionContext context) throws YieldTableGenerationException {

		return new YieldTable(context);
	}

	public void startGeneration() throws YieldTableGenerationException {
		writer.writeHeader();
	}

	public void generateYieldTableForPolygon(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		generateYieldTable(polygon, projectionResults, state, null, doGenerateDetailedTableHeader);
	}

	/**
	 * <b>lcl_GenerateYieldTable</b>
	 * <p>
	 * Generate a single yield table.
	 * <p>
	 *
	 * @param polygon                       the polygon for which a yield table is to be generated
	 * @param projectionResults             the result of projecting the polygon
	 * @param layerReportingInfo            the layer of the polygon. May be null, indicating that the yield table
	 *                                      summarizes information at the polygon level only.
	 * @param state                         the current state of the (completed) projection of <code>polygon</code>
	 * @param doGenerateDetailedTableHeader if true, displays all the expected details in the table header. If false,
	 *                                      only the table number is generated.
	 */
	public void generateYieldTableForPolygonLayer(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		if (layerReportingInfo == null) {
			throw new IllegalArgumentException("generateYieldTableForPolygonLayer: layerReportingInfo cannot be null");
		}

		generateYieldTable(polygon, projectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader);
	}

	public void generateCfsBiomassTableForPolygon(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {
		generateCfsBiomassTable(polygon, projectionResults, state, null, doGenerateDetailedTableHeader);
	}

	public void generateCfsBiomassTableForPolygonLayer(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {
		if (layerReportingInfo == null) {
			throw new IllegalArgumentException(
					"generateCfsBiomassTableForPolygonLayer: layerReportingInfo cannot be null"
			);
		}
		generateCfsBiomassTable(polygon, projectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader);
	}

	/**
	 * <b>lcl_GenerateCFSBiomassTable</b>
	 * <p>
	 * Generate a single CFS Biomass yield table.
	 * <p>
	 *
	 * @param polygon                       the polygon for which a yield table is to be generated
	 * @param projectionResults             the result of projecting the polygon
	 * @param state                         the current state of the (completed) projection of <code>polygon</code>
	 * @param layerReportingInfo            the layer of the polygon. May be null, indicating that the CFS Biomass yield
	 *                                      table summarizes information at the polygon level only.
	 * @param doGenerateDetailedTableHeader if true, displays all the expected details in the table header. If false,
	 *                                      only the table number is generated.
	 */
	public void generateCfsBiomassTable(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		writer.setCFSCategories(context);

		writeCategorizedYieldTable(
				polygon, projectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader
		);
	}

	/**
	 * Given the properly set categories generate a yield tale running only the relevant portions.
	 *
	 * @param polygon                       the polygon for which a yield table is to be generated
	 * @param projectionResults             map of year to the result of projecting the polygon to that year
	 * @param state                         the current state of the (completed) projection of <code>polygon</code>
	 * @param layerReportingInfo            the layer of the polygon. May be null, indicating that the CFS Biomass yield
	 *                                      * table summarizes information at the polygon level only.
	 * @param doGenerateDetailedTableHeader if true, displays all the expected details in the table header. If false, *
	 *                                      only the table number is generated.
	 * @throws YieldTableGenerationException in the event of a write error or calculation error
	 */
	private void writeCategorizedYieldTable(
			Polygon polygon, Map<Integer, VdypPolygon> projectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {
		writer.recordPolygonProjectionState(state);

		if (context.getParams().containsOption(ExecutionOption.REPORT_INCLUDE_CULMINATION_VALUES)) {
			YieldTableRowIterator culminationIterator = new YieldTableRowIterator(
					context, polygon, state, layerReportingInfo, 1
			);
			while (culminationIterator.hasNext()) {

				YieldTableRowContext rowContext = culminationIterator.next();
				if (rowIsToBeGenerated(rowContext)) {
					try {
						EntityVolumeDetails volume = getProjectedLayerStandVolumes(
								rowContext, projectionResults, layerReportingInfo.getLayer(),
								rowContext.getCurrentTableAge()
						);
						writer.recordCulminationValues(rowContext.getCurrentTableAge(), volume);
					} catch (Exception ex) {
					}
				}
			}
		}

		writer.writePolygonTableHeader(
				polygon, Optional.ofNullable(layerReportingInfo), doGenerateDetailedTableHeader, nextYieldTableNumber
		);

		YieldTableRowIterator rowIterator = new YieldTableRowIterator(context, polygon, state, layerReportingInfo);
		while (rowIterator.hasNext()) {

			YieldTableRowContext rowContext = rowIterator.next();
			if (rowIsToBeGenerated(rowContext)) {
				generateYieldTableRow(rowContext, projectionResults, writer);
			}
		}

		writer.writePolygonTableTrailer(nextYieldTableNumber);

		nextYieldTableNumber += 1;
	}

	public void endGeneration() throws YieldTableGenerationException {
		writer.writeTrailer();
	}

	private void generateYieldTable(
			Polygon polygon, Map<Integer, VdypPolygon> polygonProjectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {
		writer.setMOFCategories(context);
		writeCategorizedYieldTable(
				polygon, polygonProjectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader
		);
	}

	private YieldTableWriter<? extends YieldTableRowBean> buildYieldTableWriter(OutputFormat outputFormat)
			throws YieldTableGenerationException {

		YieldTableWriter<? extends YieldTableRowBean> builtWriter = switch (outputFormat) {
		case CSV_YIELD_TABLE -> CSVYieldTableWriter.of(context);
		case DCSV -> DCSVYieldTableWriter.of(context);
		case PLOTSY -> PLOTSYYieldTableWriter.of(context);
		case YIELD_TABLE -> TextYieldTableWriter.of(context);
		case TEXT_REPORT -> FullReportYieldTableWriter.of(context);
		default -> throw new IllegalStateException("Unrecognized output format " + outputFormat);
		};

		yieldTableFilePath = builtWriter.getYieldTableFilePath();

		return builtWriter;
	}

	/**
	 * <b>lcl_bDisplayCurrentYear</b>
	 * <p>
	 * Determine if the current year needs to be dumped to the yield table.
	 * <p>
	 * This routine is necessary because there may be gaps in the overall age range that needs to be excluded. This
	 * routine finds such conditions and any others that may require a particular row that would have otherwise been
	 * displayed become suppressed.
	 *
	 * @param rowContext the context of the row in question
	 */
	private boolean rowIsToBeGenerated(YieldTableRowContext rowContext) {

		var doDisplayRow = true;
		String reasonNotDisplayed = null;

		if (rowContext.getCurrentTableYear() == null) {
			reasonNotDisplayed = MessageFormat.format("current year {0} is null", rowContext.getCurrentTableYear());
			doDisplayRow = false;
		}

		// Does the current year lie in the gap (if any)?
		if (doDisplayRow //
				&& rowContext.getYearAtGapStart() != null //
				&& rowContext.getCurrentTableYear() > rowContext.getYearAtGapStart() //
				&& rowContext.getCurrentTableYear() < rowContext.getYearAtGapEnd()) {

			reasonNotDisplayed = MessageFormat.format(
					"current year {0} falls in gap [{1}, {2}]", rowContext.getCurrentTableYear(),
					rowContext.getYearAtGapStart(), rowContext.getYearAtGapEnd()
			);
			doDisplayRow = false;
		}

		// Does the current year lie outside of the range (if any)?
		if (doDisplayRow && rowContext.getYearAtStartAge() != null //
				&& (rowContext.getCurrentTableYear() < rowContext.getYearAtStartAge() //
						|| rowContext.getCurrentTableYear() > rowContext.getYearAtEndAge())) {

			reasonNotDisplayed = MessageFormat.format(
					"current year {0} not in year range [{1}, {2}]", rowContext.getCurrentTableYear(),
					rowContext.getYearAtStartAge(), rowContext.getYearAtEndAge()
			);
			doDisplayRow = false;
		}

		if (doDisplayRow) {

			reasonNotDisplayed = "current row is neither age row nor year row";

			doDisplayRow = false;

			if (rowContext.getCurrentYearIsYearRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE)) {
				reasonNotDisplayed = null;
				doDisplayRow = true;
			}

			if (rowContext.getCurrentYearIsAgeRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE)) {
				reasonNotDisplayed = null;
				doDisplayRow = true;
			}

			if (rowContext.getCurrentTableAge() == null || rowContext.getCurrentTableAge() < 0) {
				reasonNotDisplayed = MessageFormat
						.format("current table age {0} is null or less than zero", rowContext.getCurrentTableAge());
				doDisplayRow = false;
			}
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& rowContext.getCurrentTableYear().equals(rowContext.getMeasurementYear())) {
			doDisplayRow = true;
		} else if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& rowContext.getCurrentTableYear().equals(rowContext.getNowYear())) {
			doDisplayRow = true;
		} else if (params.getYearForcedIntoYieldTable() != null
				&& rowContext.getCurrentTableYear().equals(params.getYearForcedIntoYieldTable())) {
			doDisplayRow = true;
		}

		if (!doDisplayRow) {
			logger.info(
					"{}: excluding row for year {} from yield table. Reason: {}",
					rowContext.getLayerReportingInfo() == null ? rowContext.getPolygon()
							: rowContext.getLayerReportingInfo(),
					rowContext.getCurrentTableYear(), reasonNotDisplayed
			);
		}

		return doDisplayRow;
	}

	/**
	 * <b>lcl_PrintYieldTableRow</b>
	 * <p>
	 * Writes <code>row</code> out to <code>writer</code>.
	 *
	 * @param rowContext               the context of the row to be written
	 * @param polygonProjectionsByYear Map of years to projected Polygon values
	 * @param writer                   the target writer
	 * @throws YieldTableGenerationException
	 */
	private void generateYieldTableRow(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear,
			YieldTableWriter<? extends YieldTableRowBean> writer
	) throws YieldTableGenerationException {

		var polygon = rowContext.getPolygon();
		var layer = rowContext.isPolygonTable() ? null : rowContext.getLayerReportingInfo().getLayer();

		var targetAge = rowContext.getCurrentTableAgeToRequest() - rowContext.getLayerAgeOffset();

//		Awaiting the implementation of DCSV...
//
//		Integer DCSVLayerFieldOffset = null;
//		if (!rowContext.isPolygonTable()) {
//			if (rowContext.getLayerReportingInfo().getSourceLayerID() == 0) {
//				DCSVLayerFieldOffset = 0;
//			} else if (rowContext.getLayerReportingInfo().getSourceLayerID() == 1) {
//				DCSVLayerFieldOffset = DCSVField.DCSV_OFld__RS_FIRST - DCSVField.DCSV_OFld__R1_FIRST;
//			}
//		}

		Double percentStockable;
		if (rowContext.isPolygonTable()) {
			percentStockable = polygon.getPercentStockable();
		} else {
			percentStockable = polygon.determineStockabilityByProjectionType(layer.getAssignedProjectionType());
		}

		writer.startNewRecord();

		try {
			writer.recordPolygonAndLayerDetails(this.nextYieldTableNumber, rowContext);

			writer.recordCalendarYearAndLayerAge(rowContext);

			writer.recordSpeciesComposition(rowContext);

			Double secondaryHeight = null;
			try {
				EntityGrowthDetails growthDetails = null;
				EntityVolumeDetails volumeDetails = null;

				try {
					if (rowContext.isPolygonTable()) {
						growthDetails = getProjectedPolygonGrowthInfo(rowContext, polygonProjectionsByYear, targetAge);
						volumeDetails = getProjectedPolygonVolumes(rowContext, polygonProjectionsByYear, targetAge);
					} else {
						growthDetails = getProjectedLayerStandGrowthInfo(
								rowContext, polygonProjectionsByYear, layer, targetAge
						);
						volumeDetails = getProjectedLayerStandVolumes(
								rowContext, polygonProjectionsByYear, layer, targetAge
						);
					}

					if (Utils.safeGet(growthDetails.basalArea()) <= 0
							|| Utils.safeGet(growthDetails.treesPerHectare()) <= 0.0) {
						// since one or both of basal area and tph are null, null out diameter, too.
						growthDetails = new EntityGrowthDetails(
								growthDetails.siteIndex(), growthDetails.dominantHeight(), growthDetails.loreyHeight(),
								null, growthDetails.treesPerHectare(), growthDetails.basalArea()
						);
					}
				} catch (StandYieldCalculationException e) {
					logger.warn(
							"{}: unable to get growth or volume details{}", polygon,
							e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					);

					// Continue, knowing the growthDetails and/or volumeDetails may be null.
				}

				if (context.getParams()
						.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)
						&& !rowContext.isPolygonTable()) {

					var layerSp0sByPercent = layer.getSp0sByPercent();
					if (layerSp0sByPercent.size() > 1) {
						var secondarySp0 = layerSp0sByPercent.get(1);
						if (!secondarySp0.getSpeciesByPercent().isEmpty()) {
							var secondarySp64 = secondarySp0.getSpeciesByPercent().get(0);
							var speciesGrowthDetails = getProjectedLayerSpeciesGrowthInfo(
									rowContext, polygonProjectionsByYear, secondarySp64, targetAge
							);
							secondaryHeight = speciesGrowthDetails.dominantHeight();
						}
					}
				}

				Double dominantHeight = null;
				if (rowContext.isPolygonTable()) {
					var primaryLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
					if (primaryLayer != null) {
						dominantHeight = primaryLayer.determineLeadingSiteSpeciesHeight(targetAge);
					} else {
						logger.warn(
								"{}: unable to get leading species dominant height since polygon has no primary layer",
								polygon
						);
					}
				} else {
					dominantHeight = layer.determineLeadingSiteSpeciesHeight(targetAge);
				}

				// if (!writer.isCurrentlyWritingCategory(Category.CFSBIOMASS)) {
				writer.recordSiteInformation(
						percentStockable, growthDetails != null ? growthDetails.siteIndex() : null, dominantHeight,
						secondaryHeight
				);
				// }

				if (growthDetails != null && volumeDetails != null) {

					// if (!writer.isCurrentlyWritingCategory(Category.CFSBIOMASS)) {
					writer.recordGrowthDetails(growthDetails, volumeDetails);
					// }
					if (!rowContext.isPolygonTable()) {
						if (rowContext.getPolygonProjectionState().getFirstYearYieldsDisplayed(layer) == null
								&& growthDetails.basalArea() != null) {

							rowContext.getPolygonProjectionState()
									.setFirstYearYieldsDisplayed(layer, rowContext.getCurrentTableYear());
						}

						if (writer.isCurrentlyWritingCategory(Category.SPECIES_MOFVOLUME)) {
							int spIndex = 1;
							for (Species sp64 : layer.getSp64sByPercent()) {

								var mofBiomassFactor = BecZoneMethods
										.mofBiomassCoefficient(layer.getPolygon().getBecZone(), sp64.getSpeciesCode());

								var speciesVolumeDetails = getProjectionLayerSpeciesVolumes(
										rowContext, polygonProjectionsByYear, sp64, targetAge, mofBiomassFactor
								);

								writer.recordPerSpeciesVolumeInfo(
										spIndex++, speciesVolumeDetails.getLeft(), speciesVolumeDetails.getRight()
								);
							}
						}
					}

					if (writer.isCurrentlyWritingCategory(Category.CFSBIOMASS)) {
						CfsBiomassVolumeDetails cfsBiomass;
						if (rowContext.isPolygonTable()) {
							cfsBiomass = CfsBiomassCalculator
									.calculateBiomassPolygonVolumeDetails(volumeDetails, polygon);
						} else {
							cfsBiomass = CfsBiomassCalculator
									.calculateBiomassLayerVolumeDetails(volumeDetails, polygon, layer);
						}
						writer.recordCfsBiomassDetails(volumeDetails, cfsBiomass);
					}
				}

				if (context.getParams().containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)) {
					if (rowContext.getCurrentTableYear() == null) {
						throw new IllegalStateException("CurrentTableYear is null in generateYieldTableRow");
					}

					Integer currentTableYear = rowContext.getCurrentTableYear();

					String projectionMode;
					if (currentTableYear == rowContext.getMeasurementYear()) {
						projectionMode = "Ref";
					} else if (currentTableYear == rowContext.getNowYear()) {
						projectionMode = "Crnt";
					} else if (currentTableYear.equals(params.getYearForcedIntoYieldTable())) {
						projectionMode = "Spcl";
					} else if (rowContext.getYearAtDeath() != null && currentTableYear >= rowContext.getYearAtDeath()) {
						projectionMode = "Atck";
					} else if (currentTableYear < rowContext.getMeasurementYear()) {
						projectionMode = "Back";
					} else {
						projectionMode = "Frwd";
					}

					writer.recordMode(projectionMode);
				}

				// if the format is text report we need to record the dominant species at this age for the Site Curve
				// table from the report
				if (context.getParams().getOutputFormat() == OutputFormat.TEXT_REPORT) {
					String dominantSpeciesCode = null;
					Layer primaryLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
					if (primaryLayer != null
							&& rowContext.getPolygonProjectionState().layerWasProjected(primaryLayer)) {
						// look through the sp0s as supplied until you find the dominant one, then record the sp0 code
						// to the writer
						// this is to replace the overkill from V7Ext_GetProjectedLayerGroupGrowthInfo which is only
						// used for WinVDYP7 to determine the dominant species at any given age increment
						// calling obtain stand yield is slight overkill here but it is the least code duplication
						// there is a fiar bit of boiler plate to access a specifics stand information
						for (Stand sp0 : primaryLayer.getSp0sAsSupplied()) {
							LayerYields yield = obtainStandYield(
									rowContext, polygonProjectionsByYear, primaryLayer, sp0,
									rowContext.getCurrentTableAge()
							);
							if (yield.bYieldsPredicted() && yield.isDominantSp0()) {
								dominantSpeciesCode = yield.sp0Name();
								break; // exit at the first dominant species found
							}
						}
					}
					if (dominantSpeciesCode != null) {
						writer.recordDominantSpeciesByAge(rowContext.getCurrentTableAge(), dominantSpeciesCode);
					}
				}
			} catch (StandYieldCalculationException e) {
				logger.warn(
						"{}: encountered StandYieldCalculationException during yield table row generation{}",
						layer == null ? polygon : layer, e.getMessage() == null ? "" : ": " + e.getMessage()
				);
			}
		} finally {
			writer.endRecord(rowContext);
		}
	}

	private double addSafe(double x, double y) {
		return (x < 0) ? y : x + y;
	}

	/**
	 * <b>V7Ext_GetProjectedPolygonGrowthInfo</b>
	 * <p>
	 * Obtains the density, height, basal area and diameter information for the entire polygon at the age requested.
	 * <p>
	 * <b>NOTES</b>
	 * <p>
	 * Note that certain layers may not have been projected. As a result, those layers will not be included or
	 * considered in the returned polygon values.
	 * <p>
	 * When supplying a Total Age to return Growth Information, the Total Age for a polygon is considered to be the
	 * Total Age of the Primary Layer.
	 * <p>
	 * The returned values for this routine always reflect:
	 * <ul>
	 * <li>For Site Index, Dominant Height and Lorey Height: These values are the same as for the Primary Layer.
	 *
	 * <li>For TPH, Basal Area and Diameter: These values are based on the aggregate of all projected layers.
	 * </ul>
	 *
	 * @param rowContext               the row object into which the growth information is written
	 * @param polygonProjectionsByYear the result of the projection of the polygon and year given in <code>row</code>.
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedPolygonGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, int totalAge
	) throws StandYieldCalculationException {

		if (totalAge < Vdyp7Constants.MIN_SPECIES_AGE || totalAge > Vdyp7Constants.MAX_SPECIES_AGE) {
			throw new StandYieldCalculationException(
					StandYieldMessageKind.AGE_OUT_OF_RANGE, Vdyp7Constants.MIN_SPECIES_AGE,
					Vdyp7Constants.MAX_SPECIES_AGE
			);
		}

		var primaryLayer = rowContext.getPolygon().getPrimaryLayer();
		if (primaryLayer == null) {
			throw new StandYieldCalculationException(new LayerMissingException(LayerType.PRIMARY));
		}

		var primaryLayerAge0Year = primaryLayer.determineYearAtAge(0);

		Double siteIndex = null;
		Double dominantHeight = null;
		Double loreyHeight = null;

		double totalTreesPerHectare = 0;
		double totalBasalArea = 0;

		for (var layer : rowContext.getPolygon().getLayers().values()) {

			if (rowContext.getPolygonProjectionState().layerWasProjected(layer)) {

				var layerAge0Year = layer.determineYearAtAge(0);
				int ageOffset = primaryLayerAge0Year - layerAge0Year;
				var ageToRequest = totalAge + ageOffset;

				if (ageToRequest >= 0) {
					var layerGrowthInfo = getProjectedLayerStandGrowthInfo(
							rowContext, polygonProjectionsByYear, layer, ageToRequest
					);

					if (layer.getAssignedProjectionType() == ProjectionTypeCode.PRIMARY) {
						siteIndex = layerGrowthInfo.siteIndex();
						dominantHeight = layerGrowthInfo.dominantHeight();
						loreyHeight = layerGrowthInfo.loreyHeight();
					}

					if (layerGrowthInfo.treesPerHectare() != null && layerGrowthInfo.basalArea() != null) {
						totalTreesPerHectare += layerGrowthInfo.treesPerHectare();
						totalBasalArea += layerGrowthInfo.basalArea();
					}
				}
			}
		}

		Double totalDiameter = computeDiameter(totalTreesPerHectare, totalBasalArea);

		return new EntityGrowthDetails(
				siteIndex, dominantHeight, loreyHeight, totalDiameter, totalTreesPerHectare, totalBasalArea
		);
	}

	/**
	 * <b>V7Ext_GetProjectedPolygonVolumes</b>
	 * <p>
	 * Obtains the yields summarized at the polygon level for a specific stand total age.
	 * <p>
	 * Note that certain layers may not have been processed. As a result, those layers may result in no volumes despite
	 * the presence of volumes on other layers.
	 *
	 * @param rowContext               the meta values of the row being generated
	 * @param polygonProjectionsByYear the projections, by year, of this polygon
	 * @param targetAge                the age (of the primary layer) for this row
	 * @return the volume details for the given polygon
	 * @throws StandYieldCalculationException
	 */
	private EntityVolumeDetails getProjectedPolygonVolumes(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, int targetAge
	) throws StandYieldCalculationException {

		Double wholeStemVolume = 0.0;
		Double closeUtilizationVolume = 0.0;
		Double cuVolumeLessDecay = 0.0;
		Double cuVolumeLessDecayWastage = 0.0;
		Double cuVolumeLessDecayWastageBreakage = 0.0;

		var polygon = rowContext.getPolygon();
		var primaryLayer = polygon.getPrimaryLayer();

		if (primaryLayer == null) {
			throw new IllegalStateException(
					MessageFormat.format("{0}: primary layer not found", rowContext.getPolygon())
			);
		}

		var primaryLayerYearAtAge = primaryLayer.determineYearAtAge(0);

		for (var layer : polygon.getLayers().values()) {

			if (rowContext.getPolygonProjectionState().polygonWasProjected()) {

				var layerYearAtAge = layer.determineYearAtAge(0);
				var ageOffset = primaryLayerYearAtAge - layerYearAtAge;
				var ageToRequest = targetAge + ageOffset;

				if (ageToRequest > 0) {
					var layerVolumeDetails = getProjectedLayerStandVolumes(
							rowContext, polygonProjectionsByYear, layer, ageToRequest
					);

					if (layerVolumeDetails.wholeStemVolume() != null) {
						if (layerVolumeDetails.wholeStemVolume() != null) {
							wholeStemVolume += layerVolumeDetails.wholeStemVolume();
						}
						if (layerVolumeDetails.closeUtilizationVolume() != null) {
							closeUtilizationVolume += layerVolumeDetails.closeUtilizationVolume();
						}
						if (layerVolumeDetails.cuVolumeLessDecay() != null) {
							cuVolumeLessDecay += layerVolumeDetails.cuVolumeLessDecay();
						}
						if (layerVolumeDetails.cuVolumeLessDecayWastage() != null) {
							cuVolumeLessDecayWastage += layerVolumeDetails.cuVolumeLessDecayWastage();
						}
						if (layerVolumeDetails.cuVolumeLessDecayWastageBreakage() != null) {
							cuVolumeLessDecayWastageBreakage += layerVolumeDetails.cuVolumeLessDecayWastageBreakage();
						}
					}
				}
			}
		}

		return new EntityVolumeDetails(
				wholeStemVolume, closeUtilizationVolume, cuVolumeLessDecay, cuVolumeLessDecayWastage,
				cuVolumeLessDecayWastageBreakage
		);
	}

	/**
	 * <b>V7Int_GetProjectedLayerStandGrowthInfo</b>
	 * <p>
	 * Obtains the density, height, basal area and diameter information for the entire stand at a specific layer at the
	 * age requested.
	 * <p>
	 * <b>NOTES</b>
	 * <p>
	 * This is the internal implementation of the routine 'V7Int_GetProjectedLayerStandGrowthInfo'.
	 * <ul>
	 * <li>Note that certain layers may not have been processed. As a result, those layers may result in no growth info
	 * despite the presence of growth info on other layers.
	 *
	 * <li>Now allow the retrieval of growth information for layers that were not processed.
	 *
	 * <li>Based on direction from Sam Otukol, prevent projected (forward) heights from shrinking below input height.
	 * Also, prevent projected (backwards) heights from exceeding input height.
	 *
	 * <li>Also, copy the TPH and BA forward for non-processed layers.
	 *
	 * <li>Also, fill in Diameter when computable from BA and TPH and not already being returned.
	 *
	 * <li>When BA/TPH is not projected but available on input, copy it over as the projected BA/TPH.
	 *
	 * <li>Relaxed the above constraint so that either of input BA and TPH are copied individually over to the Projected
	 * BA/TPH.
	 *
	 * <li>Relax the constraint so that if the per hectare yields were suppressed (because of IPSCB206), we will also
	 * want input BA/TPH copied forward.
	 *
	 * <li>Added support for optionally turning the substitution of BA/TPH on or off.
	 * </ul>
	 *
	 * @param rowContext               the row object into which the growth information is written
	 * @param polygonProjectionsByYear the result of projecting the polygon (all years)
	 * @param layer                    the source of the growth information
	 * @param totalAge                 year for which the growth information is to be retrieved
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedLayerStandGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, Layer layer,
			int totalAge
	) throws StandYieldCalculationException {

		var leadingSpeciesSp0 = layer.determineLeadingSp0(0);
		var layerWasProcessed = rowContext.getPolygonProjectionState()
				.didRunProjection(layer.getAssignedProjectionType());

		Double siteIndex = null;
		if (leadingSpeciesSp0 != null) {
			siteIndex = leadingSpeciesSp0.getSpeciesGroup().getSiteIndex();
		}

		var projectionYear = layer.determineYearAtAge(totalAge);

		var layerYields = obtainStandYield(rowContext, polygonProjectionsByYear, layer, null, totalAge);

		double diameter = layerYields.diameter();
		double treesPerHectare = layerYields.treesPerHectare();
		double basalArea = layerYields.basalArea125cm();
		double dominantHeight = layerYields.dominantHeight();

		var measurementYear = layer.getPolygon().getReferenceYear();

		if (layer.getDoSuppressPerHAYields()) {
			diameter = treesPerHectare = basalArea = Vdyp7Constants.EMPTY_DECIMAL;
			logger.debug("{}: per-hectare projected values were suppressed", layer);
		}

		if (!layer.getDoSuppressPerHAYields() && layerWasProcessed) {
			if (treesPerHectare == Vdyp7Constants.EMPTY_DECIMAL) {
				treesPerHectare = layer.getTreesPerHectare();
				logger.debug(
						"{}: non-processed layer => using reference trees-per-hectare {}", layer,
						layer.getTreesPerHectare()
				);
			}

			if (basalArea == Vdyp7Constants.EMPTY_DECIMAL) {
				basalArea = layer.getBasalArea();
				logger.debug("{}: non-processed layer => using reference basal area {}", layer, layer.getBasalArea());
			}
		}

		Double suppliedDominantHeight = null;
		if (leadingSpeciesSp0 != null && leadingSpeciesSp0.getSpeciesGroup().getDominantHeight() != null) {
			suppliedDominantHeight = leadingSpeciesSp0.getSpeciesGroup().getDominantHeight();
		}

		// Projected height (going forward) must never be less the reference height.
		// Projected height (going backwards) must never exceed the reference height.

		if (projectionYear != Vdyp7Constants.EMPTY_INT && measurementYear != null && suppliedDominantHeight != null) {

			if (projectionYear >= measurementYear && dominantHeight < suppliedDominantHeight) {

				logger.debug(
						"Projected dominant height {} in {} is less than supplied dominant height {}"
								+ " on or after measurement year {}. Setting dominant height to supplied (\"reference\") height",
						dominantHeight, projectionYear, suppliedDominantHeight, measurementYear
				);

				dominantHeight = suppliedDominantHeight;
			} else if (projectionYear < measurementYear && dominantHeight > suppliedDominantHeight) {

				logger.debug(
						"Projected dominant height {} in {} is greater than supplied dominant height {}"
								+ " before measurement year {}. Setting dominant height to supplied (\"reference\") height",
						dominantHeight, projectionYear, suppliedDominantHeight, measurementYear
				);

				dominantHeight = suppliedDominantHeight;
			}
		}

		// If projected BA/TPH were not supplied but are available on input, copy
		// the available values over to projected. This also implies that diameter should
		// be computed as well.
		//
		// Further suppress this action if there is a VDYP7 Yield row predicted for
		// the year in question.
		//
		// Relax the constraint so that if the per hectare yields were suppressed
		// (because of IPSCB206), we will also want input BA/TPH copied forward.
		//
		// Added support for optionally turning the substitution of BA/TPH on or off.

		if ( (!layerYields.bYieldsPredicted() || layer.getDoSuppressPerHAYields())
				&& context.getParams().containsOption(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION)
				&& basalArea == Vdyp7Constants.EMPTY_DECIMAL && treesPerHectare == Vdyp7Constants.EMPTY_DECIMAL) {

			boolean didCopyBasalArea = false;
			boolean didCopyTreesPerHectare = false;

			if (layer.getBasalArea() != null) {
				diameter = Vdyp7Constants.EMPTY_DECIMAL;
				basalArea = layer.getBasalArea();
				didCopyBasalArea = true;

				logger.debug(
						"{}: projected basal area not available. Copying supplied value {} to projected value", layer,
						layer.getBasalArea()
				);
			}

			if (layer.getTreesPerHectare() != null) {
				diameter = Vdyp7Constants.EMPTY_DECIMAL;
				treesPerHectare = layer.getTreesPerHectare();
				didCopyTreesPerHectare = true;

				logger.debug(
						"{}: projected trees-per-hectare not available. Copying supplied value {} to projected value",
						layer, layer.getTreesPerHectare()
				);
			}

			if (didCopyBasalArea && didCopyTreesPerHectare) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().layer(layer)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_BASAL_AREA_FROM_SUPPLIED_LAYER
								).build()
				);
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().layer(layer)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_TPH_FROM_SUPPLIED_LAYER
								).build()
				);
			} else if (didCopyBasalArea) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().layer(layer)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_BASAL_AREA_FROM_SUPPLIED_LAYER
								).build()
				);
			} else if (didCopyTreesPerHectare) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().layer(layer)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_TPH_FROM_SUPPLIED_LAYER
								).build()
				);
			}
		}

		if (diameter <= 0 && basalArea >= 0 && treesPerHectare >= 0) {
			diameter = computeDiameter(treesPerHectare, basalArea);
			logger.debug(
					"{}: diameter computed from basal area/trees-per-hectare ({}. {} -> {})", layer, basalArea,
					treesPerHectare, diameter
			);
		}

		return new EntityGrowthDetails(
				siteIndex, dominantHeight, layerYields.loreyHeight(), diameter, treesPerHectare, basalArea
		);
	}

	/**
	 * <b>V7Int_GetProjectedLayerStandVolumes</b>
	 * <p>
	 * Obtains the yields summarized at the layer level for a specific stand total age.
	 * <p>
	 * Note that certain layers may not have been processed. As a result, those layers may result in no volumes despite
	 * the presence of volumes on other layers.
	 *
	 * @param rowContext               the meta values of the row being generated
	 * @param polygonProjectionsByYear the projections, by year, of this polygon
	 * @param layer                    the layer for which the projection values are to be retrieved
	 * @param targetAge                the age (of the primary layer) for this row
	 * @return the volume details for the given layer
	 * @throws StandYieldCalculationException
	 */
	private EntityVolumeDetails getProjectedLayerStandVolumes(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, Layer layer,
			int targetAge
	) throws StandYieldCalculationException {

		Double wholeStemVolume = 0.0;
		Double closeUtilizationVolume = 0.0;
		Double cuVolumeLessDecay = 0.0;
		Double cuVolumeLessDecayWastage = 0.0;
		Double cuVolumeLessDecayWastageBreakage = 0.0;

		boolean layerWasProjected = rowContext.getPolygonProjectionState().layerWasProjected(layer);

		if (layerWasProjected && !layer.getDoSuppressPerHAYields()) {
			var layerYields = obtainStandYield(rowContext, polygonProjectionsByYear, layer, null, targetAge);

			wholeStemVolume = layerYields.wholeStemVolume();
			closeUtilizationVolume = layerYields.closeUtilizationVolume();
			cuVolumeLessDecay = layerYields.cuVolumeLessDecay();
			cuVolumeLessDecayWastage = layerYields.cuVolumeLessDecayWastage();
			cuVolumeLessDecayWastageBreakage = layerYields.cuVolumeLessDecayWastageBreakage();
		}

		return new EntityVolumeDetails(
				wholeStemVolume, closeUtilizationVolume, cuVolumeLessDecay, cuVolumeLessDecayWastage,
				cuVolumeLessDecayWastageBreakage
		);
	}

	/**
	 * <b>V7Ext_GetProjectedLayerSpeciesGrowthInfo</b>
	 * <p>
	 * Obtains the density, height, basal area and diameter information for a specific species at a specific layer at
	 * the age requested. <b>NOTES</b>
	 * <ul>
	 * <li>Note that certain layers may not have been processed. As a result, those layers may result in no growth info
	 * despite the presence of growth info on other layers.
	 *
	 * <li>When processing stands which have secondary species which match the primary species at the SP0 and have
	 * different site information supplied, we need to add special case processing which will generate an age and height
	 * based on the original supplied age and height. See Cam's Feb 6, 2004 e-mail for details.
	 *
	 * <li>Return a Dominant Site Species flag for the projected data if the VDYP7 calculations determined that the
	 * requested species was the first supplied SP64 of its corresponding SP0 group and that SP0 group was flagged as
	 * dominant in the output calculations.
	 *
	 * <li>Allow the retrieval of growth information for layers that were not processed.
	 *
	 * <li>Prevent projected (forward) heights from shrinking below input height. Also, prevent projected (backwards)
	 * heights from exceeding input height. This check would apply to the leading site species.
	 * <p>
	 * Also, fill in Diameter when computable from BA and TPH and not already being returned.
	 *
	 * <li>When BA/TPH is not projected but available on input, copy it over as the projected BA/TPH. Pro-rate by
	 * species percent on input.
	 *
	 * <li>Relax the above constraint so that either of input BA and TPH are copied individually over to the Projected
	 * BA/TPH.
	 *
	 * <li>Further suppress this action if there is a VDYP7 Yield row predicted for the year in question.
	 *
	 * <li>Relax the constraint so that if the per hectare yields were suppressed (because of IPSCB206), we will also
	 * want input BA/TPH copied forward.
	 *
	 * <li>Added support for optionally turning the substitution of BA/TPH on or off.
	 * </ul>
	 *
	 * @param rowContext               the meta values of the row being generated
	 * @param polygonProjectionsByYear the projections, by year, of this polygon
	 * @param species                  the Species in question
	 * @param targetAge                the age of the containing Layer
	 *
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedLayerSpeciesGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, Species species,
			int targetAge
	) throws StandYieldCalculationException {

		if (targetAge < Vdyp7Constants.MIN_SPECIES_AGE || Vdyp7Constants.MAX_SPECIES_AGE < targetAge) {
			throw new StandYieldCalculationException(
					StandYieldMessageKind.AGE_OUT_OF_RANGE, Double.valueOf(Vdyp7Constants.MIN_SPECIES_AGE),
					Double.valueOf(Vdyp7Constants.MAX_SPECIES_AGE)
			);
		}

		var stand = species.getStand();
		var layer = stand.getLayer();
		var polygon = layer.getPolygon();

		var measurementYear = polygon.getReferenceYear();

		var layerYields = obtainStandYield(rowContext, polygonProjectionsByYear, layer, stand, targetAge);

		double factor;
		if (species.getSpeciesPercent() > 0 && stand.getSpeciesGroup().getSpeciesPercent() > 0) {
			factor = species.getSpeciesPercent() / stand.getSpeciesGroup().getSpeciesPercent();
		} else {
			factor = 1.0;
		}

		if (species.getNDuplicates() > 1 && species.getSpeciesPercent() > 0) {
			// TODO handle duplicates. The original VDYP7 code appears to be stateful
			// - subsequent calls to this method for the same species will adjust
			// "factor" proportionally to the percentage of that duplicate. But ultimately
			// the calling code doesn't call once per duplicate, so I don't
			// understand how this mechanism works.
		}

		Double siteIndex = species.getSiteIndex();
		Double speciesTotalAge = species.getTotalAge() == null ? null : layerYields.speciesAge();
		Double dominantHeight = species.getDominantHeight() == null ? null : layerYields.dominantHeight();
		double loreyHeight = layerYields.loreyHeight();
		Double diameter = layer.getDoSuppressPerHAYields() ? null : layerYields.diameter();
		Double treesPerHectare = layer.getDoSuppressPerHAYields() ? null : layerYields.treesPerHectare() * factor;
		Double basalArea = layer.getDoSuppressPerHAYields() ? null : layerYields.basalArea125cm() * factor;

		if (species.getTotalAge() != null && species.getSiteIndex() != null && species.getYearsToBreastHeight() != null
				&& (!species.getTotalAge().equals(stand.getSpeciesGroup().getTotalAge())
						|| !species.getSiteIndex().equals(stand.getSpeciesGroup().getSiteIndex()) || !species
								.getYearsToBreastHeight().equals(stand.getSpeciesGroup().getYearsToBreastHeight()))) {

			// Determine the age of the stand based on the difference between
			// the SP0 starting total age and the SP64 starting total age and the
			// age we are projecting to.

			speciesTotalAge += (species.getTotalAge() - stand.getSpeciesGroup().getTotalAge());

			// Compute a species height at that new age.

			dominantHeight = species.determineDominantHeightFromAgeAndSiteIndex();
		}

		// Projected Height (going forward) must never be less the reference
		// height for that species.
		//
		// Projected Height (going backwards) must never exceed the reference
		// height for that species.
		//
		// We only perform this comparison for species in which there is a reference
		// height supplied.

		if (measurementYear != null && species.getSuppliedDominantHeight() != null) {

			var projectionYear = layer.determineYearAtAge(targetAge);

			if (projectionYear >= measurementYear && dominantHeight < species.getSuppliedDominantHeight()) {

				logger.debug(
						"Projected dominant height ({}) in {} is less than supplied dominant height ({}) on or after to measurement year {}."
								+ "Setting dominant height to reference height.",
						dominantHeight, projectionYear, species.getSuppliedDominantHeight(), measurementYear
				);
				dominantHeight = species.getSuppliedDominantHeight();
			} else if (projectionYear < measurementYear && dominantHeight > species.getSuppliedDominantHeight()) {

				logger.debug(
						"Projected dominant height ({}) in {} is greater than supplied dominant height ({}) prior to measurement year {}."
								+ "Setting dominant height to reference height.",
						dominantHeight, projectionYear, species.getSuppliedDominantHeight(), measurementYear
				);
				dominantHeight = species.getSuppliedDominantHeight();
			}
		}

		// If either projected BA or TPH were not supplied but are available on
		// input, copy those not supplied to projected. This also implies that
		// diameter should be computed as well.
		//
		// This logic is always performed whether or not there is a previous error.
		//
		// And: further suppress this action if there is a VDYP7 Yield row predicted
		// for the year in question.
		//
		// And: relax the constraint so that if the per hectare yields were suppressed
		// (because of IPSCB206), we will also want input BA/TPH copied forward.
		//
		// Added support for optionally turning the substitution of BA/TPH on or off.

		var hasResults = layerYields.bYieldsPredicted() && !layer.getDoSuppressPerHAYields();
		var doAllowSubstitution = context.getParams()
				.containsOption(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION);
		if (basalArea == null && treesPerHectare == null && !hasResults && doAllowSubstitution) {

			var didCopyBasalArea = false;
			var didCopyTreesPerHectare = false;
			var percentToUse = species.getSpeciesPercent();

			if (layer.getBasalArea() != null) {
				diameter = null;
				basalArea = layer.getBasalArea() * percentToUse / 100.0;
				didCopyBasalArea = true;

				logger.debug(
						"Projected basal area for species {} not available. Copying input basal area {} protated ({}) to projected value {}",
						species, layer.getBasalArea(), percentToUse, basalArea
				);
			}

			if (layer.getTreesPerHectare() != null) {
				diameter = null;
				treesPerHectare = layer.getTreesPerHectare() * percentToUse / 100.0;
				didCopyTreesPerHectare = true;

				logger.debug(
						"Projected trees-per-hectare for species {} not available. Copying input trees-per-hectare {} protated ({}) to projected value {}",
						species, layer.getTreesPerHectare(), percentToUse, treesPerHectare
				);
			}

			if (didCopyBasalArea && didCopyTreesPerHectare) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().species(species)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_BASAL_AREA_FROM_SUPPLIED_LAYER
								).build()
				);
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().species(species)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_TPH_FROM_SUPPLIED_LAYER
								).build()
				);
			} else if (didCopyBasalArea) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().species(species)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_BASAL_AREA_FROM_SUPPLIED_LAYER
								).build()
				);
			} else if (didCopyTreesPerHectare) {
				layer.getPolygon().addMessage(
						new PolygonMessage.Builder().species(species)
								.details(
										ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
										PolygonMessageKind.COPIED_TPH_FROM_SUPPLIED_LAYER
								).build()
				);
			}
		}

		if (diameter == null && basalArea != null && treesPerHectare != null) {
			diameter = computeDiameter(treesPerHectare, basalArea);
		}

		return new EntityGrowthDetails(siteIndex, dominantHeight, loreyHeight, diameter, treesPerHectare, basalArea);
	}

	/**
	 * V7Ext_GetProjectedLayerSpeciesVolumes
	 * <p>
	 * Obtains the yields summarized at the layer species for a specific stand total age.
	 * <p>
	 * Note that certain layers may not have been processed. As a result, those layers may result in no volumes despite
	 * the presence of volumes on other layers.
	 *
	 * @param rowContext               the yield table row for which the information is being generated
	 * @param polygonProjectionsByYear the results of the projection
	 * @param sp64                     the sp64 in question
	 * @param ageToRequest             the target age of the species
	 * @param mofBiomassFactor         the factor by which to reduce the MoF biomass value
	 *
	 * @return a VolumeInfo record containing the volume information for the given species and age.
	 * @throws StandYieldCalculationException when a failure occurs during yield calculations
	 */
	private Pair<EntityVolumeDetails /* volume */, EntityVolumeDetails /* MoF biomass */>
			getProjectionLayerSpeciesVolumes(
					YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, Species sp64,
					int ageToRequest, double mofBiomassFactor
			) throws StandYieldCalculationException {

		EntityVolumeDetails volumeDetails = null;
		EntityVolumeDetails mofBiomassDetails = null;
		boolean detailsComputed = false;

		double totalBiomassWS = 0.0;
		double totalBiomassCU = 0.0;
		double totalBiomassD = 0.0;
		double totalBiomassDW = 0.0;
		double totalBiomassDWB = 0.0;

		Layer layer = sp64.getStand().getLayer();
		if (!layer.getDoSuppressPerHAYields()) {

			var percentageRatio = sp64.getSpeciesPercent() / sp64.getStand().getSpeciesGroup().getSpeciesPercent();

			// TODO: determine whether the presence of duplicates alters this (vdyp7volumes 1351 - 1360).

			Integer calendarYear = getCalendarYear(layer, ageToRequest);

			var projectedPolygon = polygonProjectionsByYear.get(calendarYear);

			if (projectedPolygon != null) {

				LayerType layerType = getLayerType(layer.getAssignedProjectionType());

				var vdypLayer = projectedPolygon.getLayers().get(layerType);
				var vdypSpecies = vdypLayer.getSpeciesBySp0(sp64.getStand().getSp0Code());

				float wholeStemVolumeFloat = vdypSpecies.getWholeStemVolumeByUtilization().get(UtilizationClass.ALL);
				Double wholeStemVolume = null;
				if (wholeStemVolumeFloat != Vdyp7Constants.EMPTY_DECIMAL) {
					wholeStemVolume = Double.valueOf(wholeStemVolumeFloat) * percentageRatio;
				}

				float cuVolumeFloat = vdypSpecies.getCloseUtilizationVolumeByUtilization().get(UtilizationClass.ALL);
				Double cuVolume = null;
				if (cuVolumeFloat != Vdyp7Constants.EMPTY_DECIMAL) {
					cuVolume = Double.valueOf(cuVolumeFloat) * percentageRatio;
				}

				float cuVolumeLessDecayFloat = vdypSpecies.getCloseUtilizationVolumeNetOfDecayByUtilization()
						.get(UtilizationClass.ALL);
				Double cuVolumeLessDecay = null;
				if (cuVolumeLessDecayFloat != Vdyp7Constants.EMPTY_DECIMAL) {
					cuVolumeLessDecay = Double.valueOf(cuVolumeLessDecayFloat) * percentageRatio;
				}

				float cuVolumeLessDecayWasteFloat = vdypSpecies
						.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(UtilizationClass.ALL);
				Double cuVolumeLessDecayWaste = null;
				if (cuVolumeLessDecayWasteFloat != Vdyp7Constants.EMPTY_DECIMAL) {
					cuVolumeLessDecayWaste = Double.valueOf(cuVolumeLessDecayWasteFloat) * percentageRatio;
				}

				float cuVolumeLessDecayWasteBreakageFloat = vdypSpecies
						.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().get(UtilizationClass.ALL);
				Double cuVolumeLessDecayWasteBreakage = null;
				if (cuVolumeLessDecayWasteBreakageFloat != Vdyp7Constants.EMPTY_DECIMAL) {
					cuVolumeLessDecayWasteBreakage = Double.valueOf(cuVolumeLessDecayWasteBreakageFloat)
							* percentageRatio;
				}

				totalBiomassWS = addSafe(totalBiomassWS, wholeStemVolume * mofBiomassFactor);
				totalBiomassCU = addSafe(totalBiomassCU, cuVolume * mofBiomassFactor);
				totalBiomassD = addSafe(totalBiomassD, cuVolumeLessDecay * mofBiomassFactor);
				totalBiomassDW = addSafe(totalBiomassDW, cuVolumeLessDecayWaste * mofBiomassFactor);
				totalBiomassDWB = addSafe(totalBiomassDWB, cuVolumeLessDecayWasteBreakage * mofBiomassFactor);

				volumeDetails = new EntityVolumeDetails(
						wholeStemVolume, cuVolume, cuVolumeLessDecay, cuVolumeLessDecayWaste,
						cuVolumeLessDecayWasteBreakage
				);

				mofBiomassDetails = new EntityVolumeDetails(
						totalBiomassWS, totalBiomassCU, totalBiomassD, cuVolumeLessDecayWaste, totalBiomassDWB
				);

				detailsComputed = true;
			}
		}

		if (!detailsComputed) {
			volumeDetails = new EntityVolumeDetails(null, null, null, null, null);
			mofBiomassDetails = new EntityVolumeDetails(null, null, null, null, null);
		}

		return new ImmutablePair<EntityVolumeDetails, EntityVolumeDetails>(volumeDetails, mofBiomassDetails);
	}

	/**
	 * <b>V7Int_ObtainStandYield</b>
	 * <p>
	 * Extract the appropriate yield from the stand at the requested age. This method requires that the given layer was
	 * projected.
	 *
	 * @param rowContext
	 * @param layer        the specific layer within the polygon for which yields are to be generated
	 * @param stand        the particular SP0 in the layer for which yield information is requested. If null, summary
	 *                     information for the whole layer is retrieved.
	 * @param ageToRequest the layer total age for which yields are to be generated
	 * @throws YieldTableGenerationException
	 */
	private LayerYields obtainStandYield(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> polygonProjectionsByYear, Layer layer,
			Stand stand, int ageToRequest
	) throws StandYieldCalculationException {

		Validate.notNull(rowContext, "YieldTable.obtainStandYield(): rowContext must not be null");
		Validate.notNull(layer, "YieldTable.obtainStandYield(): layer must not be null");
		Validate.inclusiveBetween(
				Vdyp7Constants.MIN_SPECIES_AGE, Vdyp7Constants.MAX_SPECIES_AGE, ageToRequest,
				MessageFormat.format(
						"YieldTable.obtainStandYield(): ageToRequest value {0} must be at least one", ageToRequest
				)
		);

		Validate.isTrue(
				rowContext.getPolygonProjectionState().layerWasProjected(layer),
				MessageFormat.format("YieldTable.obtainStandYield(): layer {0} must have been projected", layer)
		);

		LayerYields layerYields;

		Integer calendarYear = getCalendarYear(layer, ageToRequest);

		var projectionType = layer.getAssignedProjectionType();
		LayerType layerType = getLayerType(projectionType);

		// Obtain the yields at the requested age.

		// If the initial processing results ended up indicating a
		// code of -14, we will not attempt to obtain projected
		// values. This return code indicates the stand will never
		// reach productive status and therefore there will never
		// be projected values.
		//
		// This is not an error. It indicates the stand lies on
		// some very poor ground for growing trees.

		@SuppressWarnings("unused")
		boolean doReprojectHeight;

		switch (projectionType) {
		case VETERAN:
		case DO_NOT_PROJECT:
		case UNKNOWN:
			doReprojectHeight = true;
			break;
		case DEAD:
		case PRIMARY:
		case REGENERATION:
		case RESIDUAL:
			doReprojectHeight = false;
			break;
		default:
			throw new IllegalStateException("Unknown projection type " + projectionType);
		}

		var initialProcessingResult = rowContext.getPolygonProjectionState()
				.getProcessingResults(ProjectionStageCode.Initial, projectionType);

		if (layerType != null
				&& initialProcessingResult.map(r -> r instanceof FailedToGrowYoungStandException).orElse(false)) {
			throw new StandYieldCalculationException(new FailedToGrowYoungStandException());
		}

		// vdyp7core_requestyeardata?
		var projectedPolygon = polygonProjectionsByYear.get(calendarYear);
		if (projectedPolygon != null && layerType != null) {

			Species sp0;
			if (stand == null) {
				sp0 = layer.getSp0sByPercent().get(0).getSpeciesGroup();
			} else {
				sp0 = stand.getSpeciesGroup();
			}

			var projectedLayer = projectedPolygon.getLayers().get(layerType);
			var projectedSp0 = projectedLayer.getSpeciesBySp0(sp0.getSpeciesCode());

			// VDYP7 projects the polygon over the entire requested range of years using some
			// combination of Forward and Back. In VDYP8 we currently -do not- support Back,
			// and so some years may be missing from polygonProjectionsByYear.

			// Shouldn't this always be true because projectedPolygon != null ?
			if (polygonProjectionsByYear.containsKey(calendarYear)) {

				var sp0Name = SP0Name.forText(sp0.getSpeciesCode());
				var ucReportingLevel = context.getParams().getUtils().get(sp0Name);

				layerYields = getYields(
						calendarYear, ucReportingLevel, projectedSp0, stand == null ? projectedLayer : projectedSp0
				);
			} else {
				layerYields = new LayerYields(
						false, false /* not dominant */, sp0.getSpeciesCode(), calendarYear, 0.0, 0.0, 0.0, 0.0, 0.0,
						0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0
				);
			}
		} else {
			var polygon = layer.getPolygon();

			polygon.addMessage(
					new PolygonMessage.Builder().layer(layer)
							.details(
									ReturnCode.ERROR_CORELIBRARYERROR, MessageSeverityCode.WARNING,
									PolygonMessageKind.NO_PROJECTED_DATA, projectionType, calendarYear
							).build()
			);

			layerYields = new LayerYields(
					false, false /* not dominant */, null, calendarYear, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
					0.0, 0.0, 0.0, 0.0, 0.0, 0
			);
		}

		// TODO: handle "doReprojectHeight".

		return layerYields;
	}

	private LayerYields getYields(
			int calendarYear, UtilizationClassSet ucReportingLevel, VdypSpecies projectedSp0,
			VdypUtilizationHolder entity
	) {

		double totalAge = Vdyp7Constants.EMPTY_DECIMAL;
		double dominantHeight = Vdyp7Constants.EMPTY_DECIMAL;
		double siteIndex = Vdyp7Constants.EMPTY_DECIMAL;
		int siteCurve = Vdyp7Constants.EMPTY_INT;
		boolean isDominantSpecies = projectedSp0.getSite().isPresent();

		if (isDominantSpecies) {
			var site = projectedSp0.getSite().get();

			totalAge = site.getAgeTotal().map(v -> v.doubleValue()).orElse(null);
			dominantHeight = site.getHeight().map(v -> v.doubleValue()).orElse(null);
			siteIndex = site.getSiteIndex().map(v -> v.doubleValue()).orElse(null);
			siteCurve = site.getSiteCurveNumber().orElse(null);
		}

		var treesPerHectare = ucReportingLevel.sumOf(entity.getTreesPerHectareByUtilization());
		var wholeStemVolume = ucReportingLevel.sumOf(entity.getWholeStemVolumeByUtilization());
		var closeUtilizationVolume = ucReportingLevel.sumOf(entity.getCloseUtilizationVolumeByUtilization());
		var cuVolumeLessDecay = ucReportingLevel.sumOf(entity.getCloseUtilizationVolumeNetOfDecayByUtilization());
		var cuVolumeLessDecayWastage = ucReportingLevel
				.sumOf(entity.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization());
		var cuVolumeLessDecayWastageBreakage = ucReportingLevel
				.sumOf(entity.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization());

		var basalArea75cmPlus = UtilizationClassSet._7_5.sumOf(entity.getBaseAreaByUtilization());
		var basalArea125cmPlus = UtilizationClassSet._12_5.sumOf(entity.getBaseAreaByUtilization());

		double diameter;
		if (basalArea75cmPlus > 0 && treesPerHectare > 0) {
			diameter = BaseAreaTreeDensityDiameter.quadMeanDiameter(
					Double.valueOf(basalArea75cmPlus).floatValue(), Double.valueOf(treesPerHectare).floatValue()
			);
		} else {
			diameter = Vdyp7Constants.EMPTY_DECIMAL;
		}

		var reportedStandPercent = projectedSp0.getPercentGenus();

		double loreyHeight = entity.getLoreyHeightByUtilization().get(UtilizationClass.ALL);
		if (ucReportingLevel == UtilizationClassSet._4_0 /* i.e., "ALL" + "SMALL" */) {
			loreyHeight += entity.getLoreyHeightByUtilization().get(UtilizationClass.SMALL);
		}

		return new LayerYields(
				true, isDominantSpecies, projectedSp0.getGenus(), calendarYear, totalAge, dominantHeight, loreyHeight,
				siteIndex, diameter, treesPerHectare, wholeStemVolume, closeUtilizationVolume, cuVolumeLessDecay,
				cuVolumeLessDecayWastage, cuVolumeLessDecayWastageBreakage, basalArea75cmPlus, basalArea125cmPlus,
				reportedStandPercent, siteCurve
		);
	}

	private LayerType getLayerType(ProjectionTypeCode projectionType) {

		LayerType layerType;
		switch (projectionType) {
		case VETERAN:
			layerType = LayerType.PRIMARY;
			break;
		case DEAD:
		case PRIMARY:
		case REGENERATION:
		case RESIDUAL:
			layerType = LayerType.PRIMARY;
			break;
		case DO_NOT_PROJECT:
		case UNKNOWN:
			layerType = null;
			break;
		default:
			throw new IllegalStateException("Unknown projection type " + projectionType);
		}

		return layerType;
	}

	private int getCalendarYear(Layer layer, int ageToRequest) throws StandYieldCalculationException {

		var projectionType = layer.getAssignedProjectionType();

		// Determine the age to use based on the Projection Type.
		//
		// Dead Layers:
		// If we happen to be requesting information for the dead layer,
		// clamp the age of the request so that it does not exceed the
		// age at death since the projection ended at the year of death.
		//
		// This implies that all yield curves flatten at year of death.
		//
		// Vet Layers:
		// Vet layers are clamped to Reference Year/Age. This implies that
		// a vet layer has a flat yield curve.

		Double ageToUse = Double.valueOf(ageToRequest);

		switch (projectionType) {
		case DEAD: {
			if (layer.getAgeAtDeath() != null && ageToRequest > layer.getAgeAtDeath()) {
				ageToUse = layer.getAgeAtDeath();
				logger.debug("{}: dead layer, so clamping requested age to use to {}", layer, ageToUse);
			}
			break;
		}

		case VETERAN: {
			Integer referenceYear = layer.getPolygon().getReferenceYear();
			ageToUse = layer.determineLayerAgeAtYear(referenceYear);

			logger.debug("{}: veteran layer, so clamping requested age to use to reference age of {}", layer, ageToUse);
			break;
		}

		default:
			// use the requested age
			break;
		}

		Integer calendarYear = null;
		if (ageToUse != null) {
			calendarYear = layer.determineYearAtAge(ageToUse);
		}
		if (calendarYear == null || calendarYear < 0) {
			throw new StandYieldCalculationException(StandYieldMessageKind.YEAR_OUT_OF_RANGE, calendarYear);
		}

		return calendarYear;
	}

	private static Double computeDiameter(Double treesPerHectare, Double basalArea) {
		if (treesPerHectare != null && treesPerHectare < 1.0e6 && basalArea != null && basalArea < 1.0e6) {
			return Math.sqrt(basalArea / treesPerHectare / Vdyp7Constants.PI_40K);
		} else {
			return null;
		}
	}

	public InputStream getAsStream() {
		try {
			return new FileInputStream(yieldTableFilePath.toFile());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void close() {
		Utils.close(writer, "YieldTable.writer");
		writer = null;
	}
}
