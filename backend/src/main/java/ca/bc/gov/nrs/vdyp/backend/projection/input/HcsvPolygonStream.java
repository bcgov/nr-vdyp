package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvLayerRecordBean.SpeciesDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.model.History;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.ProjectionParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.SpeciesReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZone;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZoneMethods;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SpeciesTable;

public class HcsvPolygonStream extends AbstractPolygonStream {

	private static Logger logger = LoggerFactory.getLogger(HcsvPolygonStream.class);

	private CsvStreamIterator<HcsvPolygonRecordBean> polygonRecordIterator;
	private HcsvPolygonRecordBean nextPolygonRecord;

	private CsvStreamIterator<HcsvLayerRecordBean> layerRecordIterator;
	private HcsvLayerRecordBean nextLayerRecord;

	public HcsvPolygonStream(ProjectionState state, InputStream polygonStream, InputStream layersStream) {

		super(state);

		var hcsvPolygonStream = HcsvPolygonRecordBean.createHcsvPolygonStream(polygonStream).iterator();
		polygonRecordIterator = new CsvStreamIterator<>(hcsvPolygonStream);
		var hcsvLayerStream = HcsvLayerRecordBean.createHcsvLayerStream(layersStream).iterator();
		layerRecordIterator = new CsvStreamIterator<>(hcsvLayerStream);

		advanceToFirstPolygon();
	}

	@Override
	public Polygon getNextPolygon() throws PolygonValidationException {

		if (nextPolygonRecord == null) {
			throw new IllegalStateException("Attempt to read past end of polygon input stream");
		}

		var polygon = buildPolygon();

		advanceToNextPolygon();

		return polygon;
	}

	private void advanceToFirstPolygon() {

		assert nextPolygonRecord == null;
		assert nextLayerRecord == null;

		if (polygonRecordIterator.hasNext()) {
			nextPolygonRecord = polygonRecordIterator.next();

			while (layerRecordIterator.hasNext()) {
				nextLayerRecord = layerRecordIterator.next();
				if (nextLayerRecord.getFeatureId() >= nextPolygonRecord.getFeatureId()) {
					break;
				}
			}
		}
	}

	private void advanceToNextPolygon() {

		if (polygonRecordIterator.hasNext()) {
			nextPolygonRecord = polygonRecordIterator.next();

			while (nextLayerRecord != null && nextLayerRecord.getFeatureId() < nextPolygonRecord.getFeatureId()) {
				if (layerRecordIterator.hasNext()) {
					nextLayerRecord = layerRecordIterator.next();
				} else {
					nextLayerRecord = null;
				}
			}
		} else {
			nextPolygonRecord = null;
		}
	}

	private void advanceToNextLayer() {

		if (layerRecordIterator.hasNext()) {
			nextLayerRecord = layerRecordIterator.next();
		} else {
			nextLayerRecord = null;
		}
	}

	@Override
	public boolean hasNextPolygon() {
		return nextPolygonRecord != null;
	}

	private Polygon buildPolygon() throws PolygonValidationException {

		ProjectionParameters projectionParameters = new ProjectionParameters.Builder() //
				.enableBack(state.getValidatedParams().containsOption(ExecutionOption.BACK_GROW_ENABLED)) //
				.enableForward(state.getValidatedParams().containsOption(ExecutionOption.FORWARD_GROW_ENABLED)) //
				.measurementYear(nextPolygonRecord.getReferenceYear()).standAgeAtMeasurementYear(0).build();

		var nonVegetationMap = nextPolygonRecord.getNonVegCoverDetails();
		var otherVegetationMap = nextPolygonRecord.getOtherVegCoverDetails();

		BecZone bec = BecZoneMethods.becZoneToIndex(nextPolygonRecord.getBecZoneCode());
		Long polygonFeatureId = nextPolygonRecord.getFeatureId();

		var polygonReportingInfo = new PolygonReportingInfo.Builder().featureId(polygonFeatureId)
				.polygonNumber(nextPolygonRecord.getPolygonNumber()).mapSheet(nextPolygonRecord.getMapId())
				.mapQuad("0" /* lcl_CopyPolygonDataInfoSnapshot, line 3162 */)
				.mapSubQuad("0" /* lcl_CopyPolygonDataInfoSnapshot, line 3163 */)
				.nonProdDescriptor(nextPolygonRecord.getNonProductiveDescriptorCode()).district(null)
				.referenceYear(nextPolygonRecord.getReferenceYear()).build();

		var layers = new HashMap<String /* layer id */, Layer>();

		Polygon polygon = new Polygon.Builder() //
				.becZone(bec.getText()) //
				.cfsEcoZone(nextPolygonRecord.getCfsEcoZoneCode()) //
				.coastal(bec.getSpeciesRegion() == SpeciesRegion.COAST) //
				.currentProcessingState(PolygonProcessingState.POLYGON_DEFINED) //
				.deadLayer(null) //
				.district(null /* not available in HCSV input */) //
				.doAllowProjection(false) //
				.doAllowProjectionOfType(initializeProjectionMap(true)) //
				.featureId(nextPolygonRecord.getFeatureId()) //
				.firstYearValidYields(initializeProjectionMap(-9999)) //
				.inventoryStandard(InventoryStandard.getFromCode(nextPolygonRecord.getInventoryStandardCode())) //
				.mapSheet(nextPolygonRecord.getMapId()) //
				.nonProductiveDescriptor(nextPolygonRecord.getNonProductiveDescriptorCode()).nonVegetationTypes(null) //
				.nonVegetationTypes(nonVegetationMap) //
				.otherVegetationTypes(otherVegetationMap) //
				.percentStockable(nextPolygonRecord.getPercentStockable()) //
				.percentStockableDead(nextPolygonRecord.getPercentDead()) //
				.polygonNumber(nextPolygonRecord.getPolygonNumber()).projectionParameters(projectionParameters) //
				.referenceYear(nextPolygonRecord.getReferenceYear()) //
				.reportingInfo(polygonReportingInfo) //
				.layers(layers) //
				.build();

		while (nextLayerRecord != null && nextLayerRecord.getFeatureId() == polygonFeatureId) {

			var layerReportingInfo = new LayerReportingInfo.Builder().layerID(nextLayerRecord.getLayerId())
					.rank(nextLayerRecord.getForestCoverRankCode())
					.nonForestDesc(nextLayerRecord.getNonForestDescriptorCode())
					.processedAsVDYP7Layer(nextLayerRecord.getTargetVdyp7LayerCode()).build();

			polygonReportingInfo.getLayers().put(layerReportingInfo.getLayerID(), layerReportingInfo);

			// Note that HCSV contains no history information (lcl_CopyHistoryDataIntoSnapshot)
			var history = new History.Builder().build();

			var stands = new ArrayList<Stand>();

			Layer layer = new Layer.Builder() //
					.polygon(polygon) //
					.layerId(nextLayerRecord.getLayerId()) //
					.basalArea(nextLayerRecord.getBasalArea()) //
					.crownClosure(nextLayerRecord.getCrownClosure()) //
					.estimatedSiteIndex(nextLayerRecord.getEstimatedSiteIndex()) //
					.estimatedSiteIndexSpecies(nextLayerRecord.getEstimatedSiteIndexSpeciesCode()) //
					.measuredUtilizationLevel(7.5 /* from lcl_CopyLayerDataIntoSnapshot, line 4596 */) //
					.nonForestDescriptor(nextLayerRecord.getNonForestDescriptorCode()) //
					.precentStockable(nextLayerRecord.getLayerStockability()) //
					.rankCode(nextLayerRecord.getForestCoverRankCode()) //
					.treesPerHectare(nextLayerRecord.getStemsPerHectare()) //
					.vdyp7LayerCode(nextLayerRecord.getTargetVdyp7LayerCode()) //
					.species(stands) //
					.history(history) //
					.build();

			addLayerToPolygon(polygon, layer);

			buildStandsAndSpecies(polygon, layer);

			advanceToNextLayer();
		}

		logger.info("Successfully read polygon with feature id \"{}\"", polygon.getFeatureId());

		return polygon;
	}

	private void addLayerToPolygon(Polygon polygon, Layer layer) throws PolygonValidationException {

		// The field values of layer have been validated. Now validate
		// the layer content against the containing polygon.

		logger.debug("Performing validation of layer \"{}\" in the context of polygon \"{}\"", layer, polygon);

		Layer foundLayer = polygon.findSpecificLayer(layer.getLayerId());
		if (foundLayer != null) {

			logger.error("A layer with Id '{}' has already been supplied.", layer.getLayerId());
			throw new PolygonValidationException(
					new ValidationMessage(ValidationMessageKind.DUPLICATE_LAYER_SUPPLIED, layer.getLayerId())
			);
		}

		if (layer.getPercentStockable() != null && polygon.getPercentStockable() != null
				&& layer.getPercentStockable() > polygon.getPercentStockable()) {

			ValidationMessage message = new ValidationMessage(
					ValidationMessageKind.LAYER_STOCKABILITY_EXCEEDS_POLYGON_STOCKABILITY, layer.getLayerId(),
					layer.getPercentStockable(), polygon.getPercentStockable()
			);

			polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());
			logger.error(
					"Layer '{}' percent stockable ({}%) exceeds the polygon percent stockable ({}%)",
					layer.getLayerId(), layer.getPercentStockable(), polygon.getPercentStockable()
			);
		}

		if ("1".equals(layer.getRankCode())) {
			if (polygon.getRank1Layer() != null) {

				ValidationMessage message = new ValidationMessage(
						ValidationMessageKind.POLYGON_ALREADY_HAS_RANK_ONE_LAYER
				);

				polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());
				logger.error("Polygon already has a rank one layer");
			} else {
				polygon.setRank1Layer(layer);
			}
		}

		/* Check if Yields should be suppressed according to IPSCB206. */

		if (layer.getNonForestDescriptor() != null) {
			switch (polygon.getInventoryStandard()) {
			case FIP:
				if ("NSR".equals(layer.getNonForestDescriptor()) || "NC".equals(layer.getNonForestDescriptor())) {
					layer.setDoSuppressPerHAYields(true);
				} else {
					layer.setDoSuppressPerHAYields(false);
				}
				break;
			default:
				if ("NSR".equals(layer.getNonForestDescriptor())) {
					layer.setDoSuppressPerHAYields(true);
				} else {
					layer.setDoSuppressPerHAYields(false);
				}
				break;
			}
		} else {
			layer.setDoSuppressPerHAYields(false);
		}

		logger.debug(
				"Layer {}: Projected Per HA Yields will {}be suppressed because Inv Std is '{}' and Non-Forest Desc is '{}'",
				layer, layer.doSuppressPerHAYields() ? "" : "not ", polygon.getInventoryStandard(),
				layer.getNonForestDescriptor()
		);

		// We are going to project this layer!
		layer.setDoIncludeWithProjection(true);

		polygon.setLastReferencedLayer(layer);
		polygon.getLayers().put(layer.getLayerId(), layer);

		logger.debug("Added layer \"{}\" to polygon \"{}\"", layer, polygon);
	}

	private void buildStandsAndSpecies(Polygon polygon, Layer layer) throws PolygonValidationException {

		logger.debug("Building stand and species components of layer \"{}\"", layer);

		var layerReportingInfo = polygon.getReportingInfo().getLayers().get(layer.getLayerId());

		var layerProjectionType = layer.determineProjectionType(polygon);
		logger.debug(
				"Polygon {}: characterized layer {} to be of projection type {}.", polygon, layer, layerProjectionType
		);

		var stands = layer.getSp0sByName();

		var species = new ArrayList<Species>();

		var speciesDetails = nextLayerRecord.getSpeciesDetails();
		for (var sd : speciesDetails) {

			var speciesReportingInfo = new SpeciesReportingInfo.Builder().sp64Name(sd.speciesCode())
					.sp64Percent(sd.percent()).build();

			layerReportingInfo.getSpecies().put(speciesReportingInfo.getSp64Name(), speciesReportingInfo);

			var speciesInstance = addSpeciesToLayer(polygon, layer, sd);

			species.add(speciesInstance);
		}
	}

	private Species addSpeciesToLayer(Polygon polygon, Layer layer, SpeciesDetails sd) 
		throws PolygonValidationException {

		Species speciesInstance = null;

		var speciesCode = sd.speciesCode();
		var speciesPercent = sd.percent();
		var totalAge = sd.estimatedAge() == null ? null : Double.valueOf(sd.estimatedAge());
		var dominantHeight = sd.estimatedHeight();

		String sp0Code = SiteTool.getSpeciesVDYP7Code(speciesCode);
		
		if (SiteTool.getSpeciesIndex(sd.speciesCode()) == SpeciesTable.UNKNOWN_ENTRY_INDEX
				|| SiteTool.getSpeciesIndex(sp0Code) == SpeciesTable.UNKNOWN_ENTRY_INDEX
				/* TODO: || sp0Code is NOT a species known to the back end. Is this necessary here?) */) {

			ProjectionTypeCode layerProjectionType = layer.determineProjectionType(polygon);
			polygon.disableProjectionsOfType(layerProjectionType);

			logger.error(
					"Polygon {} Layer {}: species code {} is not recognized", polygon, layer, sd.speciesCode()
			);
			
			var validationMessage = new ValidationMessage(
					ValidationMessageKind.UNRECOGNIZED_SPECIES, layer.getLayerId(),
					sd.speciesCode()
			);

			polygon.getMessages().add(
					new PolygonMessage.Builder() //
							.setLayer(layer) //
							.setErrorCode(ReturnCode.ERROR_INVALIDSPECIES)
							.setMessage(validationMessage)
							.build()
			);
			
			throw new PolygonValidationException(validationMessage);
			
		}

		var stand = layer.getSp0sByName().get(sp0Code);

		if (stand != null) {
			
			for (var possibleDuplicate : stand.getSpecies()) {

				if (possibleDuplicate.getSpeciesCode().equals(sd.speciesCode())) {
					// We have a duplicate species

					polygon.getMessages().add(
							new PolygonMessage.Builder() //
									.setLayer(layer) //
									.setErrorCode(ReturnCode.ERROR_INVALIDSPECIES) //
									.setMessage(
											new ValidationMessage(
													ValidationMessageKind.DUPLICATE_SPECIES, layer.getLayerId(),
													sd.speciesCode()
											)
									).build()
					);

					logger.warn(
							"Polygon {} Layer {}: species code {} appears more than once in the layer definition",
							polygon, layer, sd.speciesCode()
					);

					if (!possibleDuplicate.equivalentSiteInfo(sd)) {

						polygon.getMessages().add(
								new PolygonMessage.Builder() //
										.setLayer(layer) //
										.setErrorCode(ReturnCode.ERROR_INVALIDSITEINFO)
										.setMessage(
												new ValidationMessage(
														ValidationMessageKind.INCONSISTENT_SITE_INFO, layer.getLayerId(),
														sd.speciesCode())
												) //
										.build()
						);

						logger.warn(
								"Polygon {} Layer {}: the site information information for at least two species with species code \"{}\" is inconsistent",
								polygon, layer, sd.speciesCode()
						);
					}
					speciesInstance = possibleDuplicate;
				}
			}
		} else {

			if (totalAge != null) {

				// This stand is not yet defined and this species is necessarily the
				// largest percentage sp64 and therefore will be the sp0 for the layer.

				stand = new Stand.Builder() //
						.species(new ArrayList<Species>()) //
						.layer(layer) //
						.build();

				layer.getSp0sByName().put(speciesCode, stand);
			}
		}
		
		var newSpeciesInstance = new Species.Builder() //
				.parentComponent(stand) //
				.speciesCode(speciesCode) //
				.speciesPercent(speciesPercent) //
				.totalAge(totalAge) //
				.dominantHeight(dominantHeight) //
				.build();

		if (speciesInstance != null) {
			speciesInstance.addDuplicate(newSpeciesInstance);
		} else {
			speciesInstance = newSpeciesInstance;
			stand.updateAfterSpeciesGroupAdded(speciesInstance);
		}
		
		speciesInstance.calculateUndefinedFieldValues();

		stand.updateAfterSpeciesAdded(speciesInstance);
		layer.updateAfterSpeciesAdded(stand, speciesInstance);
		
		return speciesInstance;
	}

	private <T> Map<ProjectionTypeCode, T> initializeProjectionMap(T value) {
		Map<ProjectionTypeCode, T> map = new HashMap<>();
		for (ProjectionTypeCode t : ProjectionTypeCode.values()) {
			map.put(t, value);
		}
		return map;
	}
}
