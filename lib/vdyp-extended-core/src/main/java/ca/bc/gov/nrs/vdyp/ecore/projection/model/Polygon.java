package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean.NonVegCoverDetails;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean.OtherVegCoverDetails;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.CfsEcoZoneCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.LayerSummarizationModeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.OtherVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.SilviculturalBaseCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.NullMath;
import ca.bc.gov.nrs.vdyp.math.VdypMath;

/**
 * This class is the internal representation of a Polygon to be projected.
 */
public class Polygon implements Comparable<Polygon> {

	private static Logger logger = LoggerFactory.getLogger(Polygon.class);

	// BUSINESS KEY - all fields required.

	/** Feature Id (called, sometimes, "polygon record i.d." in VDYP7 */
	private long featureId;

	// Optional Members

	/** The polygon's "polygon number" */
	private Long polygonNumber;

	/** The district responsible for the map */
	private String district;
	/** The mapsheet containing the polygon */
	private String mapSheet;
	/** The mapsheet quadrant containing the polygon */
	private String mapQuad;
	/** The mapsheet sub-quadrant containing the polygon */
	private String mapSubQuad;

	/** The inventory standard at which the polygon data was recorded. */
	private InventoryStandard inventoryStandard;

	/** The reference year of the polygon */
	private Integer referenceYear;

	/** The year in which the stand suffered a significant kill */
	private Integer yearOfDeath;

	/** If true, the polygon is in the Coastal region; otherwise, it's in the Interior region */
	private boolean isCoastal;

	/** the polygon's Forest Inventory Zone */
	private String forestInventoryZone;

	/** the polygon's BEC Zone */
	private String becZone;

	/** the polygon's Canadian Forest Service Ecological Zone */
	private CfsEcoZoneCode cfsEcoZone;

	/** the Non-Productive Descriptor of the polygon. */
	private String nonProductiveDescriptor;

	/** The percentage of the polygon that's actually stockable. */
	private Double percentStockable;

	/** The amount of the polygon that has suffered significant kill. */
	private Double percentStockableDead;

	/** The factor to multiply predicted yields by */
	private Double yieldFactor;

	/** If true, layer adjustments were supplied in the input data */
	private Boolean wereLayerAdjustmentsSupplied;

	/** The layers (to be) projected. */
	private Map<String, Layer> layers;

	/** Each entry in this map identifies the Layer for a given projection type */
	private Map<ProjectionTypeCode, Layer> layerByProjectionType;

	/** Polygon disturbance data. */
	private History history;

	/** This polygon's reporting information, including that of all child layers */
	private PolygonReportingInfo reportingInfo;

	private Map<OtherVegetationTypeCode, OtherVegCoverDetails> otherVegetationTypes;
	private Map<NonVegetationTypeCode, NonVegCoverDetails> nonVegetationTypes;

	// MUTABLE fields - the value of these fields will change over the lifetime of the object,
	// but not after the definition is locked.

	private boolean definitionIsLocked;

	/** The layer summarization mode applied to the layers within the polygon */
	private LayerSummarizationModeCode layerSummarizationMode;

	/**
	 * Points to the layer identified as that desired to be the VDYP7 primary layer, irrespective of the automatic
	 * Primary Layer selection logic. If <code>null</code>, the normal primary layer selection logic will be applied.
	 */
	private Layer targetedPrimaryLayer;

	/**
	 * Points to the layer identified as that desired to be the VDYP7 veteran layer, irrespective of the automatic
	 * Veteran Layer selection logic. If <code>null</code>, the normal veteran layer selection logic will be applied.
	 */
	private Layer targetedVeteranLayer;

	/**
	 * Points to the layer identified as the primary layer. Any time an aspect of the stand is redefined, this member
	 * will be reset and have to be redetermined.
	 *
	 * This layer will be sent to the VDYP7 libraries as the 'P' layer.
	 */
	private Layer primaryLayer;

	/**
	 * Points to the layer identified as the veteran layer. Any time an aspect of the stand is redefined, this member
	 * will be reset and have to be redetermined.
	 *
	 * Points to the layer which will be sent to the VDYP7 libraries as the 'V' layer.
	 */
	private Layer veteranLayer;

	/**
	 * Points to the layer identified as the residual layer. Any time an aspect of the stand is redefined, this member
	 * will be reset and have to be redetermined.
	 */
	private Layer residualLayer;

	/**
	 * Points to the layer identified as the regeneration layer. Any time an aspect of the stand is redefined, this
	 * member will be reset and have to be redetermined.
	 */
	private Layer regenerationLayer;

	/**
	 * Points to the layer identified as the dead layer. Any time an aspect of the stand is redefined, this member will
	 * be reset and have to be redetermined.
	 */
	private Layer deadLayer;

	/** The layer marked as having Rank '1'. */
	private Layer rank1Layer;

	/** Messages generated during the definition of the polygon */
	private ArrayList<PolygonMessage> messages;

	/** If false, projection is turned off globally for this polygon */
	private Boolean doAllowProjection;

	/** If false for a given projection type, projection is turned off for that type */
	private Map<ProjectionTypeCode, Boolean> doAllowProjectionOfType;

	/** Initialize the Polygon, according to <code>V7Int_ResetPolyInfo</code>. */
	private Polygon() {

		featureId = 0;
		district = "UNK";
		mapSheet = "TESTMAP";
		mapQuad = "0";
		mapSubQuad = "0";
		polygonNumber = 0L;
		inventoryStandard = InventoryStandard.getDefault();
		layerSummarizationMode = LayerSummarizationModeCode.getDefault();
		referenceYear = 0;
		yearOfDeath = null;
		isCoastal = false;
		forestInventoryZone = null;
		becZone = "";
		cfsEcoZone = CfsEcoZoneCode.getDefault();
		nonProductiveDescriptor = null;
		percentStockable = null;
		percentStockableDead = null;
		yieldFactor = 1.0;

		otherVegetationTypes = Map.of(
				OtherVegetationTypeCode.Bryoid, new OtherVegCoverDetails(0, OtherVegetationTypeCode.Bryoid), //
				OtherVegetationTypeCode.Herb, new OtherVegCoverDetails(0, OtherVegetationTypeCode.Herb), //
				OtherVegetationTypeCode.Shrub, new OtherVegCoverDetails(0, OtherVegetationTypeCode.Shrub)
		);
		nonVegetationTypes = Map.of(
				NonVegetationTypeCode.BurnedArea, new NonVegCoverDetails("", 0, NonVegetationTypeCode.BurnedArea), //
				NonVegetationTypeCode.ExposedSoil, new NonVegCoverDetails("", 0, NonVegetationTypeCode.ExposedSoil), //
				NonVegetationTypeCode.Other, new NonVegCoverDetails("", 0, NonVegetationTypeCode.Other), //
				NonVegetationTypeCode.Rock, new NonVegCoverDetails("", 0, NonVegetationTypeCode.Rock), //
				NonVegetationTypeCode.Snow, new NonVegCoverDetails("", 0, NonVegetationTypeCode.Snow), //
				NonVegetationTypeCode.Water, new NonVegCoverDetails("", 0, NonVegetationTypeCode.Water)
		); //

		doAllowProjection = true;
		wereLayerAdjustmentsSupplied = false;

		doAllowProjectionOfType = new HashMap<>();

		for (ProjectionTypeCode t : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			doAllowProjectionOfType.put(t, true);
		}

		layers = new HashMap<>();

		// Initialize the known Layer Ids
//		Layer compositeLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY).build();
//		layers.put(compositeLayer.getLayerId(), compositeLayer);
//		Layer spanningLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING).build();
//		layers.put(spanningLayer.getLayerId(), spanningLayer);

		layerByProjectionType = new HashMap<>();

		targetedPrimaryLayer = null;
		targetedVeteranLayer = null;
		primaryLayer = null;
		veteranLayer = null;
		residualLayer = null;
		regenerationLayer = null;
		deadLayer = null;
		rank1Layer = null;

		history = null;

		reportingInfo = null;

		messages = new ArrayList<>();
	}

	public long getFeatureId() {
		return featureId;
	}

	public Long getPolygonNumber() {
		return polygonNumber;
	}

	public String getDistrict() {
		return district;
	}

	public String getMapSheet() {
		return mapSheet;
	}

	public String getMapQuad() {
		return mapQuad;
	}

	public String getMapSubQuad() {
		return mapSubQuad;
	}

	public InventoryStandard getInventoryStandard() {
		return inventoryStandard;
	}

	public LayerSummarizationModeCode getLayerSummarizationMode() {
		return layerSummarizationMode;
	}

	/**
	 * Getter. Note that VDYP7 has a function called "V7Ext_PolygonReferenceYear" which does not necessarily return this
	 * value - see {@link this.getMeasurementYear()}
	 *
	 * @return the reference year value, which may be null.
	 */
	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Integer getYearOfDeath() {
		return yearOfDeath;
	}

	/**
	 * @return the greater of <code>referenceYear</code> and <code>yearOfDeath</code>. If both are null, return null. If
	 *         one is null, return the other.
	 */
	public Integer getMeasurementYear() {
		Integer result = null;

		if (getReferenceYear() != null) {
			result = getReferenceYear();
		}

		if (getYearOfDeath() != null && (result == null || result < getYearOfDeath())) {
			result = getYearOfDeath();
		}

		return result;
	}

	public boolean getIsCoastal() {
		return isCoastal;
	}

	public String getForestInventoryZone() {
		return forestInventoryZone;
	}

	public String getBecZone() {
		return becZone;
	}

	public CfsEcoZoneCode getCfsEcoZone() {
		return cfsEcoZone;
	}

	public String getNonProductiveDescriptor() {
		return nonProductiveDescriptor;
	}

	public Double getPercentStockable() {
		return percentStockable;
	}

	public Double getPercentStockableDead() {
		return percentStockableDead;
	}

	public Double getYieldFactor() {
		return yieldFactor;
	}

	public Map<OtherVegetationTypeCode, OtherVegCoverDetails> getOtherVegetationTypes() {
		return Collections.unmodifiableMap(otherVegetationTypes);
	}

	public Map<NonVegetationTypeCode, NonVegCoverDetails> getNonVegetationTypes() {
		return Collections.unmodifiableMap(nonVegetationTypes);
	}

	public boolean getDoAllowProjection() {
		return doAllowProjection;
	}

	public boolean doAllowProjectionOfType(ProjectionTypeCode projectionType) {
		return doAllowProjectionOfType.containsKey(projectionType) && doAllowProjectionOfType.get(projectionType);
	}

	public boolean getWereLayerAdjustmentsSupplied() {
		return wereLayerAdjustmentsSupplied;
	}

	public Map<String, Layer> getLayers() {
		return layers;
	}

	public Layer getLayerByProjectionType(ProjectionTypeCode projectionTypeCode) {
		return layerByProjectionType.get(projectionTypeCode);
	}

	public Layer getTargetedPrimaryLayer() {
		return targetedPrimaryLayer;
	}

	public Layer getTargetedVeteranLayer() {
		return targetedVeteranLayer;
	}

	public Layer getPrimaryLayer() {
		return primaryLayer;
	}

	public Layer getVeteranLayer() {
		return veteranLayer;
	}

	public Layer getResidualLayer() {
		return residualLayer;
	}

	public Layer getRegenerationLayer() {
		return regenerationLayer;
	}

	public Layer getDeadLayer() {
		return deadLayer;
	}

	public Layer getRank1Layer() {
		return rank1Layer;
	}

	public History getHistory() {
		return history;
	}

	public PolygonReportingInfo getReportingInfo() {
		return reportingInfo;
	}

	public List<PolygonMessage> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	// MUTABLE data - this may vary over the lifetime of the object, but not after
	// the definition has been locked.

	public void lockDefinition() {
		definitionIsLocked = true;
	}

	void ensureUnlocked() {
		if (definitionIsLocked) {
			throw new IllegalStateException(this + " is locked and cannot be altered");
		}
	}

	public void addMessage(PolygonMessage message) {
		ensureUnlocked();

		switch (message.getSeverity()) {
		case FATAL_ERROR:
		case ERROR:
			logger.error(message.toString());
			break;
		case WARNING:
			logger.warn(message.toString());
			break;
		case INFORMATION:
		case STATUS:
			logger.info(message.toString());
			break;
		default:
			throw new IllegalArgumentException("Unknown message severity " + message.getSeverity() + " seen");
		}

		messages.add(message);
	}

	public void setLayerSummarizationMode(LayerSummarizationModeCode layerSummarizationMode) {
		ensureUnlocked();
		this.layerSummarizationMode = layerSummarizationMode;
	}

	public void setTargetedPrimaryLayer(Layer targetedPrimaryLayer) {
		ensureUnlocked();
		this.targetedPrimaryLayer = targetedPrimaryLayer;
	}

	public void setTargetedVeteranLayer(Layer targetedVeteranLayer) {
		ensureUnlocked();
		this.targetedVeteranLayer = targetedVeteranLayer;
	}

	public void setPrimaryLayer(Layer primaryLayer) {
		ensureUnlocked();
		this.primaryLayer = primaryLayer;
	}

	public void setVeteranLayer(Layer veteranLayer) {
		ensureUnlocked();
		this.veteranLayer = veteranLayer;
	}

	public void setResidualLayer(Layer residualLayer) {
		ensureUnlocked();
		this.residualLayer = residualLayer;
	}

	public void setRegenerationLayer(Layer regenerationLayer) {
		ensureUnlocked();
		this.regenerationLayer = regenerationLayer;
	}

	public void setDeadLayer(Layer deadLayer) {
		ensureUnlocked();
		this.deadLayer = deadLayer;
	}

	public void setRank1Layer(Layer rank1Layer) {
		ensureUnlocked();
		this.rank1Layer = rank1Layer;
	}

	void setLayerByProjectionType(ProjectionTypeCode layerProjectionType, Layer layer) {
		ensureUnlocked();
		Validate.isTrue(
				!layerByProjectionType.containsKey(layerProjectionType),
				"Polygon.setLayerByProjectionType: map entry must not already exist"
		);
		layerByProjectionType.put(layerProjectionType, layer);
	}

	public static class Builder {
		private Polygon polygon = new Polygon();

		public Builder featureId(long value) {
			polygon.featureId = value;
			return this;
		}

		public Builder polygonNumber(Long polygonNumber) {
			polygon.polygonNumber = polygonNumber;
			return this;
		}

		public Builder district(String district) {
			polygon.district = district;
			return this;
		}

		public Builder mapSheet(String mapSheet) {
			polygon.mapSheet = mapSheet;
			return this;
		}

		public Builder mapQuad(String mapQuad) {
			polygon.mapQuad = mapQuad;
			return this;
		}

		public Builder mapSubQuad(String mapSubQuad) {
			polygon.mapSubQuad = mapSubQuad;
			return this;
		}

		public Builder inventoryStandard(InventoryStandard inventoryStandard) {
			polygon.inventoryStandard = inventoryStandard;
			return this;
		}

		public Builder layerSummarizationMode(LayerSummarizationModeCode layerSummarizationMode) {
			polygon.layerSummarizationMode = layerSummarizationMode;
			return this;
		}

		public Builder referenceYear(Integer referenceYear) {
			polygon.referenceYear = referenceYear;
			return this;
		}

		public Builder yearOfDeath(Integer yearOfDeath) {
			polygon.yearOfDeath = yearOfDeath;
			return this;
		}

		public Builder isCoastal(boolean isCoastal) {
			polygon.isCoastal = isCoastal;
			return this;
		}

		public Builder forestInventoryZone(String forestInventoryZone) {
			polygon.forestInventoryZone = forestInventoryZone;
			return this;
		}

		public Builder becZone(String becZone) {
			polygon.becZone = becZone;
			return this;
		}

		public Builder cfsEcoZone(CfsEcoZoneCode cfsEcoZone) {
			polygon.cfsEcoZone = cfsEcoZone;
			return this;
		}

		public Builder nonProductiveDescriptor(String nonProductiveDescriptor) {
			polygon.nonProductiveDescriptor = nonProductiveDescriptor;
			return this;
		}

		public Builder percentStockable(Double percentStockable) {
			polygon.percentStockable = percentStockable;
			return this;
		}

		public Builder percentStockableDead(Double percentStockableDead) {
			polygon.percentStockableDead = percentStockableDead;
			return this;
		}

		public Builder yieldFactor(Double yieldFactor) {
			polygon.yieldFactor = yieldFactor;
			return this;
		}

		public Builder otherVegetationTypes(Map<OtherVegetationTypeCode, OtherVegCoverDetails> otherVegetationMap) {
			polygon.otherVegetationTypes = otherVegetationMap;
			return this;
		}

		public Builder nonVegetationTypes(Map<NonVegetationTypeCode, NonVegCoverDetails> nonVegetationMap) {
			polygon.nonVegetationTypes = nonVegetationMap;
			return this;
		}

		public Builder doAllowProjection(boolean doAllowProjection) {
			polygon.doAllowProjection = doAllowProjection;
			return this;
		}

		public Builder doAllowProjectionOfType(Map<ProjectionTypeCode, Boolean> doAllowProjectionOfType) {
			polygon.doAllowProjectionOfType = doAllowProjectionOfType;
			return this;
		}

		public Builder wereLayerAdjustmentsSupplied(boolean wereLayerAdjustmentsSupplied) {
			polygon.wereLayerAdjustmentsSupplied = wereLayerAdjustmentsSupplied;
			return this;
		}

		public Builder layers(Map<String, Layer> layers) {
			polygon.layers = layers;
			return this;
		}

		public Builder layerByProjectionType(Map<ProjectionTypeCode, Layer> layerByProjectionType) {
			polygon.layerByProjectionType = layerByProjectionType;
			return this;
		}

		public Builder history(History history) {
			polygon.history = history;
			return this;
		}

		public Builder reportingInfo(PolygonReportingInfo polygonReportingInfo) {
			polygon.reportingInfo = polygonReportingInfo;
			return this;
		}

		public Polygon build() {
			return polygon;
		}
	}

	/**
	 * <code>V7Ext_CompletedPolygonDefinition</code>
	 * <p>
	 * This polygon and all its child items has been read from input. Complete its definition by building intermediate
	 * data structures as needed and calculating estimates for values not supplied in the input.
	 *
	 * @param context
	 * @throws PolygonValidationException
	 */
	public void doCompleteDefinition(ProjectionContext context) throws PolygonValidationException {

		// Assign the projection type of each of the layers. This is set at construction time to
		// UNKNOWN.

		for (Layer layer : getLayers().values()) {
			layer.doCompleteDefinition();
		}

		for (Layer layer : getLayers().values()) {
			layer.doBuildSiteSpecies();
			layer.doCompleteSiteSpeciesSiteIndexInfo(context);
		}

		var rGrowthModel = new Reference<GrowthModelCode>();
		var rProcessingModel = new Reference<ProcessingModeCode>();
		var rPrimaryLayer = new Reference<Layer>();
		var rProjectionType = new Reference<ProjectionTypeCode>();

		calculateInitialProcessingModel(rGrowthModel, rProcessingModel, rPrimaryLayer, rProjectionType);

		for (Layer layer : getLayers().values()) {
			layer.doSortSiteSpecies(rGrowthModel.get());
		}

		Layer deadLayer = getDeadLayer();

		for (Layer layer : getLayers().values()) {

			layer.calculateEstimatedSiteIndex(context, rGrowthModel.get(), false /* do not prevent recursive calls */);

			if (layer == deadLayer && layer.getYearOfDeath() != null) {
				layer.determineAgeAtDeath();
			}
		}

		mergeLayers();

		doAdjustAllLayersSpeciesPercents(context);

		doEstimateLayerCrownClosures(context);

		doEstimateYieldFactor();

		doEstimateStockability();

		if (getInventoryStandard().equals(InventoryStandard.VRI)) {
			var primaryLayer = getPrimaryLayer();
			if (primaryLayer != null) {
				if (primaryLayer.getBasalArea() == null) {
					context.getErrorLog().addMessage(
							"Layer {0} Warning: VRI Inventory Standard but basal area value missing on primary layer; will be estimated",
							primaryLayer
					);
				}
				if (primaryLayer.getTreesPerHectare() == null) {
					context.getErrorLog().addMessage(
							"Layer {0} Warning: VRI Inventory Standard but trees-per-hectare value missing on primary layer; will be estimated",
							primaryLayer
					);
				}
			}
			var veteranLayer = getVeteranLayer();
			if (veteranLayer != null) {
				if (veteranLayer.getBasalArea() == null) {
					context.getErrorLog().addMessage(
							"Layer {0} Warning: VRI Inventory Standard but basal area value missing on veteran layer; will be estimated",
							veteranLayer
					);
				}
				if (veteranLayer.getTreesPerHectare() == null) {
					context.getErrorLog().addMessage(
							"Layer {0} Warning: VRI Inventory Standard but trees-per-hectare value missing on veteran layer; will be estimated",
							veteranLayer
					);
				}
			}
		}
	}

	/**
	 * <code>lcl_CalculateCC</code>
	 *
	 * Estimate the crown closures of all Layers (that will be projected) that haven't been supplied with one.
	 *
	 * @param context the projection context of the estimation
	 */
	private void doEstimateLayerCrownClosures(ProjectionContext context) {

		getLayers().values().stream()
				.filter(l -> doAllowProjectionOfType.get(l.getAssignedProjectionType()) && l.getCrownClosure() == null)
				.forEach(l -> l.estimateCrownClosure(context));
	}

	/**
	 * <b>lcl_AdjustAllLayersSpeciesPercents</b> and <b>lcl_AdjustOneLayerSpeciesPercents</b>
	 * <p>
	 * Adjust the percents of all Layers for which the sum of the percentages of their Stands is between 99 and 99.99 or
	 * between 100.01 and 101 to be 100. If the percentage is beyond this range, it is an error. If it is within 0.01 of
	 * 100, nothing is done.
	 *
	 * @param context the projection context of the operation
	 */
	private void doAdjustAllLayersSpeciesPercents(ProjectionContext context) {

		for (Layer layer : getLayers().values()) {

			tweakPercentages(layer).ifPresent(this::addMessage);
		}
	}

	Optional<PolygonMessage> tweakPercentages(Layer layer) {
		if (layer.getSp0sAsSupplied().size() == 0) {
			return Optional.empty();
		}

		double sumStandPercentages = layer.getSp0sAsSupplied().stream()
				.map(sp0 -> sp0.getSpeciesGroup().getSpeciesPercent()).reduce(0.0, (a, b) -> a + b);

		double difference = 100.0 - sumStandPercentages;
		double absDifference = Math.abs(difference);

		if (absDifference > 0.01 /* not close enough */) {
			if (absDifference <= 1.0) {

				/*
				 * The stand has a percentage that needs adjusting. Determine the SP0 and SP64 to which the adjustment
				 * is to be performed.
				 *
				 * If the stand is less than 100%, add the difference to the leading sp0.
				 *
				 * If the stand is more than 100%, subtract the difference from the last sp0. Ensure we have selected a
				 * species with a species percent at least as great as percent we are subtracting - negative percentages
				 * are not allowed.
				 *
				 * Finally, if the adjusted sp64 is a duplicate, always increase the first duplicate and reduce the last
				 * duplicate.
				 */

				Species targetSp0, targetSp64;
				int duplicatedSpeciesIndex;
				if (sumStandPercentages < 100.0) {
					Stand targetStand = layer.getSp0sByPercent().get(0);
					targetSp0 = targetStand.getSpeciesGroup();
					targetSp64 = targetStand.getSpeciesByPercent().get(0);
					duplicatedSpeciesIndex = 0;
				} else {
					Stand targetStand = null;
					for (int i = layer.getSp0sByPercent().size() - 1; i >= 0; i--) {
						if (layer.getSp0sByPercent().get(i).getSpeciesGroup().getSpeciesPercent() >= absDifference) {
							targetStand = layer.getSp0sByPercent().get(i);
							break;
						}
					}

					/* -some- sp0 has to have its % > difference... */
					Validate.isTrue(
							targetStand != null,
							"Polygon.doAdjustAllLayersSpeciesPercents: targetStand must not be null"
					);

					targetSp0 = targetStand.getSpeciesGroup();
					targetSp64 = targetStand.getSpeciesByPercent().get(targetStand.getSpeciesByPercent().size() - 1);
					duplicatedSpeciesIndex = targetSp64.getNDuplicates();
				}

				targetSp0.adjustSpeciesPercent(difference, 0);
				targetSp64.adjustSpeciesPercent(difference, duplicatedSpeciesIndex);

				logger.debug(
						"{}: adjusted percentage of layer {}, sp0 {}, sp64 {}, duplicate {} by {}", this, layer,
						targetSp0, targetSp64, duplicatedSpeciesIndex, difference
				);
			} else {
				disableProjectionsOfType(layer.determineProjectionType(this));
				return Optional.of(
						new PolygonMessage.Builder().layer(layer).details(
								ReturnCode.ERROR_PERCENTNOT100, MessageSeverityCode.ERROR,
								PolygonMessageKind.LAYER_PERCENTAGES_TOO_INACCURATE, Double.valueOf(sumStandPercentages)
						).build()
				);
			}
		}
		return Optional.empty();
	}

	/**
	 * <b>V7Ext_InitialProcessingModeToBeUsed</b>
	 * <p>
	 * Determine the processing mode to which the stand will be initially subjected. This is determined from the
	 * characteristics of the layer associated with the first projection type that has a layer from the list
	 * <code>ACTUAL_PROJECTION_TYPES_LIST</code> whose order is crucial to this method working successfully.
	 * <p>
	 * The processing mode is specified according to how the stand is defined. The mode is completely defined by the
	 * stand attributes and is not explicitly chosen by the caller.
	 * <p>
	 * <b>Remarks</b>
	 * <ul>
	 * <li>Focus of the decision has been changed from leading SP64 to SP0.
	 * <li>FIPSTART decision logic looks at Non-Productive status.
	 * <li>The list of possible FIP non-productive codes is any value rather than a subset.
	 * </ul>
	 *
	 * @param rGrowthModel    on output, will specify the underlying growth model which will be used to perform initial
	 *                        processing based on the properties currently assigned to the polygon.
	 * @param rProcessingMode on output, will specify the processing mode initially used to process the stand. This
	 *                        selection may be overridden when the model is actually run. This routine does, however,
	 *                        identify the initial values which will be used.
	 * @param rPrimaryLayer   the polygon layer determined to be its primary species
	 * @param rProjectionType the first projection type that has a primary layer, in the order given by
	 *                        <code>ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST</code>
	 */
	public void calculateInitialProcessingModel(
			Reference<GrowthModelCode> rGrowthModel, Reference<ProcessingModeCode> rProcessingMode,
			Reference<Layer> rPrimaryLayer, Reference<ProjectionTypeCode> rProjectionType
	) {

		if (rGrowthModel.isPresent() || rProcessingMode.isPresent()) {
			throw new IllegalStateException(
					"calculateInitialProcessingModel: reference arguments already have values."
			);
		}

		rGrowthModel.set(GrowthModelCode.getDefault());
		rProcessingMode.set(ProcessingModeCode.getDefault());

		for (ProjectionTypeCode pt : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			// Iterate until we find a projection type with a primary layer; that will determine
			// the (initial) model for all projection types.

			// Determine the primary layer of the stand.
			Layer ptPrimaryLayer = findPrimaryLayerByProjectionType(pt);
			if (ptPrimaryLayer != null) {

				rPrimaryLayer.set(ptPrimaryLayer);
				rProjectionType.set(pt);

				logger.debug("{}: Projection type {}: primary layer determined to be: {}", this, pt, ptPrimaryLayer);

				if (getInventoryStandard().equals(InventoryStandard.Silviculture)) {
					logger.debug(
							"   calculateInitialProcessingModel: implemented Classification Rule 1: SILV Standard Inventory treated as FIPSTART"
					);
					rGrowthModel.set(GrowthModelCode.FIP);
					rProcessingMode.set(ProcessingModeCode.FIP_Default);
				} else if (ptPrimaryLayer.getBasalArea() != null && ptPrimaryLayer.getBasalArea() > 0.0
						&& ptPrimaryLayer.getTreesPerHectare() != null && ptPrimaryLayer.getTreesPerHectare() > 0.0) {
					logger.debug(
							"   calculateInitialProcessingModel: implemented Classification Rule 2: BA and TPH both greater than 0 ({}, {})",
							ptPrimaryLayer.getBasalArea(), ptPrimaryLayer.getTreesPerHectare()
					);
					rGrowthModel.set(GrowthModelCode.VRI);
					rProcessingMode.set(ProcessingModeCode.VRI_Default);
				} else if (getInventoryStandard().equals(InventoryStandard.FIP)) {
					logger.debug(
							"   calculateInitialProcessingModel: implemented Classification Rule 3: FIP Standard Inventory"
					);
					rGrowthModel.set(GrowthModelCode.FIP);
					rProcessingMode.set(ProcessingModeCode.FIP_Default);
				} else {
					logger.debug(
							"   calculateInitialProcessingModel: implemented Classification Rule 4: all other cases"
					);
					rGrowthModel.set(GrowthModelCode.VRI);
					rProcessingMode.set(ProcessingModeCode.VRI_Default);
				}

				// Process only the first Projection Type for which there is a Primary layer.
				break;
			}
		}
	}

	/**
	 * <b>lcl_CalculateStockability</b>
	 * <p>
	 * Determine the polygon's Percent Stockable Land.
	 * <p>
	 * This implements the second approximation method describes in the document: IPSCB304.DOC. The single priviso is
	 * that if a percent forested land is already specified, use that in preference to computing a value. Must compute a
	 * yield factor through a call to 'lcl_CalculateYieldFactor' <b>prior</b> to calling this routine.
	 * <p>
	 * When calculating stockability, placed a minimum value of '1' if less than 1 so that the core library does not
	 * interpret that as a missing value and attempt to fill it in using its own logic.
	 * <p>
	 * In the presence of a dead layer, we will use a hard coded default of 85% if no value is supplied (after
	 * discussions with Sam Otukol).
	 */
	private void doEstimateStockability() throws PolygonValidationException {

		if (percentStockable != null) {
			logger.debug("{}: using supplied stockability of {}", this, percentStockable);
			return;
		}

		if (getDeadLayer() != null) {
			// With no explicit stockability, and a dead layer, use DEAD_STOCKABLE == 85%
			this.percentStockable = Vdyp7Constants.DEAD_STOCKABLE;
			logger.debug(
					"{}: no explicit stockability given and dead layer present; using stockability {}", this,
					percentStockable
			);
			return;
		}

		var primaryLayer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);

		double years = 0;

		if (primaryLayer != null) {
			var leadingSpecies = primaryLayer.determineLeadingSp0(0);
			if (leadingSpecies == null || leadingSpecies.getSpeciesByPercent().isEmpty()) {
				throw new PolygonValidationException(
						new ValidationMessage(ValidationMessageKind.NO_LEADING_SPECIES, this, primaryLayer)
				);
			}

			var measurementYear = this.getMeasurementYear();

			if (getHistory() != null && getHistory().getSilvicultureBase() == SilviculturalBaseCode.DISTURBED
					&& measurementYear != null && getHistory().getDisturbanceStartYear() > 0) {
				/*
				 * 2004/02/16: According to IPSCB304, the following rules are applied: Subsequent discussion (Bartram,
				 * 2002b) provided more detail on what further constitutes a recent disturbance. One or more history
				 * records with History Code (SIV_BASE) = 'DI' must exist for the polygon. Disturbance year will be
				 * determined from ACTIVITY_START_DATE and in the case where multiple 'DI' records exist per polygon the
				 * earliest historic year will be used. Years from disturbance then equals REFERENCE_ YEAR minus
				 * disturbance year. When multiple layers exist reference year (on the Layer Table) will correspond to
				 * the layer used to determine disturbance year. Only history records that predate the reference year
				 * will qualify as a disturbance indicator.
				 *
				 * and then later in the document:
				 *
				 * First define: YEARS = years from disturbance if history records exist with SILV_BASE = 'DI' or age of
				 * the leading species age if no such history records exist
				 */

				if (getHistory().getDisturbanceStartYear() > measurementYear) {
					years = leadingSpecies.getSpeciesGroup().getTotalAge();
				} else {
					years = (double) measurementYear - getHistory().getDisturbanceStartYear();
					years = VdypMath.clamp(years, 0, leadingSpecies.getSpeciesGroup().getTotalAge());
				}
			} else {
				years = leadingSpecies.getSpeciesGroup().getTotalAge();
			}

			logger.debug(
					"{}: years since disturbance: total age: {}; measurement year: {}; year of disturbance {}; years since disturbance",
					this, leadingSpecies.getSpeciesGroup().getTotalAge(), measurementYear,
					getHistory().getDisturbanceStartYear(), years
			);
		}

		double p1;

		if (years < Vdyp7Constants.YEAR_LIMIT) {
			p1 = Vdyp7Constants.P1_MAX;
		} else if (years > Vdyp7Constants.YEAR_MAX) {
			p1 = 0;
		} else {
			var exponent = (years - Vdyp7Constants.YEAR_LIMIT) / Vdyp7Constants.YEAR_HALF;
			var lP1 = Math.pow(0.5, exponent) - (1.0 - Vdyp7Constants.P1_MAX);
			if (lP1 < 0) {
				p1 = 0;
			} else {
				p1 = lP1;
			}

			logger.debug(
					"{}: stocking potential: unconstrained stocking potential (lP1): {}; final stocking potential (p1): {}",
					lP1, p1
			);
		}

		var crownClosure = 0;
		if (primaryLayer != null && primaryLayer.getCrownClosure() != null) {
			crownClosure = (short) Math.max(crownClosure, primaryLayer.getCrownClosure());
			logger.debug(
					"{}: crown closure: primary layer crown closure: {}; value used: {}", primaryLayer,
					primaryLayer.getCrownClosure(), crownClosure
			);
		}

		var ccFullyStocked = isCoastal ? Vdyp7Constants.CC_COAST : Vdyp7Constants.CC_INTERIOR;

		double percentStocked;
		if (crownClosure >= ccFullyStocked) {
			percentStocked = 100;
		} else {
			percentStocked = crownClosure / ccFullyStocked * 100;
		}

		logger.debug("{}: percentStocked: {}; ccFullyStocked: {}", this, percentStocked, ccFullyStocked);

		double percentStockability;
		if (inventoryStandard == InventoryStandard.VRI) {

			var percentCrownSpaces = percentStocked - crownClosure;

			var estimatedPercentStockable = otherVegetationTypes.get(OtherVegetationTypeCode.Shrub)
					.otherVegCoverPercent()
					+ otherVegetationTypes.get(OtherVegetationTypeCode.Herb).otherVegCoverPercent()
					+ nonVegetationTypes.get(NonVegetationTypeCode.ExposedSoil).nonVegCoverPercent()
					+ nonVegetationTypes.get(NonVegetationTypeCode.BurnedArea).nonVegCoverPercent();

			estimatedPercentStockable = VdypMath.clamp(estimatedPercentStockable, 0, 100);

			var percentNonStockable = nonVegetationTypes.values().stream()
					.filter(
							n -> n.getNonVegCoverType() != NonVegetationTypeCode.BurnedArea
									&& n.getNonVegCoverType() != NonVegetationTypeCode.ExposedSoil
					).map(n -> n.nonVegCoverPercent()).reduce(
							otherVegetationTypes.get(OtherVegetationTypeCode.Bryoid).otherVegCoverPercent(),
							(a, b) -> a + b
					);
			percentNonStockable = VdypMath.clamp(percentNonStockable, 0, 100);

			var percentUnaccounted = 100 - (crownClosure + estimatedPercentStockable + percentNonStockable);
			percentUnaccounted = Math.max(percentUnaccounted, 0);

			var percentAbsorbable = estimatedPercentStockable + percentNonStockable + percentUnaccounted;
			double percentStockableAbsorbable, percentUnaccountedAbsorbable;
			if (percentAbsorbable > 0) {
				percentStockableAbsorbable = percentCrownSpaces * (estimatedPercentStockable / percentAbsorbable);
				percentUnaccountedAbsorbable = percentCrownSpaces * (percentUnaccounted / percentAbsorbable);
			} else {
				percentStockableAbsorbable = 0;
				percentUnaccountedAbsorbable = 0;
			}

			double netPercentStockable, netPercentUnaccounted;
			if (percentCrownSpaces < percentAbsorbable) {
				netPercentStockable = estimatedPercentStockable - percentStockableAbsorbable;
				netPercentUnaccounted = percentUnaccounted - percentUnaccountedAbsorbable;
			} else {
				netPercentStockable = 0;
				netPercentUnaccounted = 0;
			}

			percentStockability = percentStocked + (netPercentStockable + netPercentUnaccounted) * p1;
			percentStockability = VdypMath.clamp(percentStockability, 0, 100);
		} else {
			// FIP, Silvaculture, unknown...

			var percentUnaccounted = 100.0 - percentStocked;
			percentUnaccounted = VdypMath.clamp(percentUnaccounted, 0, 100);

			percentStockability = percentStocked + (percentUnaccounted * 0.9 * p1);
			percentStockability = VdypMath.clamp(percentStockability, 0, 100);

			// If we are also Non-Productive, adjust according to the yield factor (see IPSCB202).

			if (nonProductiveDescriptor != null && yieldFactor >= 0.0) {
				percentStockability *= yieldFactor;
			}
		}

		if (percentStockability < 1.0) {
			percentStockability = 1.0;
		}

		percentStockable = percentStockability;

		logger.debug("{}: percent stockable estimated at: {}", this, percentStockable);
	}

	/**
	 * <code>lcl_CalculateYieldFactor</code>
	 *
	 * If the yield factor for this polygon has not been calculated, do so.
	 *
	 * @throws PolygonValidationException
	 */
	private void doEstimateYieldFactor() throws PolygonValidationException {

		if (yieldFactor != null) {
			logger.debug(
					"{}: Yield Factor rule 1 applies: An explicit yield factor of {} was supplied, using that factor",
					this, yieldFactor
			);
			if (yieldFactor == 0.0) {
				logger.debug("   explicit yield factor of 0.0 was supplied, substituting the value 1.0");
				yieldFactor = 1.0;
			}
		} else if (inventoryStandard == InventoryStandard.FIP && nonProductiveDescriptor != null) {
			var primaryLayer = this.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
			if (primaryLayer == null) {
				throw new PolygonValidationException(
						new ValidationMessage(ValidationMessageKind.PRIMARY_LAYER_NOT_FOUND, this)
				);
			}

			if (primaryLayer.getCrownClosure() != null && primaryLayer.getCrownClosure() <= 50) {
				yieldFactor = primaryLayer.getCrownClosure() / 100.0;
			} else {
				yieldFactor = 0.5;
			}
			logger.debug(
					"{}: Yield Factor rule 2 applies: yieldFactor = min(0.5, crownClosure/100.0) = {}", this,
					yieldFactor
			);
		} else {
			yieldFactor = 1.0;
			logger.debug("{}: Yield Factor rule 3 applies. Rule 1 and 2 do not apply so use no yield factor.", this);
		}
	}

	/**
	 * <code>V7Int_DetermineProjectionTypeStockability</code>
	 * <p>
	 * Determines the portion of the polygon stockability that is attributed to the given projection type.
	 * <p>
	 * All layers should have a % Stockable (stockability) attributes. The % stockable values should be determined as
	 * follows:
	 * <ul>
	 * <li>Layer_type_Code V: %stockable = CC%
	 * <li>Layer_Type_Code R: %stockable = 100 ? (%dead + V layer %stockable)
	 * <li>Layer_Type_Code Y: %stockable = %dead
	 * <li>Layer_Type_Code D: %stockable = %dead
	 * </ul>
	 * If all is done well, the sum of %stockable for V, R and Y should not exceed 100%.
	 * <p>
	 * Remarks:
	 *
	 * <pre>
	 *       A: If there is no veteran layer:
	 *          1: Only one residual layer + D layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             Stockable_Dead = StockPct * Dead_Pct /100;
	 *             Stockable_Primary = StockPct - Stockable_Dead;
	 *             Stockable_Regeneration = Stockable_Dead;
	 *
	 *          2: Two residual layers + D layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             L2 is the residual layer
	 *                a) If BA of L1 and L2 are both available, then,
	 *                   Stockable_Dead = StockPct * Pct_Dead/100;
	 *                   Stockable_Primary = (StockPct - Stockable_Dead) * BA1/(BA1+BA2);
	 *                   Stockable_Residual =(StockPct - Stockable_Dead) * BA2/(BA1+BA2);
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *                b) If BA of L1 OR L2 are not available, then,
	 *                   Stockable_Dead = StockPct * Pct_Dead/100;
	 *                   Stockable_Primary = StockPct - Stockable_Dead;
	 *                   Stockable_Residual = Stockable_Dead;
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *          3: Three residual layer + D layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             L2 is the residual layer
	 *             L3 is assumed to be the Young layer
	 *                a) If BA of L1 and L2 are both available, then,
	 *                   Stockable_Dead = StockPct*Pct_Dead/100;
	 *                   Stockable_Primary = (StockPct - Stockable_Dead)* BA1/(BA1+BA2);
	 *                   Stockable_Residual = ( StockPct - Stockable_Dead)* BA2/(BA1+BA2);
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *                b) If BA of L1 OR L2 are not available, then,
	 *                   Stockable_Dead = StockPct*Pct_Dead/100;
	 *                   Stockable_Primary = StockPct - Stockable_Dead;
	 *                   Stockable_Residual = Stockable_Dead;
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *
	 *       B: If there is a veteran layer:
	 *          1: Only one residual layer + D layer + Veteran layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             Stockable_Vet = CCvet%
	 *             Stockable_Dead = StockPct * Dead_Pct /100;
	 *             Stockable_Primary = StockPct - Stockable_Dead- Stockable_Vet;
	 *             Stockable_Young = Stockable_Dead;
	 *
	 *          2: Two residual layer + D layer + Veteran layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             L2 is the residual layer
	 *                a) If BA of L1 and L2 are both available, then,
	 *                   Stockable_Vet = CCvet%
	 *                   Stockable_Dead = StockPct*Pct_Dead/100
	 *                   Stockable_Primary = (StockPct - Stockable_Dead - Stockable_Vet) * BA1/(BA1+BA2)
	 *                   Stockable_Residual =(StockPct - Stockable_Dead - Stockable_Vet) * BA2/(BA1+BA2)
	 *                   Stockable_Young = Stockable_Dead
	 *
	 *                b) If BA of L1 OR L2 are not available, then,
	 *                   Stockable_Vet = CCvet%
	 *                   Stockable_Dead = StockPct*Pct_Dead/100
	 *                   Stockable_Primary = StockPct - Stockable_Dead - Stockable_Vet
	 *                   Stockable_Residual = Stockable_Dead;
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *          3: Three residual layer + D layer + Veteran layer:
	 *             L1 is the primary layer and rank 1 layer
	 *             L2 is the residual layer
	 *             L3 is assumed to be the Young layer
	 *                a) If BA of L1 and L2 are both available, then,
	 *                   Stockable_Vet = CCvet%
	 *                   Stockable_Dead = StockPct*Pct_Dead/100;
	 *                   Stockable_Primary = (StockPct - Stockable_Dead - Stockable_Vet) * BA1/(BA1+BA2);
	 *                   Stockable_Residual =(StockPct - Stockable_Dead - Stockable_Vet) * BA2/(BA1+BA2);
	 *                   Stockable_Regeneration = Stockable_Dead;
	 *
	 *                b) If BA of L1 OR L2 are not available, then,
	 *                   Stockable_Vet = CCvet%
	 *                   Stockable_Dead = StockPct*Pct_Dead/100;
	 *                   Stockable_Primary = StockPct - Stockable_Dead- Stockable_Vet;
	 *                   Stockable_Residual = Stockable_Dead;
	 *                   Stockable_Regeneration = Stockable_Dead;
	 * </pre>
	 */
	public double determineStockabilityByProjectionType(ProjectionTypeCode projectionType) {

		Double polygonPercentStockable = this.getPercentStockable();

		double primaryPercentStockable = 0.0;
		double veteranPercentStockable = 0.0;
		double deadPercentStockable = 0.0;
		double regenerationPercentStockable = 0.0;
		double residualPercentStockable = 0.0;

		Layer primaryLayer = this.getTargetedPrimaryLayer();
		if (primaryLayer == null) {
			primaryLayer = this.getPrimaryLayer();
		}
		if (primaryLayer == null) {
			primaryLayer = this.getRank1Layer();
		}

		Layer veteranLayer = this.getTargetedVeteranLayer();
		if (veteranLayer == null) {
			veteranLayer = this.getVeteranLayer();
		}

		Layer deadLayer = getDeadLayer();
		Layer regenerationLayer = getRegenerationLayer();
		Layer residualLayer = getResidualLayer();

		if (polygonPercentStockable == null) {
			polygonPercentStockable = 85.0; /*
											 * where does this come from??? PM 05-25: Seems like default max poly
											 * stockability
											 */
		}

		if (deadLayer != null) {
			Double m = NullMath
					.max(getPercentStockableDead(), deadLayer.getPercentStockable(), (a, b) -> Math.max(a, b), null);
			if (m != null && getPercentStockable() != null) {
				deadPercentStockable = getPercentStockable() * m / 100.0;
			}
			if (deadPercentStockable < 1.0) {
				deadPercentStockable = 1.0;
			}
		}

		if (regenerationLayer != null && deadPercentStockable > 0.0) {
			regenerationPercentStockable = deadPercentStockable;
		} else if (regenerationLayer != null) {
			Double m = NullMath.max(
					getPercentStockableDead(), regenerationLayer.getPercentStockable(), (a, b) -> Math.max(a, b), null
			);
			if (m != null && getPercentStockable() != null) {
				regenerationPercentStockable = getPercentStockable() * m / 100.0;
			}
			if (regenerationPercentStockable < 1.0) {
				regenerationPercentStockable = 1.0;
			}
		}

		if (veteranLayer != null) {
			if (veteranLayer.getCrownClosure() != null && veteranLayer.getCrownClosure() >= 1.0) {
				veteranPercentStockable = veteranLayer.getCrownClosure();
			} else {
				veteranPercentStockable = 1.0;
			}
		}

		if (primaryLayer != null && residualLayer != null) {
			Double primaryBasalArea = primaryLayer.getBasalArea();
			Double residualBasalArea = residualLayer.getBasalArea();

			primaryPercentStockable = polygonPercentStockable - veteranPercentStockable - deadPercentStockable
					- regenerationPercentStockable;
			residualPercentStockable = primaryPercentStockable;

			if (primaryBasalArea != null && residualBasalArea != null) {

				primaryPercentStockable *= (primaryBasalArea / (primaryBasalArea + residualBasalArea));
				residualPercentStockable *= (residualBasalArea / (primaryBasalArea + residualBasalArea));

				if (primaryPercentStockable <= 1.0) {
					primaryPercentStockable = 1.0;
				}
				if (residualPercentStockable <= 1.0) {
					residualPercentStockable = 1.0;
				}
			} else {

				residualPercentStockable = Math.max(deadPercentStockable, 1.0);
				primaryPercentStockable -= residualPercentStockable;
			}

		} else if (primaryLayer != null) {
			primaryPercentStockable = polygonPercentStockable - veteranPercentStockable - deadPercentStockable
					- regenerationPercentStockable;
			if (primaryPercentStockable <= 1.0) {
				primaryPercentStockable = 1.0;
			}
		} else if (residualLayer != null) {
			residualPercentStockable = polygonPercentStockable - veteranPercentStockable - deadPercentStockable
					- regenerationPercentStockable;
			if (residualPercentStockable <= 1.0) {
				residualPercentStockable = 1.0;
			}
		}

		switch (projectionType) {
		case DEAD:
			return deadPercentStockable;
		case PRIMARY:
			return primaryPercentStockable;
		case REGENERATION:
			return regenerationPercentStockable;
		case RESIDUAL:
			return residualPercentStockable;
		case VETERAN:
			return veteranPercentStockable;
		default:
			return 0.0;
		}
	}

	public Double determineStandAgeAtYear(Integer year) {
		Double standAgeAtYear = null;

		Layer primaryLayer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
		if (primaryLayer != null) {
			standAgeAtYear = primaryLayer.determineLayerAgeAtYear(year);
		}

		return standAgeAtYear;
	}

	public Layer findSpecificLayer(String layerId) {

		Layer selectedLayer = null;

		if (layerId == null) {
			throw new IllegalArgumentException("findSpecificLayer: layerId is null");
		} else if (Vdyp7Constants.VDYP7_LAYER_ID_SPANNING.equals(layerId)) {
			selectedLayer = getLayers().get(layerId);
		} else if (Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY.equals(layerId)) {
			selectedLayer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
		} else {
			for (Layer l : getLayers().values()) {
				if (layerId.equals(l.getLayerId())) {
					selectedLayer = l;
					break;
				}
			}
		}

		return selectedLayer;
	}

	private final String POLYGON_DESCRIPTOR_FORMAT = "%-7s%10s%-3s%5d";

	public String buildPolygonDescriptor(int year) {

		String mapSheet = this.mapSheet.length() > 7 ? this.mapSheet.substring(0, 7) : this.mapSheet;
		String district = this.district == null ? ""
				: this.district.length() > 3 ? this.district.substring(0, 3) : this.district;
		var descriptor = String
				.format(POLYGON_DESCRIPTOR_FORMAT, mapSheet, Long.toString(polygonNumber), district, year);

		return descriptor;
	}

	public String buildPolygonDescriptor() {

		return buildPolygonDescriptor(this.getMeasurementYear());
	}

	/**
	 * Assign <code>newDeadLayer</code> as the dead layer of this polygon. Assign year-of-death of this layer to the
	 * maximum of (<code>yearOfDeath</code>, the layer's year of death, and this polygon's yearOfDeath). Assign
	 * percent-stockable of this layer to the maximum of (<code>percentStockKilled</code>,
	 * <code>newDeadLayer.getPercentStockable()</code>, and this polygon's percentStockableDead). <code>null</code> is
	 * never greater than other values in a comparison.
	 * <p>
	 * This method does -not- consider whether the polygon already has a dead layer - it simply replaces the existing
	 * one if there is one. If this is not the desired behaviour, call <code>getDeadLayer</code> before calling this
	 * method to determine if a dead layer has already been assigned.
	 *
	 * @param newDeadLayer       the new dead layer
	 * @param yearOfDeath        as described
	 * @param percentStockKilled as described
	 */
	public void assignDeadLayer(Layer newDeadLayer, Integer yearOfDeath, Double percentStockKilled) {

		yearOfDeath = NullMath
				.max(yearOfDeath, newDeadLayer.getYearOfDeath(), (a, b) -> Math.max(a, b), Vdyp7Constants.EMPTY_INT);
		yearOfDeath = NullMath.max(yearOfDeath, getYearOfDeath(), (a, b) -> Math.max(a, b), Vdyp7Constants.EMPTY_INT);

		if (yearOfDeath == null) {
			yearOfDeath = getReferenceYear();
		}

		logger.debug("assignDeadLayer: using year-of-death {}", yearOfDeath);

		percentStockKilled = NullMath
				.max(percentStockableDead, percentStockKilled, (a, b) -> Math.max(a, b), Vdyp7Constants.EMPTY_DECIMAL);
		percentStockKilled = NullMath.max(
				newDeadLayer.getPercentStockable(), percentStockKilled, (a, b) -> Math.max(a, b),
				Vdyp7Constants.EMPTY_DECIMAL
		);

		logger.debug("{}: percent stockable land killed: {}", this, percentStockKilled);

		setDeadLayer(newDeadLayer);
		deadLayer.setAsDeadLayer(yearOfDeath, percentStockKilled);
	}

	public void disableProjectionsOfType(ProjectionTypeCode layerProjectionType) {
		if (layerProjectionType == ProjectionTypeCode.UNKNOWN) {
			doAllowProjection = false;
		} else {
			doAllowProjectionOfType.put(layerProjectionType, false);
		}
	}

	/**
	 * <b>V7Int_DetermineProjectionTypePrimaryLayer</b>
	 * <p>
	 * For a given projection type, determine the 'primary' layer for that projection type.
	 * <p>
	 * Passing ProjectionTypeCode.UNKNOWN indicates to this routine to scan from the first projection type through to
	 * the last projection type, returning the first primary layer it finds in any of the projection types.
	 *
	 * @param projectionType the projection type for which you want the primary layer.
	 * @return the Layer for the given Projection Type. <code>null</code> is returned if there is no primary layer for
	 *         the given projection type The there is no layer for that projection type.
	 */
	public Layer findPrimaryLayerByProjectionType(ProjectionTypeCode projectionType) {

		Layer primaryLayer = null;

		switch (projectionType) {
		case PRIMARY:
			primaryLayer = determinePrimaryLayer();
			break;
		case VETERAN:
			primaryLayer = determineVeteranLayer();
			break;
		case RESIDUAL:
			primaryLayer = getResidualLayer();
			break;
		case REGENERATION:
			primaryLayer = getRegenerationLayer();
			break;
		case DEAD:
			primaryLayer = getDeadLayer();
			break;
		case UNKNOWN:
			for (var pType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
				primaryLayer = findPrimaryLayerByProjectionType(pType);
				if (primaryLayer != null) {
					break;
				}
			}
			break;
		case DO_NOT_PROJECT:
		default:
			break;
		}

		return primaryLayer;
	}

	private Layer determinePrimaryLayer() {

		if (getPrimaryLayer() == null) {
			try {
				mergeLayers();
			} catch (PolygonValidationException e) {
				logger.warn(
						"{}: saw PolygonValidationException when determining primary layer{}", this,
						e.getMessage() != null ? "; reason = " + e.getMessage() : ""
				);
			}
		}

		return getPrimaryLayer();
	}

	private Layer determineVeteranLayer() {

		if (getVeteranLayer() == null) {
			try {
				mergeLayers();
			} catch (PolygonValidationException e) {
				logger.warn(
						"{}: saw PolygonValidationException when determining primary layer{}", this,
						e.getMessage() != null ? "; reason = " + e.getMessage() : ""
				);
			}
		}

		return getVeteranLayer();
	}

	private void mergeLayers() throws PolygonValidationException {

		InventoryStandard inventoryStandard = getInventoryStandard();
		switch (inventoryStandard) {
		case FIP:
		case Silviculture: {
			setLayerSummarizationMode(LayerSummarizationModeCode.TwoLayer);

			logger.debug("{}: selected FIP Inventory Layer Summarization Mode: {}", this, getLayerSummarizationMode());

			prepareFip2Layer();
			break;
		}
		case VRI:
			setLayerSummarizationMode(LayerSummarizationModeCode.TwoLayer);

			logger.debug("{}: selected VRI Inventory Layer Summarization Mode: {}", this, getLayerSummarizationMode());

			prepareVri2Layer();
			break;

		case Unknown:
		default:
			throw new PolygonValidationException(
					new ValidationMessage(
							ValidationMessageKind.UNRECOGNIZED_INVENTORY_STANDARD_CODE, this, inventoryStandard
					)
			);
		}

		logger.debug(
				"{}: after merging, selected primary layer: {}", this,
				getPrimaryLayer() == null ? "none" : getPrimaryLayer().toDetailedString()
		);
		logger.debug(
				"{}: after merging, selected veteran layer: {}", this,
				getVeteranLayer() == null ? "none" : getVeteranLayer().toDetailedString()
		);
	}

	/**
	 * <code>lcl_PrepareFIP2Layer</code>
	 * <p>
	 * Select the Primary and Veteran Layers based on IPSCB460 for FIP Inventory Polygons
	 * <p>
	 * According to IPSCB460:
	 * <ul>
	 * <li>A primary layer is defined to be the Rank 1 Layer.
	 * <li>A veteran layer is defined to be
	 * <ul>
	 * <li>a non-primary layer with layer Id '1'
	 * <li>has 1.0 <= CC <= 10.0
	 * <li>rank code not specified
	 * <li>crown closure <= 5.0%
	 * <li>all species (sp64) in the layer have height >= 10.0m.
	 * </ul>
	 * </ul>
	 * Layers targeted as the primary or veteran VDYP7 layer are used without consideration to any other selection
	 * criteria.
	 * <p>
	 */
	private void prepareFip2Layer() {
		setPrimaryLayer(selectPrimaryLayer());
		setVeteranLayer(selectFipVeteranLayer());
	}

	/**
	 * <code>lcl_PrepareVri2Layer</code>
	 * <p>
	 * Select the Primary and Veteran Layers based on IPSCB460 for VRI Inventory Polygons. Neither may be selected; in
	 * particular, if a primary is not selected, a veteran will not be selected.
	 * <p>
	 * According to IPSCB460:
	 * <ul>
	 * <li>A primary layer is defined to be the Rank 1 Layer.
	 * <li>A veteran layer is defined to be
	 * <ul>
	 * <li>a non-primary layer with layer Id '1'
	 * <li>has 1.0 <= CC <= 10.0
	 * <li>all species (sp64) in the layer have height >= 10.0m.
	 * <li>has leading species age > 140 (original VDYP7 code does not consider this)
	 * <li>has 1.0 <= TPH < 25.0 (original VDYP7 code does not consider this)
	 * <li>has diameter > 23.0 (original VDYP7 code does not consider this)
	 * </ul>
	 * </ul>
	 * Layers targeted as the primary or veteran VDYP7 layer are used without consideration to any other selection
	 * criteria.
	 * <p>
	 */
	private void prepareVri2Layer() {
		setPrimaryLayer(selectPrimaryLayer());
		setVeteranLayer(selectVriVeteranLayer());
	}

	/** vdyp7merge local function */
	private Layer selectPrimaryLayer() {

		Layer selectedPrimaryLayer = null;

		logger.debug("{}: determining VDYP primary layer", this);

		Layer deadLayer = getDeadLayer();
		logger.debug("{}: polygon dead layer is {}", this, deadLayer == null ? "not set" : deadLayer.getLayerId());

		if (getTargetedPrimaryLayer() != null) {

			selectedPrimaryLayer = getTargetedPrimaryLayer();
			logger.debug("   layer {} is identified/targeted as the primary layer", selectedPrimaryLayer);
		} else if (getRank1Layer() != null && getRank1Layer() != deadLayer) {

			selectedPrimaryLayer = getRank1Layer();
			logger.debug("   layer {} is primary layer due to being the rank 1 layer", selectedPrimaryLayer);
		} else if (getLayers().size() > 0) {

			// Find the first live-stem Layer that satisfies the criteria. If none do, choose any
			// live-stem Layer.
			Layer fallbackChoice = null;

			for (Layer candidate : getLayers().values().stream().filter(l -> !l.getIsDeadLayer()).toList()) {
				if (fallbackChoice == null) {
					fallbackChoice = candidate;
				}

				Stand leadSp0 = candidate.determineLeadingSp0(0);
				if (leadSp0 != null) {
					if (candidate.getCrownClosure() <= 10.0
							&& leadSp0.getSpeciesGroup().getTotalAge() >= leadSp0.determineMaturityAge()) {
						// candidate is really a veteran layer
						continue;
					} else {
						selectedPrimaryLayer = candidate;
						break;
					}
				}
			}

			if (selectedPrimaryLayer == null) {
				selectedPrimaryLayer = fallbackChoice;
			}

			if (selectedPrimaryLayer != null) {
				var message = new PolygonMessage.Builder().layer(selectedPrimaryLayer)
						.details(
								ReturnCode.SUCCESS, MessageSeverityCode.INFORMATION,
								PolygonMessageKind.NO_PRIMARY_LAYER_SUPPLIED, selectedPrimaryLayer.getLayerId()
						).build();

				addMessage(message);
			}
		}

		return selectedPrimaryLayer;
	}

	/** vdyp7merge local function */
	private Layer selectFipVeteranLayer() {

		Layer veteranLayer = getVeteranLayer();

		if (veteranLayer == null && getPrimaryLayer() != null) {

			if (getTargetedVeteranLayer() != null) {

				veteranLayer = getTargetedVeteranLayer();
				logger.debug("{}: layer {} identified as targeted veteran layer.", this, veteranLayer.getLayerId());
			} else {

				logger.debug("{}: no Targeted VDYP7 Veteran Layer identified.", this);
				logger.debug("   scanning for candidate layer that matches the following criteria:");
				logger.debug("   1. Non Primary Layer");
				logger.debug("   2. Must be Layer '1'");
				logger.debug("   3. Must not be Rank '1'");
				logger.debug("   4. 0.0% <= CC <= 5.0%");

				for (Layer candidate : getLayers().values()) {

					logger.debug("{}: current statistics:", this);
					logger.debug("   layer ID: '{}'", candidate.getLayerId());
					logger.debug("   rank code: '{}'", candidate.getRankCode());
					logger.debug("   is primary layer? {}", candidate == getPrimaryLayer() ? "Yes" : "No");
					logger.debug(
							"   crown closure: {}",
							candidate.getCrownClosure() == null ? 0.0
									: Math.round(candidate.getCrownClosure() * 100.0) / 100.0
					);

					if (candidate != getPrimaryLayer() && "1".equals(candidate.getLayerId())
							&& candidate.getCrownClosure() >= 0.0 && candidate.getCrownClosure() <= 5.0) {

						String rankCode = candidate.getRankCode();
						if (rankCode == null && candidate.doesHeightExceed(Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT)
								&& !candidate.getSp0sAsSupplied().isEmpty()) {
							veteranLayer = candidate;
							break;
						}
					}
				}

				if (veteranLayer != null) {
					veteranLayer.setDoIncludeWithProjection(true);
					veteranLayer.setVdyp7LayerCode(ProjectionTypeCode.VETERAN);

					logger.debug(
							"{}: layer {} passes all criteria and will be processed as a veteran layer", this,
							veteranLayer.getLayerId()
					);
				} else {
					logger.warn(
							"{}: no layer with id \"1\" that is not the primary layer, has 0 <= CC <= 5.0, no rank code"
									+ " and height at least {}m was found; hence, there is no identified veteran layer",
							this, Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT
					);
				}
			}
		}

		return veteranLayer;
	}

	/** vdyp7merge local function */
	private Layer selectVriVeteranLayer() {

		Layer selectedVeteranLayer = null;

		if (getPrimaryLayer() == null) {
			logger.debug("{}: stand contains no primary layer, therefore stand can not contain a veteran layer", this);
		} else if (getVeteranLayer() == null) {

			if (getTargetedVeteranLayer() != null) {
				selectedVeteranLayer = getTargetedVeteranLayer();
				logger.debug(
						"{}: targeted veteran layer '{}' identified as veteran layer", this,
						selectedVeteranLayer.getLayerId()
				);
			} else {
				for (Layer candidate : getLayers().values()) {

					if (candidate != getPrimaryLayer() && "1".equals(candidate.getLayerId())) {

						Species leadSp64 = candidate.determineLeadingSp64(0);
						if (leadSp64 != null) {
							logger.debug(
									"{}: testing Layer '{}' to see if it meets the veteran Layer thresholds...", this,
									candidate.getLayerId()
							);

							// TODO: add detailed debug logging at lcl_PrepareVRI2Layer, lines 1261 - 1312.

							double dbh = Math.sqrt(
									candidate.getBasalArea() / candidate.getTreesPerHectare() / Vdyp7Constants.PI_40K
							);

							if (leadSp64.getTotalAge() > Vdyp7Constants.MIN_VETERAN_LAYER_AGE
									&& candidate.getBasalArea() >= 0.0
									&& candidate.getCrownClosure() >= Vdyp7Constants.MIN_VETERAN_LAYER_CROWN_CLOSURE
									&& candidate.getCrownClosure() <= Vdyp7Constants.MAX_VETERAN_LAYER_CROWN_CLOSURE
									&& candidate.getTreesPerHectare() >= Vdyp7Constants.MIN_VETERAN_LAYER_TPH
									&& candidate.getTreesPerHectare() < Vdyp7Constants.MAX_VETERAN_LAYER_TPH_EXCLUSIVE
									&& dbh >= Vdyp7Constants.MIN_VETERAN_LAYER_DBH
									&& candidate.doesHeightExceed(Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT)) {

								selectedVeteranLayer = candidate;
								break;
							} else {
								logger.debug(
										"{}: layer '{}' did not pass the tests to be a veteran layer", this,
										candidate.getLayerId()
								);
							}
						} else {
							logger.debug(
									"{}: does not have a leading species and therefore cannot be a veteran layer",
									candidate.getLayerId()
							);
						}
					} else {
						logger.debug(
								"{}: layer '{}' is either the primary layer or does not have layer id \"1\"", this,
								candidate.getLayerId()
						);
					}
				}
			}
		} else {
			selectedVeteranLayer = getVeteranLayer();
			logger.debug(
					"{}: layer {} was already identified as a veteran layer: '{}'", this, getVeteranLayer().getLayerId()
			);
		}

		return selectedVeteranLayer;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Polygon that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Long.valueOf(featureId).hashCode();
	}

	@Override
	public int compareTo(Polygon that) {
		return this.featureId == that.featureId ? 0 : this.featureId > that.featureId ? 1 : -1;
	}

	// toString implementations

	@Override
	public String toString() {
		return Long.toString(featureId);
	}

	public String toDetailedString() {
		// TODO: elaborate, in the manner of V7Ext_LogLayerDescriptor
		return toString();
	}
}
