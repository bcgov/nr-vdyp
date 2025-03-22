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

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.StandYieldCalculationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
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
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class YieldTable implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(YieldTable.class);

	private final ProjectionContext context;
	private final ValidatedParameters params;

	private YieldTableWriter<? extends YieldTableRowValues> writer;
	private Path yieldTableFilePath;

	private int yieldTableCount = 0;

	private YieldTable(ProjectionContext context) throws YieldTableGenerationException {
		this.context = context;
		this.params = context.getValidatedParams();
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
					"VDYP.CTR", polygon, state, projectionType
			);
			for (var e : componentResultsByYear.entrySet()) {
				var year = e.getKey();
				VdypPolygon forwardVdypPolygon = e.getValue();

				resultsByYear.put(year, forwardVdypPolygon);
			}
		}

		if (state.didRunProjectionStage(ProjectionStageCode.Back, projectionType)) {
			var componentResultsByYear = getComponentProjectionResultsByYear(
					"VDYPBACK.CTR", polygon, state, projectionType
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
				polygon.getMapSheet(), polygon.getPolygonNumber(), 0 /* expect any year */
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
				"Expected, but did not find, a polygon {0} in the output of Forward", polygon.getPolygonNumber()
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
				if (rowContext.isPolygonTable()) {
					growthDetails = getProjectedPolygonGrowthInfo(rowContext, projectedPolygons, targetAge);
				} else {
					growthDetails = getProjectedLayerStandGrowthInfo(rowContext, projectedPolygons, layer, targetAge);

					var layerSp0sByPercent = layer.getSp0sByPercent();
					if (layerSp0sByPercent.size() > 1 && context.getValidatedParams().containsOption(
							ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
					)) {
						var secondarySp0 = layerSp0sByPercent.get(1);
						if (secondarySp0.getSpeciesByPercent().size() > 0) {
							var secondarySp64 = secondarySp0.getSpeciesByPercent().get(0);
							getProjectedLayerSpeciesGrowthInfo(rowContext, projectedPolygons, secondarySp64, targetAge);
						}
					}
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

				writer.writeProjectionGrowthInfo();

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

	private static Double computeDiameter(Double treesPerHectare, Double basalArea) {
		if (treesPerHectare != null && treesPerHectare < 1.0e6 && basalArea != null && basalArea < 1.0e6) {
			return Math.sqrt(basalArea / treesPerHectare / Vdyp7Constants.PI_40K);
		} else {
			return null;
		}
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

	private void getProjectedLayerSpeciesGrowthInfo(
			YieldTableRowContext rowContext, Map<Integer, VdypPolygon> projectedPolygons, Species secondarySp64,
			int targetAge
	) {
		// TODO Auto-generated method stub

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
		Validate.notNull(stand, "YieldTable.obtainStandYield(): stand must not be null");
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

		double ageToUse = ageToRequest;

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

		Integer calendarYear = layer.determineYearAtAge(ageToUse);
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

		var sp0 = stand.getSpeciesGroup();

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
		if (layerType != null) {
			if (runCode == -14) {
				throw new StandYieldCalculationException(-14 /* ?!? */);
			}

			// VDYP7 projects the polygon over the entire requested range of years using some
			// combination of Forward and Back. In VDYP8 we currently -do not- support Back,
			// and so some years may be missing from projectedPolygons.

			if (projectedPolygons.containsKey(calendarYear)) {
				var projectedPolygon = projectedPolygons.get(calendarYear);

				var projectedLayer = projectedPolygon.getLayers().get(layerType);
				VdypSpecies projectedSp0 = projectedLayer.getSpeciesBySp0(sp0.getSpeciesCode());

				projectedSp0.getSite().ifPresent(s -> {
					var totalAge = s.getAgeTotal().map(a -> a).orElse(null);
					var dominantHeight = s.getHeight().map(v -> v).orElseGet(null);
					var siteIndex = s.getSiteIndex().map(i -> i).orElseGet(null);
					var siteCurve = s.getSiteCurveNumber().map(c -> c).orElseGet(null);
					var ageAtBreastHeight = s.getYearsAtBreastHeight().map(y -> y).orElseGet(null);

					System.out.println("");
				});
			} else {

			}
		}

		return new LayerYields(
				doReprojectHeight, doReprojectHeight, runCode, runCode, ageToUse, ageToUse, ageToUse, ageToUse,
				ageToUse, ageToUse, ageToUse, ageToUse, ageToUse, ageToUse, ageToUse, ageToUse, ageToUse, ageToUse,
				runCode
		);
	}

	public InputStream getAsStream() {
		try {
			return new FileInputStream(yieldTableFilePath.toFile());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
		writer = null;
	}
}
