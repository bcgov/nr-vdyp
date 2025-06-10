package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.exceptionhandler.ExceptionHandlerQueue;
import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.backend.model.v1.PolygonMessageKind;
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

	private CsvToBean<HcsvPolygonRecordBean> hcsvPolygonStream;
	private CsvStreamIterator<HcsvPolygonRecordBean> polygonRecordIterator;
	private HcsvPolygonRecordBean nextPolygonRecord;

	private CsvToBean<HcsvLayerRecordBean> hcsvLayerStream;
	private CsvStreamIterator<HcsvLayerRecordBean> layerRecordIterator;
	private HcsvLayerRecordBean nextLayerRecord;

	public HcsvPolygonStream(ProjectionContext context, InputStream polygonStream, InputStream layersStream) {

		super(context);

		var exceptionHandler = new ExceptionHandlerQueue();

		hcsvPolygonStream = HcsvPolygonRecordBean.createHcsvPolygonStream(exceptionHandler, polygonStream);
		polygonRecordIterator = new CsvStreamIterator<>(hcsvPolygonStream.iterator());
		hcsvLayerStream = HcsvLayerRecordBean.createHcsvLayerStream(exceptionHandler, layersStream);
		layerRecordIterator = new CsvStreamIterator<>(hcsvLayerStream.iterator());

		advanceToFirstPolygon();
	}

	@Override
	public boolean hasNextPolygon() {
		return nextPolygonRecord != null;
	}

	@Override
	public Polygon getNextPolygon() throws PolygonValidationException, IllegalStateException {

		if (nextPolygonRecord == null) {
			throw new IllegalStateException("Attempt to read past end of polygon input stream");
		}

		Polygon polygon = null;

		List<ValidationMessage> validationMessagesSeenDuringRead = new ArrayList<ValidationMessage>();
		PolygonValidationException validationExceptionSeenDuringBuild = null;
		try {
			validationMessagesSeenDuringRead = collectExceptions();
			polygon = buildPolygon();
		} catch (PolygonValidationException e) {
			validationExceptionSeenDuringBuild = e;
		} finally {
			advanceToNextPolygon();
		}

		if (validationExceptionSeenDuringBuild != null) {
			if (!validationMessagesSeenDuringRead.isEmpty()) {
				validationMessagesSeenDuringRead.addAll(validationExceptionSeenDuringBuild.getValidationMessages());
				throw new PolygonValidationException(validationMessagesSeenDuringRead);
			} else {
				throw validationExceptionSeenDuringBuild;
			}
		} else if (!validationMessagesSeenDuringRead.isEmpty()) {
			throw new PolygonValidationException(validationMessagesSeenDuringRead);
		}

		return polygon;
	}

	private List<ValidationMessage> collectExceptions() {
		var messages = new ArrayList<ValidationMessage>();

		for (var e : hcsvPolygonStream.getCapturedExceptions()) {
			collectMessages(messages, e);
		}

		for (var e : hcsvLayerStream.getCapturedExceptions()) {
			collectMessages(messages, e);
		}

		return messages;
	}

	private void collectMessages(List<ValidationMessage> messages, Exception e) {
		if (e instanceof CsvConstraintViolationException cve) {
			if (cve.getSourceObject() instanceof AbstractProjectionRequestException apre) {
				messages.addAll(apre.getValidationMessages());
			} else {
				throw new IllegalStateException(
						"Expecting instanceof AbstractProjectionRequestException; saw instead " + e.getClass().getName()
				);
			}
			System.out.println(e);
		} else {
			throw new IllegalStateException(
					"Expecting instanceof CsvConstraintViolationException; saw instead " + e.getClass().getName()
			);
		}
	}

	private void advanceToFirstPolygon() {

		Validate.isTrue(
				nextPolygonRecord == null, "HcsvPolygonStream.advanceToFirstPolygon: nextPolygonRecord must be null"
		);
		Validate.isTrue(nextLayerRecord == null, "HcsvPolygonStream.nextLayerRecord: nextPolygonRecord must be null");

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

	private Polygon buildPolygon() throws PolygonValidationException {

		Polygon polygon = null;

		try {
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

			polygon = new Polygon.Builder() //
					.becZone(bec.getText()) //
					.cfsEcoZone(nextPolygonRecord.getCfsEcoZoneCode()) //
					.isCoastal(bec.getSpeciesRegion() == SpeciesRegion.COAST) //
					.district(null /* not available in HCSV input */) //
					.doAllowProjection(true) //
					.doAllowProjectionOfType(initializeProjectionMap(true)) //
					.featureId(nextPolygonRecord.getFeatureId()) //
					.inventoryStandard(InventoryStandard.getFromCode(nextPolygonRecord.getInventoryStandardCode())) //
					.mapSheet(nextPolygonRecord.getMapId()) //
					.nonProductiveDescriptor(nextPolygonRecord.getNonProductiveDescriptorCode())
					.nonVegetationTypes(nonVegetationMap) //
					.otherVegetationTypes(otherVegetationMap) //
					.percentStockable(nextPolygonRecord.getPercentStockable()) //
					.percentStockableDead(nextPolygonRecord.getPercentDead()) //
					.polygonNumber(nextPolygonRecord.getPolygonNumber()) //
					.referenceYear(nextPolygonRecord.getReferenceYear()) //
					.reportingInfo(polygonReportingInfo) //
					.layers(layers) //
					.build();

			int layerOrderNumber = 0;
			while (nextLayerRecord != null && nextLayerRecord.getFeatureId() == polygonFeatureId) {

				try {
					// Note that HCSV contains no history information (lcl_CopyHistoryDataIntoSnapshot)
					var history = new History.Builder().build();

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
							.percentStockable(nextLayerRecord.getLayerStockability()) //
							.rankCode(nextLayerRecord.getForestCoverRankCode()) //
							.treesPerHectare(nextLayerRecord.getStemsPerHectare()) //
							.vdyp7LayerCode(nextLayerRecord.getTargetVdyp7LayerCode()) //
							.history(history) //
							.build();

					addLayerToPolygon(polygon, layer);

					var layerReportingInfo = new LayerReportingInfo.Builder() //
							.layer(layer) //
							.sourceLayerID(layerOrderNumber) //
							.build();

					polygonReportingInfo.getLayerReportingInfos()
							.put(layerReportingInfo.getLayerID(), layerReportingInfo);

					buildStandsAndSpecies(polygon, layer);

				} finally {
					layerOrderNumber += 1;

					advanceToNextLayer();
				}
			}

			polygon.doCompleteDefinition(context);

		} catch (Exception e) {
			if (! (e instanceof PolygonValidationException)) {
				var message = new ValidationMessage(ValidationMessageKind.GENERIC, e.getMessage());
				throw new PolygonValidationException(message);
			} else {
				throw e;
			}
		}

		logger.info("Successfully read polygon with feature id \"{}\"", polygon.getFeatureId());

		return polygon;
	}

	/**
	 * <code>V7Ext_AddLayer</code>
	 * <p>
	 * Add the given Layer (containing stand information) to the given Polygon.
	 *
	 * @param polygon as described
	 * @param layer   as described
	 * @throws PolygonValidationException
	 */
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

			polygon.addMessage(
					new PolygonMessage.Builder().layer(layer)
							.details(
									ReturnCode.ERROR_INVALIDPARAMETER, MessageSeverityCode.ERROR,
									PolygonMessageKind.LAYER_STOCKABILITY_EXCEEDS_POLYGON_STOCKABILITY,
									layer.getPercentStockable(), polygon.getPercentStockable()
							).build()
			);
		}

		if ("1".equals(layer.getRankCode())) {
			if (polygon.getRank1Layer() != null) {

				polygon.addMessage(
						new PolygonMessage.Builder().layer(layer)
								.details(
										ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
										PolygonMessageKind.POLYGON_ALREADY_HAS_RANK_ONE_LAYER
								).build()
				);
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

		var layerReportingInfo = polygon.getReportingInfo().getLayerReportingInfos().get(layer.getLayerId());

		var layerProjectionType = layer.determineProjectionType(polygon);
		logger.debug(
				"Polygon {}: characterized layer {} to be of projection type {}.", polygon, layer, layerProjectionType
		);

		var species = new ArrayList<Species>();
		var speciesReportingInfoList = new ArrayList<SpeciesReportingInfo>();

		var sp64Details = nextLayerRecord.getSpeciesDetails();
		var index = 0;
		for (var sd : sp64Details) {

			var speciesReportingInfo = new SpeciesReportingInfo.Builder() //
					.sp64Name(sd.speciesCode()) //
					.sp64Percent(sd.percent()) //
					.asSuppliedIndex(index++) //
					.build();

			speciesReportingInfoList.add(speciesReportingInfo);

			species.add(addSpeciesToLayer(layer, sd));
		}

		layerReportingInfo.setSpeciesReportingInfos(speciesReportingInfoList);
	}

	/**
	 * <code>V7Ext_AddSpeciesComponent</code>
	 * <p>
	 * Add a Species with the given details to the given layer.
	 *
	 * @param layer       the target layer
	 * @param sp64Details the details of the species to be added
	 * @return the resulting Species instance
	 * @throws PolygonValidationException when the species details contain inaccurate information
	 */
	private Species addSpeciesToLayer(Layer layer, SpeciesDetails sp64Details) throws PolygonValidationException {

		Species sp64 = null;

		var speciesCode = sp64Details.speciesCode();
		var speciesPercent = sp64Details.percent();
		var totalAge = sp64Details.estimatedAge() == null ? null : Double.valueOf(sp64Details.estimatedAge());
		var dominantHeight = sp64Details.estimatedHeight();

		String sp0Code = SiteTool.getSpeciesVDYP7Code(speciesCode);

		if (SiteTool.getSpeciesIndex(sp64Details.speciesCode()) == SpeciesTable.UNKNOWN_ENTRY_INDEX
				|| SiteTool.getSpeciesIndex(sp0Code) == SpeciesTable.UNKNOWN_ENTRY_INDEX) {

			ProjectionTypeCode layerProjectionType = layer.determineProjectionType(layer.getPolygon());
			layer.getPolygon().disableProjectionsOfType(layerProjectionType);

			logger.error("{}: species code {} is not recognized", layer, sp64Details.speciesCode());

			var validationMessage = new ValidationMessage(
					ValidationMessageKind.UNRECOGNIZED_SPECIES, layer.getPolygon(), layer.getLayerId(),
					sp64Details.speciesCode()
			);

			throw new PolygonValidationException(validationMessage);
		}

		boolean isNewStand;
		var stand = layer.getSp0sByNameMap().get(sp0Code);
		if (stand != null) {

			isNewStand = false;

			for (var possibleDuplicate : stand.getSpeciesByPercent()) {

				if (possibleDuplicate.getSpeciesCode().equals(sp64Details.speciesCode())) {
					// We have a duplicate species

					layer.getPolygon().addMessage(
							new PolygonMessage.Builder() //
									.stand(stand) //
									.details(
											ReturnCode.ERROR_SPECIESALREADYEXISTS, MessageSeverityCode.WARNING,
											PolygonMessageKind.DUPLICATE_SPECIES, sp64Details.speciesCode()
									).build()
					);

					logger.warn(
							"Polygon {} Layer {}: species code {} appears more than once in the layer definition",
							layer.getPolygon(), layer, sp64Details.speciesCode()
					);

					if (!possibleDuplicate.equivalentSiteInfo(sp64Details)) {

						layer.getPolygon().addMessage(
								new PolygonMessage.Builder() //
										.stand(stand) //
										.details(
												ReturnCode.ERROR_INVALIDSITEINFO, MessageSeverityCode.WARNING,
												PolygonMessageKind.INCONSISTENT_SITE_INFO, sp64Details.speciesCode()
										) //
										.build()
						);

						logger.warn(
								"Polygon {} Layer {}: the site information information for at least two species with species code \"{}\" is inconsistent",
								layer.getPolygon(), layer, sp64Details.speciesCode()
						);
					}
					sp64 = possibleDuplicate;
				}
			}
		} else {

			isNewStand = true;

			// This stand is not yet defined and this species is necessarily the
			// largest percentage sp64 and therefore will be the sp0 for the layer.

			stand = new Stand.Builder() //
					.sp0Code(sp0Code) //
					.layer(layer) //
					.build();
		}

		var newSp64 = new Species.Builder() //
				.stand(stand) //
				.speciesCode(speciesCode) //
				.speciesPercent(speciesPercent) //
				.totalAge(totalAge) //
				.dominantHeight(dominantHeight) //
				.build();

		if (sp64 != null) {

			// The new sp64 is a duplicate of an existing sp64. First, adjust
			// the existing sp64 to include whatever information from the new
			// sp64 it doesn't already have, and add the new sp64's percentage
			// to the total for that sp64.

			sp64.addDuplicate(newSp64);
		} else {
			// The new sp64 is new to the layer.

			sp64 = newSp64;

			// If no stand exists in the layer for this sp0, add one, and make the
			// new species the species group for the stand.

			if (isNewStand) {
				Species sp0 = new Species.Builder().stand(stand) //
						.speciesCode(speciesCode) //
						.speciesPercent(0) //
						.build();
				stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
				layer.addStand(stand);
			}
		}

		sp64.calculateUndefinedFieldValues();

		stand.addSp64(sp64);
		layer.addSp64(sp64);

		return sp64;
	}

	private <T> Map<ProjectionTypeCode, T> initializeProjectionMap(T value) {
		Map<ProjectionTypeCode, T> map = new HashMap<>();
		for (ProjectionTypeCode t : ProjectionTypeCode.values()) {
			map.put(t, value);
		}
		return map;
	}
}
