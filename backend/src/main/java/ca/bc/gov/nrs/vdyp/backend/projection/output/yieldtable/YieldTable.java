package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
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
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class YieldTable {

	private static final Logger logger = LoggerFactory.getLogger(YieldTable.class);

	private final ProjectionContext context;
	private final ValidatedParameters params;

	private int yieldTableCount = 0;

	private YieldTable(ProjectionContext context) {
		this.context = context;
		this.params = context.getValidatedParams();
	}

	public static YieldTable of(ProjectionContext context) throws YieldTableGenerationException {

		return new YieldTable(context);
	}

	public void generateYieldTableForPolygon(
			Polygon polygon, PolygonProjectionState state, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		var polygonAfterProjection = readProjectionResults(polygon, state, ProjectionTypeCode.PRIMARY);

		generateYieldTable(polygon, polygonAfterProjection, state, null, doGenerateDetailedTableHeader);
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

	private void generateYieldTable(
			Polygon polygon, Map<Integer, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>>> polygonProjectionResults,
			PolygonProjectionState state, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException {

		var outputFormat = params.getOutputFormat();

		try (YieldTableWriter writer = buildYieldTableWriter(outputFormat)) {

			yieldTableCount += 1;

			for (var polygonProjectionResultsForYear : polygonProjectionResults.entrySet()) {

				var row = createTableRow(polygon, layerReportingInfo, polygonProjectionResultsForYear.getKey());

				writer.writeHeader(polygon, layerReportingInfo, doGenerateDetailedTableHeader, yieldTableCount);

				YieldTableRowIterator rowIterator = new YieldTableRowIterator(context, row);
				while (rowIterator.hasNext()) {

					row = rowIterator.next();
					if (rowIsToBeGenerated(row)) {
						generateRow(row, polygonProjectionResultsForYear.getValue(), writer);
					}
				}
			}

			writer.writeTrailer(yieldTableCount);
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	private YieldTableWriter buildYieldTableWriter(OutputFormat outputFormat) throws YieldTableGenerationException {

		YieldTableWriter writer;

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

		return writer;
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

	private Map<Integer, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>>>
			readProjectionResults(Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType)
					throws YieldTableGenerationException {

		var resultsByYear = new HashMap<Integer, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>>>();

		if (state.didRunProjectionStage(ProjectionStageCode.Forward, projectionType)) {
			var componentResultsByYear = getComponentProjectionResultsByYear(
					"VDYP.CTR", polygon, state, projectionType
			);
			for (var e : componentResultsByYear.entrySet()) {
				resultsByYear.put(e.getKey(), new ImmutablePair<>(Optional.of(e.getValue()), Optional.empty()));
			}
		}

		if (state.didRunProjectionStage(ProjectionStageCode.Back, projectionType)) {
			var componentResultsByYear = getComponentProjectionResultsByYear(
					"VDYPBACK.CTR", polygon, state, projectionType
			);
			for (var e : componentResultsByYear.entrySet()) {
				var year = e.getKey();
				VdypPolygon backVdypPolygon = e.getValue();
				if (resultsByYear.containsKey(e.getKey())) {
					var forwardVdypPolygon = resultsByYear.get(e.getKey()).getLeft();
					resultsByYear.put(year, new ImmutablePair<>(forwardVdypPolygon, Optional.of(backVdypPolygon)));
				} else {
					resultsByYear.put(year, new ImmutablePair<>(Optional.empty(), Optional.of(backVdypPolygon)));
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

	private YieldTableData createTableRow(Polygon polygon, LayerReportingInfo layerReportingInfo, Integer year)
			throws YieldTableGenerationException {

		var row = new YieldTableData(polygon, layerReportingInfo);

		row.setReferenceYear(year);

		Integer yearOfDeath = null;

		Layer layer;
		if (row.isPolygonTable()) {
			layer = null;

			try {
				double ageAtYear = polygon.determineStandAgeAtYear(row.getReferenceYear());
				row.setReferenceAge((int) ageAtYear);
			} catch (PolygonValidationException e) {
				throw new YieldTableGenerationException(e);
			}

			var primaryLayer = polygon.getPrimaryLayer();
			yearOfDeath = primaryLayer.getYearOfDeath();
			if (yearOfDeath == null) {
				yearOfDeath = polygon.getYearOfDeath();
			}
			
			row.setNumSpecies(0);
		} else {
			layer = layerReportingInfo.getLayer();

			row.setProjectionType(layerReportingInfo.getProcessedAsVDYP7Layer());

			yearOfDeath = layer.getYearOfDeath();
			if (yearOfDeath == null) {
				yearOfDeath = polygon.getYearOfDeath();
			}

			if (row.getProjectionType() == ProjectionTypeCode.DEAD) {

				if (yearOfDeath != null) {
					double layerAgeAtDeath = layer.determineLayerAgeAtYear(yearOfDeath);
					row.setAgeAtDeath((int) layerAgeAtDeath);
				}
			}

			double ageAtYear = layer.determineLayerAgeAtYear(row.getReferenceYear());
			row.setReferenceAge((int) ageAtYear);
			
			row.setNumSpecies(layer.getSp64sAsSupplied().size());
		}

		// In case the polygon has a dead stem layer and the year of death
		// occurred after the reference year, use the year of death as the
		// measurement year.
		//
		// Note that this needs to be an explicitly separate calculation from
		// the section that sets the '.yearAtDeath' member because that attribute
		// should only be set for the Dead Stem Layer. This is so the yield table
		// generator can mark the row as a Year of Death row.

		if (yearOfDeath != null && (row.getReferenceYear() == null || yearOfDeath > row.getReferenceYear())) {

			double relevantAge;
			if (row.isPolygonTable()) {
				try {
					relevantAge = polygon.determineStandAgeAtYear(yearOfDeath);
				} catch (PolygonValidationException e) {
					throw new YieldTableGenerationException(e);
				}
			} else {
				assert layer != null;
				relevantAge = layer.determineLayerAgeAtYear(yearOfDeath);
			}

			row.setMeasurementYear(yearOfDeath);
			row.setMeasurementAge((int) relevantAge);
		} else {
			row.setMeasurementYear(row.getReferenceYear());
			row.setMeasurementAge(row.getReferenceAge());
		}

		assert row.getMeasurementAge() != null && row.getMeasurementYear() != null;

		row.calculateTableRangeInformation(context);

		calculateLayerAgeOffsets(row);

		return row;
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
	 * @param row the row in question
	 */
	private boolean rowIsToBeGenerated(YieldTableData row) {

		var doDisplayRow = true;

		if (row.getYearAtGapStart() != null && row.getYearAtGapEnd() != null
				&& row.getCurrentTableYear() > row.getYearAtGapStart()
				&& row.getCurrentTableYear() < row.getYearAtGapEnd()) {

			doDisplayRow = false;
		}

		if (row.getCurrentTableYear() == null || row.getCurrentTableYear() < row.getYearAtStartAge()
				|| row.getCurrentTableYear() > row.getYearAtEndAge()) {
			doDisplayRow = false;
		}

		if (doDisplayRow) {

			doDisplayRow = false;

			if (row.getCurrentYearIsYearRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE)) {
				doDisplayRow = true;
			}

			if (row.getCurrentYearIsAgeRow()
					&& params.containsOption(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE)) {
				doDisplayRow = true;
			}

			if (row.getCurrentTableAge() == null) {
				doDisplayRow = false;
			}
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& row.getCurrentTableYear().equals(row.getMeasurementYear())) {
			doDisplayRow = true;
		} else if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& row.getCurrentTableYear().equals(row.getCurrentYear())) {
			doDisplayRow = true;
		} else if (params.getYearForcedIntoYearTable() != null
				&& row.getCurrentTableYear().equals(params.getYearForcedIntoYearTable())) {
			doDisplayRow = true;
		}

		if (!doDisplayRow) {
			logger.debug(
					"{}: excluding row for year {} from yield table",
					row.getLayerReportingInfo() == null ? row.getPolygon() : row.getLayerReportingInfo(),
					row.getCurrentTableYear()
			);
		}

		return doDisplayRow;
	}

	/**
	 * <b>lcl_PrintYieldTableRow</b>
	 * <p>
	 * Writes <code>row</code> out to <code>writer</code>.
	 *
	 * @param row    the row to be written
	 * @param writer the target writer
	 * @param pair
	 * @throws YieldTableGenerationException
	 */
	private void generateRow(
			YieldTableData row, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>> pair, YieldTableWriter writer
	) throws YieldTableGenerationException {

		var polygon = row.getPolygon();
		var layer = row.isPolygonTable() ? null : row.getLayerReportingInfo().getLayer();

		var targetAge = row.getCurrentTableAgeToRequest() - row.getLayerAgeOffset();

		Integer DCSVLayerFieldOffset = null;
		if (!row.isPolygonTable()) {
			if (row.getLayerReportingInfo().getSourceLayerID() == 0) {
				DCSVLayerFieldOffset = 0;
			} else if (row.getLayerReportingInfo().getSourceLayerID() == 1) {
				DCSVLayerFieldOffset = DCSVField.DCSV_OFld__RS_FIRST - DCSVField.DCSV_OFld__R1_FIRST;
			}
		}

		var becZone = row.getPolygon().getBecZone();
		Double percentStockable;
		if (row.isPolygonTable()) {
			percentStockable = polygon.getPercentStockable();
		} else {
			percentStockable = polygon.determineStockabilityByProjectionType(layer.getAssignedProjectionType());
		}

		writer.startNewRecord();

		writer.writeCalendarYearAndLayerAge(row);

		writer.writeSpeciesComposition(row);

		writeProjectionGrowthInfo(row, writer, pair, targetAge);
	}

	/**
	 * from yldtable.c lines 3241 - 3356
	 * <p>
	 * 2005/03/28: Added some logic to make the VDYP7CORE more in keeping with how VDYP7Batch generates its yield
	 * tables.
	 * <p>
	 * Comments taken from that source code:
	 * <p>
	 * 2004/11/17: According to Cam's Nov.9, 2004 e-mail, we want the yield table to reflect the ages of the primary
	 * species within the particular layer. Further, there should be no age corrections made to adjust the displayed age
	 * to be relative to the primary layer age.
	 * <p>
	 * 2004/11/25: Further to the above note, the primary species for a layer is the species VDYP7CORE determines to be
	 * the primary species at reference age rather than the leading species as supplied.
	 * <p>
	 * 2007/02/10: According recent telephone conversations and e-mails, we are going to disable the layer offset
	 * calculations. The main reason for this is because the years for which you want a projection may not have been
	 * computed. Further, by adjusting backwards, you may require BACKGROW to have run while it had explicitly been set
	 * to not run or it ran resulting in an error.
	 * <p>
	 * The solution here is to leave the age correction logic inside the code so that it can be re-activated easily and
	 * leave the age offset at zero so that there is no effect due to age correction.
	 * <p>
	 * By way of example:
	 * <p>
	 * A primary and secondary species differ in age by 10 years. The stand is projected over the time range of the
	 * primary species. VDYP7 determines the secondary species to be the leading species. The arithmetic to get the year
	 * at which the secondary species could require a projection for the stand before or after the range of years over
	 * which the stand was originally projected.
	 */
	private void calculateLayerAgeOffsets(YieldTableData row) {
		row.setLayerAgeOffset(0.0);
	}

	private void writeProjectionGrowthInfo(
			YieldTableData row, YieldTableWriter writer, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>> pair,
			Double targetAge
	) {
		if (row.isPolygonTable()) {
			getProjectedPolygonGrowthInfo(row, pair, targetAge);
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
	 * @param row  the row object into which the growth information is written
	 * @param pair the source of the growth information
	 * @param the  year for which the growth information is to be retrieved
	 */
	private void getProjectedPolygonGrowthInfo(
			YieldTableData row, Pair<Optional<VdypPolygon>, Optional<VdypPolygon>> pair, Double year
	) {

		if (year < Vdyp7Constants.MIN_SPECIES_AGE || year > Vdyp7Constants.MAX_SPECIES_AGE) {
			throw new IllegalArgumentException("getProjectionPolygonGrowthInfo: targetAge");
		}

		var primaryLayer = row.getPolygon().getPrimaryLayer();
		var primaryLayerAge0Year = primaryLayer.determineYearAtAge(0);

		double currentSiteIndex = 0;
		double dominantHeight = 0;
		double loreyHeight = 0;

		double totalTreesPerHectare = 0;
		double totalBasalArea = 0;

		for (var layer : row.getPolygon().getLayers().values()) {

			var layerAge0Year = layer.determineYearAtAge(0);
			double ageOffset = (double) primaryLayerAge0Year - layerAge0Year;
			var ageToRequest = year + ageOffset;

			if (ageToRequest >= 0) {
				getProjectionLayerStandGrowthInfo(row, layer, ageToRequest);
			}
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
	 * @param row          the row object into which the growth information is written
	 * @param vdypPolygon  the source of the growth information
	 * @param ageToRequest year for which the growth information is to be retrieved
	 */
	private void getProjectionLayerStandGrowthInfo(YieldTableData row, Layer layer, double ageToRequest) {

		var leadingSpeciesSp0 = layer.determineLeadingSp0(0);

		var projectionYear = layer.determineYearAtAge(ageToRequest);

		obtainStandYield(row, layer, leadingSpeciesSp0, ageToRequest);
	}

	/**
	 * <b>V7Int_ObtainStandYield</b>
	 * <p>
	 * Extract the appropriate yield from the stand at the requested age.
	 * <p>
	 * <b>Notes</b>
	 * <ul>
	 * <li>In cases of species being projected and not having a projected age and height even though it had one on
	 * input, we will now fill in the missing values. This seems to occur especially on Vet layers for secondary species
	 * with site information.
	 * <li>Made changes to catch a case where projected height is not always recalculated. See the comments at the top
	 * of this module as well as Cam's e-mail.
	 * <li>Based on Sam's Apr.14, 2008 e-mail, copy input BA/TPH over to projected if there is no projected BA/TPH. This
	 * also implies that the diameter should be filled in.
	 * </ul>
	 *
	 * @param row
	 * @param layer        the specific layer within the polygon for which yields are to be generated
	 * @param stand        the particular SP0 in the layer for which yield information is requested. If null, summary
	 *                     information for the whole layer is retrieved.
	 * @param ageToRequest the layer total age for which yields are to be generated
	 */
	private void obtainStandYield(YieldTableData row, Layer layer, Stand stand, double ageToRequest) {

		assert row != null && layer != null && stand != null && ageToRequest > 0;

		var projectionType = layer.getAssignedProjectionType();

		boolean dataExistsForLayerAtYear;
		if (ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST.contains(projectionType)) {
			dataExistsForLayerAtYear = true;
			logger.debug("{} obtainStandYield: projection type is {}", layer, projectionType);
		} else if (projectionType == ProjectionTypeCode.DO_NOT_PROJECT) {
			dataExistsForLayerAtYear = false;
			logger.debug("{} obtainStandYield: layer was marked \"do not project\"", layer);
		} else {
			throw new IllegalStateException(
					MessageFormat.format("{0} obtainStandYield: Projection type was not set for this layer!", layer)
			);
		}

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
		Integer calendarYear;

		switch (projectionType) {
		case DEAD: {
			if (layer.getAgeAtDeath() != null && ageToRequest > layer.getAgeAtDeath()) {
				ageToUse = layer.getAgeAtDeath();
				logger.debug("{}: dead layer, so clamping requested age to use to {}", layer, ageToUse);
			}
			break;
		}

		case VETERAN: {
			calendarYear = layer.getPolygon().getReferenceYear();
			ageToUse = layer.determineLayerAgeAtYear(calendarYear);
			logger.debug("{}: veteran layer, so clamping requested age to use to reference age of {}", layer, ageToUse);
			break;
		}
		default:
			// use the requested age
			break;
		}

		calendarYear = layer.determineYearAtAge(ageToUse);

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
		switch (projectionType) {
		case VETERAN:
			doReprojectHeight = true;
			break;
		case DEAD:
		case PRIMARY:
		case REGENERATION:
		case RESIDUAL:
			doReprojectHeight = false;
			break;
		case DO_NOT_PROJECT:
		case UNKNOWN:
			doReprojectHeight = true;
			break;
		default:
			throw new IllegalStateException("Unknown projection type " + projectionType);
		}

	}
}
