package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.StandYieldCalculationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationClassSet;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.Utils;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.forward.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.forward.ForwardDataStreamReader;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

public class YieldTable implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(YieldTable.class);

	private final ProjectionContext context;
	private final ValidatedParameters params;

	private YieldTableWriter<? extends YieldTableRowValues> writer;
	private Path yieldTableFilePath;

	private int yieldTableCount = 0;

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
			Polygon polygon, PolygonProjectionState state, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		var projectionResults = readProjectionResults(polygon, state, ProjectionTypeCode.PRIMARY);

		generateYieldTable(polygon, projectionResults, state, null, doGenerateDetailedTableHeader);
	}

	/**
	 * <b>lcl_GenerateYieldTable</b>
	 * <p>
	 * Generate a single yield table.
	 * <p>
	 *
	 * @param polygon                       the polygon for which a yield table is to be generated
	 * @param polygonAfterProjection        the result of projecting the polygon
	 * @param layerReportingInfo            the layer of the polygon. May be null, indicating that the yield table
	 *                                      summarizes information at the polygon level only.
	 * @param state                         the current state of the (completed) projection of <code>polygon</code>
	 * @param doGenerateDetailedTableHeader if true, displays all the expected details in the table header. If false,
	 *                                      only the table number is generated.
	 */
	public void generateYieldTableForPolygonLayer(
			Polygon polygon, PolygonProjectionState state, LayerReportingInfo layerReportingInfo,
			boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		if (layerReportingInfo == null) {
			generateYieldTableForPolygon(polygon, state, doGenerateDetailedTableHeader);
		} else {
			var polygonProjectionResults = readProjectionResults(
					polygon, state, layerReportingInfo.getProcessedAsVDYP7Layer()
			);
			generateYieldTable(
					polygon, polygonProjectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader
			);
		}
	}

	public void generateCfsBiomassTableForPolygon(
			Polygon polygon, PolygonProjectionState state, boolean doGenerateDetailedTableHeader
	) {
		generateCfsBiomassTable(polygon, state, null, doGenerateDetailedTableHeader);
	}

	public void generateCfsBiomassTable(
			Polygon polygon, PolygonProjectionState state, LayerReportingInfo layerReportingInfo,
			boolean doGenerateDetailedTableHeader
	) {
		// TODO Implement this
	}

	public void endGeneration() throws YieldTableGenerationException {
		writer.writeTrailer();
	}

	public InputStream getAsInputStream() {
		try {
			return new BufferedInputStream(new FileInputStream(writer.getYieldTableFilePath().toFile()));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					MessageFormat.format(
							"Yield table path {} not found, despite our creating it", writer.getYieldTableFilePath()
					)
			);
		}
	}

	private void generateYieldTable(
			Polygon polygon, Map<Integer, VdypPolygon> polygonProjectionResults, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		writer.writePolygonTableHeader(
				polygon, Optional.ofNullable(layerReportingInfo), doGenerateDetailedTableHeader, yieldTableCount
		);

		YieldTableRowIterator rowIterator = new YieldTableRowIterator(context, polygon, state, layerReportingInfo);
		while (rowIterator.hasNext()) {

			YieldTableRowContext rowContext = rowIterator.next();
			if (rowIsToBeGenerated(rowContext)) {
				generateYieldTableRow(rowContext, polygonProjectionResults, writer);
			}
		}

		writer.writePolygonTableTrailer(yieldTableCount);

		yieldTableCount += 1;
	}

	private YieldTableWriter<? extends YieldTableRowValues> buildYieldTableWriter(OutputFormat outputFormat)
			throws YieldTableGenerationException {

		YieldTableWriter<? extends YieldTableRowValues> writer;

		switch (outputFormat) {
		case CSV_YIELD_TABLE:
			writer = CSVYieldTableWriter.of(context);
			break;
		case DCSV:
			writer = DCSVYieldTableWriter.of(context);
			break;
		case PLOTSY:
			writer = PLOTSYYieldTableWriter.of(context);
			break;
		case YIELD_TABLE:
			writer = TextYieldTableWriter.of(context);
			break;
		default:
			throw new IllegalStateException("Unrecognized output format " + outputFormat);
		}

		yieldTableFilePath = writer.getYieldTableFilePath();

		return writer;
	}

	private Map<Integer, VdypPolygon>
			readProjectionResults(Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType)
					throws YieldTableGenerationException {

		var resultsByYear = new HashMap<Integer, VdypPolygon>();

		if (state.didRunProjectionStage(ProjectionStageCode.Forward, projectionType)) {
			var componentResultsByYear = getComponentProjectionResultsByYear(
					Vdyp7Constants.FORWARD_CONTROL_FILE_NAME, polygon, state, projectionType
			);
			for (var e : componentResultsByYear.entrySet()) {
				var year = e.getKey();
				VdypPolygon forwardVdypPolygon = e.getValue();

				resultsByYear.put(year, forwardVdypPolygon);
			}
		}

		if (state.didRunProjectionStage(ProjectionStageCode.Back, projectionType)) {
			var componentResultsByYear = getComponentProjectionResultsByYear(
					Vdyp7Constants.BACK_CONTROL_FILE_NAME, polygon, state, projectionType
			);
			for (var e : componentResultsByYear.entrySet()) {
				var year = e.getKey();
				VdypPolygon backVdypPolygon = e.getValue();

				if (resultsByYear.containsKey(year)) {
					throw new IllegalStateException(
							MessageFormat.format(
									"{0}: contains both FORWARD and BACK results for the same year {1}", polygon,
									e.getKey()
							)
					);
				} else {
					resultsByYear.put(year, backVdypPolygon);
				}
			}
		}

		return resultsByYear;
	}

	private Map<Integer, VdypPolygon> getComponentProjectionResultsByYear(
			String controlFileName, Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType
	) throws YieldTableGenerationException {

		var parser = new ForwardControlParser();

		Path stepExecutionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

		var vdypControlFileResolver = new FileSystemFileResolver(stepExecutionFolder);

		var expectedPolygonIdentifier = new PolygonIdentifier(
				polygon.getMapSheet(), polygon.getPolygonNumber(), polygon.getDistrict(), 0 /* expect any year */
		);

		try (var is = vdypControlFileResolver.resolveForInput(controlFileName)) {

			Map<String, Object> controlMap = parser.parse(is, vdypControlFileResolver, new HashMap<>());

			Object polygonFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name());
			Object speciesFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name());
			Object utilizationsFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name());

			var readerControlMap = new HashMap<String, Object>();
			readerControlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), polygonFileLocation);
			readerControlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), speciesFileLocation);
			readerControlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), utilizationsFileLocation);

			var absolutePathFileResolver = new FileSystemFileResolver();
			readerControlMap.put(
					ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
					new VdypPolygonParser()
							.map(polygonFileLocation.toString(), absolutePathFileResolver, readerControlMap)
			);
			readerControlMap.put(
					ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
					new VdypSpeciesParser()
							.map(speciesFileLocation.toString(), absolutePathFileResolver, readerControlMap)
			);
			readerControlMap.put(
					ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
					new VdypUtilizationParser()
							.map(utilizationsFileLocation.toString(), absolutePathFileResolver, readerControlMap)
			);
			readerControlMap.put(ControlKey.BEC_DEF.name(), controlMap.get(ControlKey.BEC_DEF.name()));
			readerControlMap.put(ControlKey.SP0_DEF.name(), controlMap.get(ControlKey.SP0_DEF.name()));

			var projectionResultsByYear = new HashMap<Integer, VdypPolygon>();

			ForwardDataStreamReader reader = new ForwardDataStreamReader(readerControlMap);

			var vdypPolygon = reader.readNextPolygon();
			while (vdypPolygon.isPresent()
					&& expectedPolygonIdentifier.getBase().equals(vdypPolygon.get().getPolygonIdentifier().getBase())) {

				projectionResultsByYear.put(vdypPolygon.get().getPolygonIdentifier().getYear(), vdypPolygon.get());

				vdypPolygon = reader.readNextPolygon();
			}

			if (projectionResultsByYear.size() == 0) {
				recordPolygonProjectionInformationMissing(polygon);
			}

			if (vdypPolygon.isPresent()) {
				throw new YieldTableGenerationException(
						MessageFormat.format(
								"Expected exactly one polygon in the output of {0}, but saw {1} as well",
								controlFileName, vdypPolygon.get().getPolygonIdentifier()
						)
				);
			}

			return projectionResultsByYear;

		} catch (ResourceParseException | ProcessingException | IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	private void recordPolygonProjectionInformationMissing(Polygon polygon) {
		String message = MessageFormat.format(
				"Expected, but did not find, a polygon {0,number,#} in the output of Forward",
				polygon.getPolygonNumber()
		);
		logger.warn("{}: {}", polygon, message);
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

		if (doDisplayRow && //
				(rowContext.getYearAtStartAge() == null //
						|| rowContext.getCurrentTableYear() < rowContext.getYearAtStartAge() //
						|| rowContext.getCurrentTableYear() > rowContext.getYearAtEndAge())) {

			reasonNotDisplayed = MessageFormat.format(
					"current year {0} not in age range [{1}, {2}]", rowContext.getCurrentTableYear(),
					rowContext.getYearAtStartAge(), rowContext.getYearAtEndAge()
			);
			doDisplayRow = false;
		}

		if (doDisplayRow) {

			reasonNotDisplayed = "current row is neither age row nor year row";

			doDisplayRow = false;

			if (rowContext.getCurrentYearIsYearRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE)) {
				doDisplayRow = true;
			}

			if (rowContext.getCurrentYearIsAgeRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE)) {
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
				&& rowContext.getCurrentTableYear().equals(rowContext.getCurrentYear())) {
			doDisplayRow = true;
		} else if (params.getYearForcedIntoYieldTable() != null
				&& rowContext.getCurrentTableYear().equals(params.getYearForcedIntoYieldTable())) {
			doDisplayRow = true;
		}

		if (!doDisplayRow) {
			logger.debug(
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
	 * @param rowContext the context of the row to be written
	 * @param writer     the target writer
	 * @param projection
	 * @throws YieldTableGenerationException
	 */
	private void generateYieldTableRow(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons,
			YieldTableWriter<? extends YieldTableRowValues> writer
	) throws YieldTableGenerationException {

		var layer = rowContext.isPolygonTable() ? null : rowContext.getLayerReportingInfo().getLayer();

		var targetAge = rowContext.getCurrentTableAgeToRequest() - rowContext.getLayerAgeOffset();

		Integer DCSVLayerFieldOffset = null;
		if (!rowContext.isPolygonTable()) {
			if (rowContext.getLayerReportingInfo().getSourceLayerID() == 0) {
				DCSVLayerFieldOffset = 0;
			} else if (rowContext.getLayerReportingInfo().getSourceLayerID() == 1) {
				DCSVLayerFieldOffset = DCSVField.DCSV_OFld__RS_FIRST - DCSVField.DCSV_OFld__R1_FIRST;
			}
		}

		var becZone = rowContext.getPolygon().getBecZone();
		Double percentStockable;
		if (rowContext.isPolygonTable()) {
			percentStockable = rowContext.getPolygon().getPercentStockable();
		} else {
			percentStockable = rowContext.getPolygon()
					.determineStockabilityByProjectionType(layer.getAssignedProjectionType());
		}

		writer.startNewRecord();

		try {
			writer.recordPerPolygonDetails(rowContext.getPolygon(), this.yieldTableCount);

			writer.recordCalendarYearAndLayerAge(rowContext);

			writer.recordSpeciesComposition(rowContext);

			Double secondaryHeight = null;
			try {
				EntityGrowthDetails growthDetails;
				EntityVolumeDetails volumeDetails;
				if (rowContext.isPolygonTable()) {
					growthDetails = getProjectedPolygonGrowthInfo(rowContext, projectedPolygons, targetAge);
					volumeDetails = getProjectedPolygonVolumes(rowContext, projectedPolygons, targetAge);
				} else {
					growthDetails = getProjectedLayerStandGrowthInfo(rowContext, projectedPolygons, layer, targetAge);
					volumeDetails = getProjectedLayerStandVolumes(rowContext, projectedPolygons, layer, targetAge);

					var layerSp0sByPercent = layer.getSp0sByPercent();
					if (layerSp0sByPercent.size() > 1 && context.getParams().containsOption(
							ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
					)) {
						var secondarySp0 = layerSp0sByPercent.get(1);
						if (secondarySp0.getSpeciesByPercent().size() > 0) {
							var secondarySp64 = secondarySp0.getSpeciesByPercent().get(0);
							var speciesGrowthDetails = getProjectedLayerSpeciesGrowthInfo(
									rowContext, projectedPolygons, secondarySp64, targetAge
							);
							secondaryHeight = speciesGrowthDetails.dominantHeight();
						}
					}
				}

				if (Utils.safeGet(growthDetails.basalArea()) <= 0
						|| Utils.safeGet(growthDetails.treesPerHectare()) <= 0.0) {
					growthDetails = new EntityGrowthDetails(
							growthDetails.siteIndex(), growthDetails.dominantHeight(), growthDetails.loreyHeight(),
							null, growthDetails.treesPerHectare(), growthDetails.basalArea()
					);
				}

				Double dominantHeight;
				if (rowContext.isPolygonTable()) {
					var primaryLayer = rowContext.getPolygon().getLayerByProjectionType(ProjectionTypeCode.PRIMARY);
					dominantHeight = primaryLayer.determineLeadingSiteSpeciesHeight(targetAge);
				} else {
					dominantHeight = layer.determineLeadingSiteSpeciesHeight(targetAge);
				}

				writer.recordSiteInformation(
						percentStockable, growthDetails.siteIndex(), dominantHeight, secondaryHeight
				);

				if (!rowContext.isPolygonTable()
						&& rowContext.getPolygonProjectionState().getFirstYearYieldsDisplayed(layer) == null
						&& growthDetails.basalArea() != null) {
					rowContext.getPolygonProjectionState()
							.setFirstYearYieldsDisplayed(layer, rowContext.getCurrentYear());
				}

				writer.recordGrowthDetails(growthDetails, volumeDetails);

				if (context.getParams().containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)) {
					if (rowContext.getCurrentTableYear() == null) {
						throw new IllegalStateException("CurrentTableYear is null in generateYieldTableRow");
					}

					int currentTableYear = rowContext.getCurrentTableYear();

					String projectionMode;
					if (currentTableYear == rowContext.getMeasurementYear()) {
						projectionMode = "Ref";
					} else if (currentTableYear == rowContext.getCurrentYear()) {
						projectionMode = "Crnt";
					} else if (currentTableYear == rowContext.getCurrentTableYear()) {
						projectionMode = "Spcl";
					} else if (rowContext.getYearAtDeath() != null && currentTableYear >= rowContext.getYearAtDeath()) {
						projectionMode = "Atck";
					} else if (currentTableYear < rowContext.getMeasurementYear()) {
						projectionMode = "Back";
					} else {
						projectionMode = "Fwrd";
					}

					writer.recordMode(projectionMode);
				}
			} catch (StandYieldCalculationException e) {
				logger.warn(
						"{}: encountered StandYieldCalculationException during yield table row generation{}",
						layer == null ? rowContext.getPolygon() : layer,
						e.getMessage() == null ? "" : ": " + e.getMessage()
				);
			}
		} finally {
			writer.endRecord();
		}
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
	 * @param rowContext        the row object into which the growth information is written
	 * @param projectedPolygons the result of the projection of the polygon and year given in <code>row</code>.
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedPolygonGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, int totalAge
	) throws StandYieldCalculationException {

		if (totalAge < Vdyp7Constants.MIN_SPECIES_AGE || totalAge > Vdyp7Constants.MAX_SPECIES_AGE) {
			throw new IllegalArgumentException("getProjectionPolygonGrowthInfo: targetAge");
		}

		var primaryLayer = rowContext.getPolygon().getPrimaryLayer();
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
							rowContext, projectedPolygons, layer, ageToRequest
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

		var result = new EntityGrowthDetails(
				siteIndex, dominantHeight, loreyHeight, totalDiameter, totalTreesPerHectare, totalBasalArea
		);

		return result;
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
	 * @param rowContext       the row object into which the growth information is written
	 * @param projectedPolygon
	 * @param vdypPolygon      the source of the growth information
	 * @param totalAge         year for which the growth information is to be retrieved
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedLayerStandGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, Layer layer, int totalAge
	) throws StandYieldCalculationException {

		var leadingSpeciesSp0 = layer.determineLeadingSp0(0);
		var projectionYear = layer.determineYearAtAge(totalAge);

		Double siteIndex = leadingSpeciesSp0.getSpeciesGroup().getSiteIndex();

		var layerYields = obtainStandYield(rowContext, projectedPolygons, layer, leadingSpeciesSp0, totalAge);

		// siteIndex, Double dominantHeight, Double loreyHeight, Double diameter, Double treesPerHectare, Double
		// basalArea
		return new EntityGrowthDetails(
				siteIndex, layerYields.dominantHeight(), layerYields.loreyHeight(), layerYields.diameter(),
				layerYields.treesPerHectare(), layerYields.basalArea125cm()
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
	 * @param rowContext        the meta values of the row being generated
	 * @param projectedPolygons the projections, by year, of this polygon
	 * @param targetAge         the age (of the primary layer) for this row
	 * @return the volume details for the given polygon
	 * @throws StandYieldCalculationException
	 */
	private EntityVolumeDetails getProjectedPolygonVolumes(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, int targetAge
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

		if (!rowContext.getPolygonProjectionState().didRunProjection()) {
			throw new IllegalStateException(
					MessageFormat.format("{0}: did not run projection", rowContext.getPolygon())
			);
		}

		var primaryLayerYearAtAge = primaryLayer.determineYearAtAge(0);

		for (var layer : polygon.getLayers().values()) {

			if (rowContext.getPolygonProjectionState().didRunProjection()) {

				var layerYearAtAge = layer.determineYearAtAge(0);
				var ageOffset = primaryLayerYearAtAge - layerYearAtAge;
				var ageToRequest = targetAge + ageOffset;

				if (ageToRequest > 0) {
					var layerVolumeDetails = getProjectedLayerStandVolumes(
							rowContext, projectedPolygons, layer, ageToRequest
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
	 * <b>V7Int_GetProjectedLayerStandVolumes</b>
	 * <p>
	 * Obtains the yields summarized at the layer level for a specific stand total age.
	 * <p>
	 * Note that certain layers may not have been processed. As a result, those layers may result in no volumes despite
	 * the presence of volumes on other layers.
	 *
	 * @param rowContext        the meta values of the row being generated
	 * @param projectedPolygons the projections, by year, of this polygon
	 * @param layer             the layer for which the projection values are to be retrieved
	 * @param targetAge         the age (of the primary layer) for this row
	 * @return the volume details for the given layer
	 * @throws StandYieldCalculationException
	 */
	private EntityVolumeDetails getProjectedLayerStandVolumes(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, Layer layer, int targetAge
	) throws StandYieldCalculationException {
		if (!rowContext.getPolygonProjectionState().didRunProjection()) {
			throw new IllegalStateException(
					MessageFormat.format("{0}: did not run projection", rowContext.getPolygon())
			);
		}

		Double wholeStemVolume = 0.0;
		Double closeUtilizationVolume = 0.0;
		Double cuVolumeLessDecay = 0.0;
		Double cuVolumeLessDecayWastage = 0.0;
		Double cuVolumeLessDecayWastageBreakage = 0.0;

		boolean layerWasProjected = rowContext.getPolygonProjectionState().layerWasProjected(layer);

		if (layerWasProjected && !layer.getDoSuppressPerHAYields()) {
			var layerYields = obtainStandYield(rowContext, projectedPolygons, layer, null, targetAge);

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
	 * <li>Now return a Dominant Site Species flag for the projected data if the VDYP7 calculations determined that the
	 * requested species was the first supplied SP64 of its corresponding SP0 group and that SP0 group was flagged as
	 * dominant in the output calculations.
	 *
	 * <li>Now allow the retrieval of growth information for layers that were not processed.
	 *
	 * <li>Based on direction from Sam Otukol, prevent projected (forward) heights from shrinking below input height.
	 * Also, prevent projected (backwards) heights from exceeding input height. This check would apply to the leading
	 * site species.
	 * <p>
	 * Also, fill in Diameter when computable from BA and TPH and not already being returned.
	 *
	 * <li>When BA/TPH is not projected but available on input, copy it over as the projected BA/TPH. Pro-rate by
	 * species percent on input.
	 *
	 * <li>Relaxed the above constraint so that either of input BA and TPH are copied individually over to the Projected
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
	 * @param rowContext        the meta values of the row being generated
	 * @param projectedPolygons the projections, by year, of this polygon
	 * @param species           the Species in question
	 * @param targetAge         the age of the containing Layer
	 *
	 * @throws StandYieldCalculationException
	 */
	private EntityGrowthDetails getProjectedLayerSpeciesGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, Species species, int targetAge
	) throws StandYieldCalculationException {

		if (targetAge < Vdyp7Constants.MIN_SPECIES_AGE || Vdyp7Constants.MAX_SPECIES_AGE > targetAge) {
			throw new StandYieldCalculationException(-2);
		}

		var stand = species.getStand();
		var layer = stand.getLayer();
		var polygon = layer.getPolygon();

		var measurementYear = polygon.getReferenceYear();

		var layerYields = obtainStandYield(rowContext, projectedPolygons, layer, stand, targetAge);

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
				.containsOption(ExecutionOption.DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION);
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
				context.addMessage(
						Level.WARN,
						"Starting values for basal area and trees-per-hectare copied over for QA and alternative model used for Species {}",
						species
				);
			} else if (didCopyBasalArea) {
				context.addMessage(
						Level.WARN,
						"Starting values for basal area copied over for QA and alternative model used for Species {}",
						species
				);
			} else if (didCopyTreesPerHectare) {
				context.addMessage(
						Level.WARN,
						"Starting values for trees-per-hectare copied over for QA and alternative model used for Species {}",
						species
				);
			}
		}

		if (diameter == null && basalArea != null && treesPerHectare != null) {
			diameter = computeDiameter(treesPerHectare, basalArea);
		}

		return new EntityGrowthDetails(siteIndex, dominantHeight, loreyHeight, diameter, treesPerHectare, basalArea);
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
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, Layer layer, Stand stand,
			int ageToRequest
	) throws StandYieldCalculationException {

		Validate.notNull(rowContext, "YieldTable.obtainStandYield(): rowContext must not be null");
		Validate.notNull(layer, "YieldTable.obtainStandYield(): layer must not be null");
		Validate.notNull(
				ageToRequest > 0,
				MessageFormat.format(
						"YieldTable.obtainStandYield(): ageToRequest value {0} must be at least one", ageToRequest
				)
		);

		Validate.isTrue(
				rowContext.getPolygonProjectionState().layerWasProjected(layer),
				MessageFormat.format("YieldTable.obtainStandYield(): layer {0} must have been projected", layer)
		);

		var projectionType = layer.getAssignedProjectionType();

		// Determine the age to use based on the Projection Type.
		//
		// Dead Layers:
		// If we happen to be requesting information for the dead layer,
		// clamp the age of the request so that it does not exceed the
		// age at death. The projection ended at the year of death so
		// the maximum age to request information out of VDYP7CORE is that
		// year.
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
			throw new StandYieldCalculationException(calendarYear == null ? -9 : calendarYear);
		}

		// Obtain the yields at the requested age.

		// If the initial processing results ended up indicating a
		// code of -14, we will not attempt to obtain projected
		// values. This return code indicates the stand will never
		// reach productive status and therefore there will never
		// be projected values.
		//
		// This is not an error. It indicates the stand lies on
		// some very poor ground for growing trees.

		Species sp0;
		if (stand == null) {
			sp0 = layer.getSp0sByPercent().get(0).getSpeciesGroup();
		} else {
			sp0 = stand.getSpeciesGroup();
		}

		boolean doReprojectHeight;
		LayerType layerType;
		switch (projectionType) {
		case VETERAN:
			layerType = LayerType.PRIMARY;
			doReprojectHeight = true;
			break;
		case DEAD:
		case PRIMARY:
		case REGENERATION:
		case RESIDUAL:
			layerType = LayerType.PRIMARY;
			doReprojectHeight = false;
			break;
		case DO_NOT_PROJECT:
		case UNKNOWN:
			layerType = null;
			doReprojectHeight = true;
			break;
		default:
			throw new IllegalStateException("Unknown projection type " + projectionType);
		}

		var initialProcessingResult = rowContext.getPolygonProjectionState()
				.getProcessingResults(ProjectionStageCode.Initial, projectionType);
		var runCode = initialProcessingResult.getRunCode().isPresent() ? initialProcessingResult.getRunCode().get() : 0;

		if (layerType != null && runCode == -14) {
			throw new StandYieldCalculationException(-14 /* ?!? */);
		}

		var projectedPolygon = projectedPolygons.get(calendarYear);
		if (projectedPolygon != null) {
			var projectedLayer = projectedPolygon.getLayers().get(layerType);
			var projectedSp0 = projectedLayer.getSpeciesBySp0(sp0.getSpeciesCode());
			var standYear = calendarYear;

			boolean isDominantSpecies = projectedSp0.getSite().isPresent();

			if (layerType != null) {

				// VDYP7 projects the polygon over the entire requested range of years using some
				// combination of Forward and Back. In VDYP8 we currently -do not- support Back,
				// and so some years may be missing from projectedPolygons.

				if (projectedPolygons.containsKey(calendarYear)) {

					var sp0Name = SP0Name.forText(sp0.getSpeciesCode());
					var ucReportingLevel = context.getParams().getUtils().get(sp0Name);

					double totalAge = Vdyp7Constants.EMPTY_DECIMAL;
					double ageAtBreastHeight = Vdyp7Constants.EMPTY_DECIMAL;
					double dominantHeight = Vdyp7Constants.EMPTY_DECIMAL;
					double siteIndex = Vdyp7Constants.EMPTY_DECIMAL;
					int siteCurve = Vdyp7Constants.EMPTY_INT;

					if (projectedSp0.getSite().isPresent()) {
						var site = projectedSp0.getSite().get();

						totalAge = site.getAgeTotal().map(v -> v.doubleValue()).orElse(null);
						ageAtBreastHeight = site.getYearsAtBreastHeight().map(v -> v.doubleValue()).orElse(null);
						dominantHeight = site.getHeight().map(v -> v.doubleValue()).orElse(null);
						siteIndex = site.getSiteIndex().map(v -> v.doubleValue()).orElse(null);
						siteCurve = site.getSiteCurveNumber().orElse(null);
					}

					var treePerHectare = ucReportingLevel.sumOf(projectedSp0.getTreesPerHectareByUtilization());
					var wholeStemVolume = ucReportingLevel.sumOf(projectedSp0.getWholeStemVolumeByUtilization());
					var closeUtilizationVolume = ucReportingLevel
							.sumOf(projectedSp0.getCloseUtilizationVolumeByUtilization());
					var cuVolumeLessDecay = ucReportingLevel
							.sumOf(projectedSp0.getCloseUtilizationVolumeNetOfDecayByUtilization());
					var cuVolumeLessDecayWastage = ucReportingLevel
							.sumOf(projectedSp0.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization());
					var cuVolumeLessDecayWastageBreakage = ucReportingLevel
							.sumOf(projectedSp0.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization());

					var basalArea75cmPlus = UtilizationClassSet._7_5.sumOf(projectedSp0.getBaseAreaByUtilization());
					var basalArea125cmPlus = UtilizationClassSet._12_5.sumOf(projectedSp0.getBaseAreaByUtilization());

					var diameter = ucReportingLevel.sumOf(projectedSp0.getQuadraticMeanDiameterByUtilization());
					var reportedStandPercent = projectedSp0.getPercentGenus();

					double loreyHeight;
					if (ucReportingLevel == UtilizationClassSet._7_5 /* i.e., "ALL" */) {
						loreyHeight = projectedSp0.getLoreyHeightByUtilization().get(UtilizationClass.ALL);
					} else if (ucReportingLevel == UtilizationClassSet._4_0 /* i.e., "ALL" + "SMALL" */) {
						loreyHeight = projectedSp0.getLoreyHeightByUtilization().get(UtilizationClass.ALL)
								+ projectedSp0.getLoreyHeightByUtilization().get(UtilizationClass.SMALL);
					} else {
						loreyHeight = 0.0;
					}

					return new LayerYields(
							true, isDominantSpecies, sp0.getSpeciesCode(), calendarYear, totalAge, loreyHeight,
							siteIndex, diameter, treePerHectare, wholeStemVolume, closeUtilizationVolume,
							cuVolumeLessDecay, cuVolumeLessDecayWastage, cuVolumeLessDecayWastageBreakage, ageToUse,
							basalArea75cmPlus, basalArea125cmPlus, reportedStandPercent, siteCurve
					);
				}
			} else {
				return new LayerYields(
						false, isDominantSpecies, sp0.getSpeciesCode(), calendarYear, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
						0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0
				);
			}
		} else {
			context.addMessage(
					Level.WARN,
					"{0}: projected data for the {1} layer was not generated at stand age {2}, calendar year {3,number,#}",
					rowContext.getPolygon(), projectionType, ageToUse, calendarYear
			);
		}

		return new LayerYields(
				false, false, null, calendarYear, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0
		);
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
