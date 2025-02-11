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
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
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

	public HcsvPolygonStream(ProjectionContext state, InputStream polygonStream, InputStream layersStream) {

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
				.deadLayer(null) // set later
				.district(null /* not available in HCSV input */) //
				.doAllowProjection(true) //
				.doAllowProjectionOfType(initializeProjectionMap(true)) //
				.featureId(nextPolygonRecord.getFeatureId()) //
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
					.assignedProjectionType(ProjectionTypeCode.UNKNOWN) //
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

		polygon.doCompleteDefinition();

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
					new ValidationMessage(ValidationMessageKind.DUPLICATE_LAYER_SUPPLIED, polygon, layer.getLayerId())
			);
		}

		if (layer.getPercentStockable() != null && polygon.getPercentStockable() != null
				&& layer.getPercentStockable() > polygon.getPercentStockable()) {

			ValidationMessage message = new ValidationMessage(
					ValidationMessageKind.LAYER_STOCKABILITY_EXCEEDS_POLYGON_STOCKABILITY, polygon, layer.getLayerId(),
					layer.getPercentStockable(), polygon.getPercentStockable()
			);

			polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());
			logger.error(
					"Layer '{}' percent stockable ({}%) exceeds the polygon percent stockable ({}%)", polygon,
					layer.getLayerId(), layer.getPercentStockable(), polygon.getPercentStockable()
			);
		}

		if ("1".equals(layer.getRankCode())) {
			if (polygon.getRank1Layer() != null) {

				ValidationMessage message = new ValidationMessage(
						ValidationMessageKind.POLYGON_ALREADY_HAS_RANK_ONE_LAYER, polygon
				);

				polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());
				logger.error("Polygon {} already has a rank one layer", polygon);
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
				layer, layer.getDoSuppressPerHAYields() ? "" : "not ", polygon.getInventoryStandard(),
				layer.getNonForestDescriptor()
		);

		// We are going to project this layer!
		layer.setDoIncludeWithProjection(true);

		polygon.getLayers().put(layer.getLayerId(), layer);

		if (layer.getVdyp7LayerCode() != null) {

			switch (layer.getVdyp7LayerCode()) {
			case DEAD: {
				// The current layer is targeted for the Dead Stem projection type. If we don't already have a
				// targeted dead stem layer, record this layer as the targeted dead stem layer.

				if (polygon.getDeadLayer() == null) {
					polygon.assignDeadLayer(layer, null, layer.getPercentStockable());
					logger.debug("Targeting layer {} as the Dead Stem VDYP7 layer", layer);
				} else {
					logger.warn("Layer {} was already identified as the Dead Stem VDYP7 Layer", polygon.getDeadLayer());
				}
				break;
			}
			case PRIMARY: {
				// The current layer is targeted to the Primary Layer. If we don't already have a
				// targeted primary layer, record this layer as the targeted primary layer.

				if (polygon.getTargetedPrimaryLayer() == null) {
					polygon.setTargetedPrimaryLayer(layer);
					logger.debug("Targeting layer {} as the Primary VDYP7 layer", layer);
				} else {
					logger.warn(
							"Layer {} was already identified as the Primary VDYP7 Layer",
							polygon.getTargetedPrimaryLayer()
					);
				}
				break;
			}
			case REGENERATION: {
				// The current layer is targeted to the Regeneration Layer. If we don't already have a
				// targeted regeneration layer, record this layer as the targeted regeneration layer.

				if (polygon.getRegenerationLayer() == null) {
					polygon.setRegenerationLayer(layer);
					logger.debug("Targeting layer {} as the Regeneration VDYP7 layer", layer);
				} else {
					logger.warn(
							"Layer {} was already identified as the Regeneration VDYP7 Layer",
							polygon.getRegenerationLayer()
					);
				}
				break;
			}
			case RESIDUAL: {
				// The current layer is targeted to the Residual Layer. If we don't already have a
				// targeted residual layer, record this layer as the targeted residual layer.

				if (polygon.getResidualLayer() == null) {
					polygon.setResidualLayer(layer);
					logger.debug("Targeting layer {} as the Residual VDYP7 layer", layer);
				} else {
					logger.warn(
							"Layer {} was already identified as the Residual VDYP7 Layer", polygon.getResidualLayer()
					);
				}
				break;
			}
			case VETERAN: {
				// The current layer is targeted to the Veteran Layer. If we don't already have a
				// targeted veteran layer, record this layer as the targeted veteran layer.

				if (polygon.getTargetedVeteranLayer() == null) {
					polygon.setTargetedVeteranLayer(layer);
					logger.debug("Targeting layer {} as the Veteran VDYP7 layer", layer);
				} else {
					logger.warn(
							"Layer {} was already identified as the Veteran VDYP7 Layer",
							polygon.getTargetedVeteranLayer()
					);
				}
				break;
			}
			default:
				logger.debug("Ignoring targeted VDYP7 layer {} of type {}", layer, layer.getVdyp7LayerCode());
				break;
			}
		}

		logger.debug("Added layer \"{}\" to polygon \"{}\"", layer, polygon);
	}

	private void buildStandsAndSpecies(Polygon polygon, Layer layer) throws PolygonValidationException {

		logger.debug("Building stand and species components of layer \"{}\"", layer);

		var layerReportingInfo = polygon.getReportingInfo().getLayers().get(layer.getLayerId());

		var layerProjectionType = layer.determineProjectionType(polygon);
		logger.debug(
				"Polygon {}: characterized layer {} to be of projection type {}.", polygon, layer, layerProjectionType
		);

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

			logger.error("Polygon {} Layer {}: species code {} is not recognized", polygon, layer, sd.speciesCode());

			var validationMessage = new ValidationMessage(
					ValidationMessageKind.UNRECOGNIZED_SPECIES, polygon, layer.getLayerId(), sd.speciesCode()
			);

			polygon.getMessages().add(
					new PolygonMessage.Builder() //
							.setLayer(layer) //
							.setErrorCode(ReturnCode.ERROR_INVALIDSPECIES).setMessage(validationMessage).build()
			);

			throw new PolygonValidationException(validationMessage);
		}

		boolean isNewStand = false;

		var stand = layer.getSp0sByNameMap().get(sp0Code);
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
													ValidationMessageKind.DUPLICATE_SPECIES, polygon, layer.getLayerId(),
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
														ValidationMessageKind.INCONSISTENT_SITE_INFO,
														polygon, layer.getLayerId(), sd.speciesCode()
												)
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

				isNewStand = true;
				
				stand = new Stand.Builder() //
						.species(new ArrayList<Species>()) //
						.sp0Code(sp0Code) //
						.layer(layer) //
						.build();
			} else {
				var validationMessage = new ValidationMessage(
						ValidationMessageKind.SPECIES_WITH_NO_STAND_OR_AGE, polygon, layer.getLayerId(), sd.speciesCode()
				);

				polygon.getMessages().add(
						new PolygonMessage.Builder() //
								.setLayer(layer) //
								.setErrorCode(ReturnCode.ERROR_INVALIDSPECIES).setMessage(validationMessage).build()
				);

				throw new PolygonValidationException(validationMessage);
			}
		}

		var newSpeciesInstance = new Species.Builder() //
				.stand(stand) //
				.speciesCode(speciesCode) //
				.speciesPercent(speciesPercent) //
				.totalAge(totalAge) //
				.dominantHeight(dominantHeight) //
				.build();

		if (speciesInstance != null) {
			// The new sp64 is a duplicate of an existing sp64. First, adjust
			// the existing sp64 to include whatever information from the new
			// sp64 it doesn't already have, and add the new sp64's percentage
			// to the total for that sp64.
			
			speciesInstance.addDuplicate(newSpeciesInstance);
			
			// 
		} else {
			// The new sp64 is new to the layer. 
			
			speciesInstance = newSpeciesInstance;
			
			// If no stand exists in the layer for this sp0, add one, and make the 
			// new species the species group for the stand.
						
			if (isNewStand) {
				stand.addSpeciesGroup(speciesInstance, layer.getSp0sAsSupplied().size());
				layer.addStand(stand);
			}
		}

		speciesInstance.calculateUndefinedFieldValues();

		stand.updateAfterSp64Added(speciesInstance);
		layer.updateAfterSp64Added(speciesInstance);

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
