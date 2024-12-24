package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.NonVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.OtherVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZone;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModel;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationMode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingMode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.UtilizationClass;

/**
 * This class is the internal representation of a Polygon to be projected.
 */
public class Polygon implements Comparable<Polygon> {

	private static Logger logger = LoggerFactory.getLogger(Polygon.class);
	
	// BUSINESS KEY - all fields required.

	/** Feature Id */
	private long featureId;

	// Optional Members
	
	/** The polygon's "polygon number" */
	private Long polygonNumber;

	/** The current Processing State of the Polygon */
	private PolygonProcessingState currentProcessingState;

	/** The reporting levels for each of the possible SP0s when predicting yields. */
	private Map<String, UtilizationClass> reportingLevelBySp0;

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
	private CfsEcoZone cfsEcoZone;

	/** the Non-Productive Descriptor of the polygon. */
	private String nonProductiveDescriptor;

	/** The percentage of the polygon that's actually stockable. */
	private Double percentStockable;

	/** The amount of the polygon that has suffered significant kill. */
	private Double percentStockableDead;

	/** The factor to multiply predicted yields by */
	private Double yieldFactor;

	private Map<OtherVegetationType, OtherVegCoverDetails> otherVegetationTypes;
	private Map<NonVegetationType, NonVegCoverDetails> nonVegetationTypes;

	private GrowthModel growthModelToBeUsed;
	private Map<ProjectionTypeCode, GrowthModel> growthModelUsedByProjectionType;
	private Map<ProjectionTypeCode, ProcessingMode> processingModeUsedByProjectionType;
	private Map<ProjectionTypeCode, Double> percentForestedLandUsed;
	private Map<ProjectionTypeCode, Double> yieldFactorUsed;

	/** If false, projection is turned off globally for this polygon */
	private Boolean doAllowProjection;

	/** If false for a given projection type, projection is turned off for that type */
	private Map<ProjectionTypeCode, Boolean> doAllowProjectionOfType;

	/** If true, layer adjustments were supplied in the input data */
	private Boolean wereLayerAdjustmentsSupplied;

	/** The layers (to be) processed during this projection. */
	private Map<String, Layer> layers;

	/** Each entry in this map identifies the Layer for a given projection type */
	private Map<ProjectionTypeCode, Layer> layerByProjectionType;

	/** Polygon disturbance data. */
	private History history;

	/** The parameters used for the most recent projection of a layer of this polygon. */
	private ProjectionParameters projectionParameters;

	// Per-projection type information

	private Map<ProjectionTypeCode, GrowthModel> initialModelReturnCode;
	private Map<ProjectionTypeCode, Integer> initialProcessingResults;
	private Map<ProjectionTypeCode, Integer> adjustmentProcessingResults;
	private Map<ProjectionTypeCode, Integer> projectionProcessingResults;
	private Map<ProjectionTypeCode, Integer> firstYearValidYields;

	/** The messages generated during the projection of the polygon. */
	private List<PolygonMessage> messages;

	/** This polygon's reporting information, including that of all child layers */
	private PolygonReportingInfo reportingInfo;

	// MUTABLE fields - the value of these fields will change over the lifetime of the object

	/** The layer summarization mode applied to the layers within the polygon */
	private LayerSummarizationMode layerSummarizationMode;

	/**
	 * Points to the last referenced layer. This provides a shortcut for other routines to make the automatic search for
	 * the same layer unnecessary.
	 */
	private Layer lastReferencedLayer;

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

	/** Initialize the Polygon, according to <code>V7Int_ResetPolyInfo</code>. */
	private Polygon() {
		currentProcessingState = PolygonProcessingState.DEFINING_POLYGON;

		featureId = 0;
		district = "UNK";
		mapSheet = "TESTMAP";
		mapQuad = "0";
		mapSubQuad = "0";
		polygonNumber = 0L;
		inventoryStandard = InventoryStandard.getDefault();
		layerSummarizationMode = LayerSummarizationMode.getDefault();
		referenceYear = 0;
		yearOfDeath = null;
		isCoastal = false;
		forestInventoryZone = "";
		becZone = "";
		cfsEcoZone = CfsEcoZone.getDefault();
		nonProductiveDescriptor = "";
		percentStockable = null;
		percentStockableDead = null;
		yieldFactor = 1.0;

		otherVegetationTypes = Map.of(
				OtherVegetationType.Bryoid, new OtherVegCoverDetails(0, OtherVegetationType.Bryoid), //
				OtherVegetationType.Herb, new OtherVegCoverDetails(0, OtherVegetationType.Herb), //
				OtherVegetationType.Shrub, new OtherVegCoverDetails(0, OtherVegetationType.Shrub)
		);
		nonVegetationTypes = Map.of(
				NonVegetationType.BurnedArea, new NonVegCoverDetails("", 0, NonVegetationType.BurnedArea), //
				NonVegetationType.ExposedSoil, new NonVegCoverDetails("", 0, NonVegetationType.ExposedSoil), //
				NonVegetationType.Other, new NonVegCoverDetails("", 0, NonVegetationType.Other), //
				NonVegetationType.Rock, new NonVegCoverDetails("", 0, NonVegetationType.Rock), //
				NonVegetationType.Snow, new NonVegCoverDetails("", 0, NonVegetationType.Snow), //
				NonVegetationType.Water, new NonVegCoverDetails("", 0, NonVegetationType.Water)
		); //

		doAllowProjection = true;
		wereLayerAdjustmentsSupplied = false;

		growthModelToBeUsed = GrowthModel.getDefault();

		growthModelUsedByProjectionType = new HashMap<>();
		processingModeUsedByProjectionType = new HashMap<>();
		percentForestedLandUsed = new HashMap<>();
		yieldFactorUsed = new HashMap<>();
		doAllowProjectionOfType = new HashMap<>();
		initialModelReturnCode = new HashMap<>();
		initialProcessingResults = new HashMap<>();
		adjustmentProcessingResults = new HashMap<>();
		projectionProcessingResults = new HashMap<>();
		firstYearValidYields = new HashMap<>();

		for (ProjectionTypeCode t : ProjectionTypeCode.values()) {
			growthModelUsedByProjectionType.put(t, GrowthModel.UNKNOWN);
			processingModeUsedByProjectionType.put(t, ProcessingMode.getDefault());
			percentForestedLandUsed.put(t, null);
			yieldFactorUsed.put(t, null);
			doAllowProjectionOfType.put(t, true);
			initialModelReturnCode.put(t, GrowthModel.UNKNOWN);
			initialProcessingResults.put(t, -9999);
			adjustmentProcessingResults.put(t, -9999);
			projectionProcessingResults.put(t, -9999);
			firstYearValidYields.put(t, -9999);
		}

		layers = new HashMap<>();

		// Initialize the known Layer Ids
		Layer compositeLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY).build();
		layers.put(compositeLayer.getLayerId(), compositeLayer);
		Layer spanningLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING).build();
		layers.put(spanningLayer.getLayerId(), spanningLayer);

		layerByProjectionType = new HashMap<>();

		lastReferencedLayer = null;
		targetedPrimaryLayer = null;
		targetedVeteranLayer = null;
		primaryLayer = null;
		veteranLayer = null;
		residualLayer = null;
		regenerationLayer = null;
		deadLayer = null;
		rank1Layer = null;

		history = null;
		reportingLevelBySp0 = null;

		projectionParameters = new ProjectionParameters.Builder().build();

		reportingInfo = null;
	}

	public long getFeatureId() {
		return featureId;
	}

	public Long getPolygonNumber() {
		return polygonNumber;
	}

	public PolygonProcessingState getCurrentProcessingState() {
		return currentProcessingState;
	}

	public Map<String, UtilizationClass> getReportingLevelBySp0() {
		return Collections.unmodifiableMap(reportingLevelBySp0);
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

	public LayerSummarizationMode getLayerSummarizationMode() {
		return layerSummarizationMode;
	}

	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Integer getYearOfDeath() {
		return yearOfDeath;
	}

	public boolean isCoastal() {
		return isCoastal;
	}

	public String getForestInventoryZone() {
		return forestInventoryZone;
	}

	public String getBecZone() {
		return becZone;
	}

	public CfsEcoZone getCfsEcoZone() {
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

	public Map<OtherVegetationType, OtherVegCoverDetails> getOtherVegetationTypes() {
		return Collections.unmodifiableMap(otherVegetationTypes);
	}

	public Map<NonVegetationType, NonVegCoverDetails> getNonVegetationTypes() {
		return Collections.unmodifiableMap(nonVegetationTypes);
	}

	public GrowthModel getGrowthModelToBeUsed() {
		return growthModelToBeUsed;
	}

	public Map<ProjectionTypeCode, GrowthModel> getGrowthModelUsedByProjectionType() {
		return Collections.unmodifiableMap(growthModelUsedByProjectionType);
	}

	public Map<ProjectionTypeCode, ProcessingMode> getProcessingModeUsedByProjectionType() {
		return Collections.unmodifiableMap(processingModeUsedByProjectionType);
	}

	public Map<ProjectionTypeCode, Double> getPercentForestedLandUsed() {
		return Collections.unmodifiableMap(percentForestedLandUsed);
	}

	public Map<ProjectionTypeCode, Double> getYieldFactorUsed() {
		return yieldFactorUsed;
	}

	public boolean doAllowProjection() {
		return doAllowProjection;
	}

	public Map<ProjectionTypeCode, Boolean> getAllowedProjectionMap() {
		return Collections.unmodifiableMap(doAllowProjectionOfType);
	}

	public boolean wereLayerAdjustmentsSupplied() {
		return wereLayerAdjustmentsSupplied;
	}

	public Map<String, Layer> getLayers() {
		return layers;
	}

	public Map<ProjectionTypeCode, Layer> getLayerByProjectionType() {
		return layerByProjectionType;
	}

	public Layer getLastReferencedLayer() {
		return lastReferencedLayer;
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

	public ProjectionParameters getProjectionParameters() {
		return projectionParameters;
	}

	public Map<ProjectionTypeCode, GrowthModel> getInitialModelReturnCode() {
		return initialModelReturnCode;
	}

	public Map<ProjectionTypeCode, Integer> getInitialProcessingResults() {
		return initialProcessingResults;
	}

	public Map<ProjectionTypeCode, Integer> getAdjustmentProcessingResults() {
		return adjustmentProcessingResults;
	}

	public Map<ProjectionTypeCode, Integer> getProjectionProcessingResults() {
		return projectionProcessingResults;
	}

	public Map<ProjectionTypeCode, Integer> getFirstYearValidYields() {
		return firstYearValidYields;
	}

	public PolygonReportingInfo getReportingInfo() {
		return reportingInfo;
	}

	public List<PolygonMessage> getMessages() {
		return messages;
	}

	// MUTABLE data - this may vary over the lifetime of the object.

	public void setLayerSummarizationMode(LayerSummarizationMode layerSummarizationMode) {
		this.layerSummarizationMode = layerSummarizationMode;
	}

	public void setLastReferencedLayer(Layer lastReferencedLayer) {
		this.lastReferencedLayer = lastReferencedLayer;
	}

	public void setTargetedPrimaryLayer(Layer targetedPrimaryLayer) {
		this.targetedPrimaryLayer = targetedPrimaryLayer;
	}

	public void setTargetedVeteranLayer(Layer targetedVeteranLayer) {
		this.targetedVeteranLayer = targetedVeteranLayer;
	}

	public void setPrimaryLayer(Layer primaryLayer) {
		this.primaryLayer = primaryLayer;
	}

	public void setVeteranLayer(Layer veteranLayer) {
		this.veteranLayer = veteranLayer;
	}

	public void setResidualLayer(Layer residualLayer) {
		this.residualLayer = residualLayer;
	}

	public void setRegenerationLayer(Layer regenerationLayer) {
		this.regenerationLayer = regenerationLayer;
	}

	public void setDeadLayer(Layer deadLayer) {
		this.deadLayer = deadLayer;
	}

	public void setRank1Layer(Layer rank1Layer) {
		this.rank1Layer = rank1Layer;
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

		public Builder currentProcessingState(PolygonProcessingState currentProcessingState) {
			polygon.currentProcessingState = currentProcessingState;
			return this;
		}

		public Builder reportingLevelBySp0(Map<String, UtilizationClass> reportingLevelBySp0) {
			polygon.reportingLevelBySp0 = reportingLevelBySp0;
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

		public Builder layerSummarizationMode(LayerSummarizationMode layerSummarizationMode) {
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

		public Builder coastal(boolean isCoastal) {
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

		public Builder cfsEcoZone(CfsEcoZone cfsEcoZone) {
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

		public Builder otherVegetationTypes(Map<OtherVegetationType, OtherVegCoverDetails> otherVegetationMap) {
			polygon.otherVegetationTypes = otherVegetationMap;
			return this;
		}

		public Builder nonVegetationTypes(Map<NonVegetationType, NonVegCoverDetails> nonVegetationMap) {
			polygon.nonVegetationTypes = nonVegetationMap;
			return this;
		}

		public Builder growthModelToBeUsed(GrowthModel growthModelToBeUsed) {
			polygon.growthModelToBeUsed = growthModelToBeUsed;
			return this;
		}

		public Builder
				growthModelUsedByProjectionType(Map<ProjectionTypeCode, GrowthModel> growthModelUsedByProjectionType) {
			polygon.growthModelUsedByProjectionType = growthModelUsedByProjectionType;
			return this;
		}

		public Builder processingModeUsedByProjectionType(
				Map<ProjectionTypeCode, ProcessingMode> processingModeUsedByProjectionType
		) {
			polygon.processingModeUsedByProjectionType = processingModeUsedByProjectionType;
			return this;
		}

		public Builder percentForestedLandUsed(Map<ProjectionTypeCode, Double> percentForestedLandUsed) {
			polygon.percentForestedLandUsed = percentForestedLandUsed;
			return this;
		}

		public Builder yieldFactorUsed(Map<ProjectionTypeCode, Double> yieldFactorUsed) {
			polygon.yieldFactorUsed = yieldFactorUsed;
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

		public Builder lastReferencedLayer(Layer lastReferencedLayer) {
			polygon.lastReferencedLayer = lastReferencedLayer;
			return this;
		}

		public Builder targetedPrimaryLayer(Layer targetedPrimaryLayer) {
			polygon.targetedPrimaryLayer = targetedPrimaryLayer;
			return this;
		}

		public Builder targetedVeteranLayer(Layer targetedVeteranLayer) {
			polygon.targetedVeteranLayer = targetedVeteranLayer;
			return this;
		}

		public Builder primaryLayer(Layer primaryLayer) {
			polygon.primaryLayer = primaryLayer;
			return this;
		}

		public Builder veteranLayer(Layer veteranLayer) {
			polygon.veteranLayer = veteranLayer;
			return this;
		}

		public Builder residualLayer(Layer residualLayer) {
			polygon.residualLayer = residualLayer;
			return this;
		}

		public Builder regenerationLayer(Layer regenerationLayer) {
			polygon.regenerationLayer = regenerationLayer;
			return this;
		}

		public Builder deadLayer(Layer deadLayer) {
			polygon.deadLayer = deadLayer;
			return this;
		}

		public Builder rank1Layer(Layer rank1Layer) {
			polygon.rank1Layer = rank1Layer;
			return this;
		}

		public Builder history(History history) {
			polygon.history = history;
			return this;
		}

		public Builder projectionParameters(ProjectionParameters projectionParameters) {
			polygon.projectionParameters = projectionParameters;
			return this;
		}

		public Builder initialModelReturnCode(Map<ProjectionTypeCode, GrowthModel> initialModelReturnCode) {
			polygon.initialModelReturnCode = initialModelReturnCode;
			return this;
		}

		public Builder initialProcessingResults(Map<ProjectionTypeCode, Integer> initialProcessingResults) {
			polygon.initialProcessingResults = initialProcessingResults;
			return this;
		}

		public Builder adjustmentProcessingResults(Map<ProjectionTypeCode, Integer> adjustmentProcessingResults) {
			polygon.adjustmentProcessingResults = adjustmentProcessingResults;
			return this;
		}

		public Builder projectionProcessingResults(Map<ProjectionTypeCode, Integer> projectionProcessingResults) {
			polygon.projectionProcessingResults = projectionProcessingResults;
			return this;
		}

		public Builder firstYearValidYields(Map<ProjectionTypeCode, Integer> firstYearValidYields) {
			polygon.firstYearValidYields = firstYearValidYields;
			return this;
		}

		public Builder messages(List<PolygonMessage> messages) {
			polygon.messages = messages;
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

	public Layer findSpecificLayer(String layerId) throws PolygonValidationException {

		Layer selectedLayer = null;

		if (layerId == null) {
			selectedLayer = getLastReferencedLayer();
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

		setLastReferencedLayer(selectedLayer);

		return selectedLayer;
	}

	private Layer findPrimaryLayerByProjectionType(ProjectionTypeCode projectionType)
			throws PolygonValidationException {

		Layer primaryLayer = null;

		switch (projectionType) {
		case DEAD:
			primaryLayer = determineDeadLayer();
			break;
		case PRIMARY:
			primaryLayer = determinePrimaryLayer();
			break;
		case REGENERATION:
			primaryLayer = getRegenerationLayer();
			break;
		case RESIDUAL:
			primaryLayer = getResidualLayer();
			break;
		case VETERAN:
			primaryLayer = determineVeteranLayer();
			break;
		case UNKNOWN:
			for (var pType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES) {
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
	private Layer determinePrimaryLayer() throws PolygonValidationException {

		Layer primaryLayer;

		if (getPrimaryLayer() != null) {
			primaryLayer = getPrimaryLayer();
		} else {
			mergeLayers(ProjectionTypeCode.UNKNOWN);
			primaryLayer = getPrimaryLayer();
		}

		setLastReferencedLayer(primaryLayer);

		return primaryLayer;
	}

	private Layer determineVeteranLayer() throws PolygonValidationException {

		Layer veteranLayer;

		if (getVeteranLayer() != null) {
			veteranLayer = getVeteranLayer();
		} else {
			mergeLayers(ProjectionTypeCode.UNKNOWN);
			veteranLayer = getVeteranLayer();
		}

		setLastReferencedLayer(veteranLayer);

		return veteranLayer;
	}

	private Layer determineDeadLayer() {

		Layer deadLayer = null;

		if (getDeadLayer() != null) {
			deadLayer = getDeadLayer();
		}

		setLastReferencedLayer(deadLayer);

		return deadLayer;
	}

	private void mergeLayers(ProjectionTypeCode projectionType) throws PolygonValidationException {

		if (projectionType == ProjectionTypeCode.PRIMARY || projectionType == ProjectionTypeCode.UNKNOWN) {

			InventoryStandard inventoryStandard = getInventoryStandard();
			switch (inventoryStandard) {
			case FIP:
			case Silviculture: {
				setLayerSummarizationMode(determineFipMergeModel());

				logger.debug(
						"Selected FIP Inventory Layer Summarization Mode: {}", getLayerSummarizationMode()
				);

				switch (getLayerSummarizationMode()) {
				case RankOneOnly:
					// There are two layer summarization modes. RankOneOnly is outdated and kept for compatibility.
					throw new UnsupportedOperationException(
							"LayerSummarizationMode \"RankOneOnly\" is not yet supported"
					);
				case TwoLayer:
					prepareFip2Layer();
					break;
				case Unknown:
				default:
					throw new PolygonValidationException(
							new ValidationMessage(
									ValidationMessageKind.UNRECOGNIZED_LAYER_SUMMARIZATION_MODE,
									getLayerSummarizationMode()
							)
					);
				}
				break;
			}
			case VRI:
				setLayerSummarizationMode(determineVriMergeModel());

				logger.debug(
						"Selected VRI Inventory Layer Summarization Mode: {}", getLayerSummarizationMode()
				);

				switch (getLayerSummarizationMode()) {
				case RankOneOnly:
					// There are two layer summarization modes. RankOneOnly is outdated and kept for compatibility.
					throw new UnsupportedOperationException(
							"LayerSummarizationMode \"RankOneOnly\" is not yet supported"
					);
				case TwoLayer:
					prepareVri2Layer();
					break;
				case Unknown:
				default:
					throw new PolygonValidationException(
							new ValidationMessage(
									ValidationMessageKind.UNRECOGNIZED_LAYER_SUMMARIZATION_MODE,
									getLayerSummarizationMode()
							)
					);
				}
				break;
			case Unknown:
			default:
				throw new PolygonValidationException(
						new ValidationMessage(
								ValidationMessageKind.UNRECOGNIZED_INVENTORY_STANDARD_CODE, inventoryStandard
						)
				);
			}
		}

		logger.debug("Selected primary layer: {}", getPrimaryLayer().toDetailedString());
		logger.debug("Selected veteran layer: {}", getVeteranLayer().toDetailedString());
	}

	private LayerSummarizationMode determineFipMergeModel() {
		// RankOneOnly is legacy and supported only for backwards compatibility. TwoLayer is
		// the mode to be used going forward.
		return LayerSummarizationMode.TwoLayer;
	}

	private LayerSummarizationMode determineVriMergeModel() {
		// RankOneOnly is legacy and supported only for backwards compatibility. TwoLayer is
		// the mode to be used going forward.
		return LayerSummarizationMode.TwoLayer;
	}

	/**
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
	 * 
	 * @param polygon the polygon descriptor to be preprocessed.
	 */
	private void prepareFip2Layer() {
		setPrimaryLayer(selectPrimaryLayer());
		setVeteranLayer(selectFipVeteranLayer());
	}

	/**
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
	 * 
	 * @param polygon the polygon to be preprocessed.
	 */
	private void prepareVri2Layer() {
		setPrimaryLayer(selectPrimaryLayer());
		setVeteranLayer(selectVriVeteranLayer());
	}

	private Layer selectPrimaryLayer() {

		Layer primaryLayer = null;

		logger.debug("Determining VDYP primary layer");

		Layer deadLayer = determineDeadLayer();
		logger.debug("Polygon dead layer is {}", deadLayer == null ? "not set" : deadLayer.getLayerId());

		if (getTargetedPrimaryLayer() != null) {

			primaryLayer = getTargetedPrimaryLayer();
			logger.debug("Layer {} is identified/targeted as the primary layer", primaryLayer);
		} else if (getRank1Layer() != null && getRank1Layer() != deadLayer) {

			primaryLayer = getRank1Layer();
			logger.debug("Layer {} is primary layer due to being the rank 1 layer", primaryLayer);
		} else if (getLayers().size() > 0) {

			// Find the first live-stem Layer that satisfies the criteria. If none do, choose any
			// live-stem Layer.
			//
			// NOTE: the original VDYP7 code finds the first live stem layer, then the second, and
			// if the first doesn't qualify as a primary layer it simply chooses the second (assuming
			// one exists) without regard to its qualifications. This seems odd and incomplete, so
			// here we find the first live-stem layer that is qualified to be a primary and - if none
			// exist - return the first found.
			Layer fallbackChoice = null;

			for (Layer candidate : getLayers().values().stream().filter(l -> !l.isDeadLayer()).toList()) {
				if (fallbackChoice == null) {
					fallbackChoice = candidate;
				}

				Stand leadSp0 = candidate.determineLeadingSp0(0);
				if (leadSp0 != null) {
					if (candidate.getCrownClosure() <= 10.0
							&& leadSp0.getSpeciesGroup().getTotalAge() >= leadSp0.determineMaturityAge()) {
						// Layer 1 is really a veteran layer
						continue;
					} else {
						primaryLayer = candidate;
						break;
					}
				}
			}

			if (primaryLayer == null) {
				primaryLayer = fallbackChoice;
			}

			if (primaryLayer != null) {
				var message = new PolygonMessage.Builder().setLayer(primaryLayer).setErrorCode(ReturnCode.SUCCESS)
						.setMessage(
								new ValidationMessage(
										ValidationMessageKind.NO_PRIMARY_LAYER_SUPPLIED, primaryLayer.getLayerId()
								)
						).build();
				getMessages().add(message);
			}
		}

		return primaryLayer;
	}

	private Layer selectFipVeteranLayer() {

		Layer veteranLayer = getVeteranLayer();

		if (veteranLayer == null && getPrimaryLayer() != null) {

			if (getTargetedVeteranLayer() != null) {

				veteranLayer = getTargetedVeteranLayer();
				logger.debug("Layer {} identified as targeted veteran layer.", veteranLayer.getLayerId());
			} else {

				logger.debug("No Targeted VDYP7 Veteran Layer identified.");
				logger.debug("Scanning for candidate layer that matches the following criteria:");
				logger.debug("  1. Non Primary Layer");
				logger.debug("  2. Must be Layer '1'");
				logger.debug("  3. Must not be Rank '1'");
				logger.debug("  4. 0.0% <= CC <= 5.0%");

				for (Layer candidate : getLayers().values()) {

					logger.debug("Current Layer Stats:");
					logger.debug("  Layer ID: '{}'", candidate.getLayerId());
					logger.debug("  Rank Code: '{}'", candidate.getRankCode());
					logger.debug("  Primary Layer? {}", candidate == getPrimaryLayer() ? "Yes" : "No");
					logger.debug("  Crown Closure: {}", Math.round(candidate.getCrownClosure() * 100.0) / 100.0);

					if (candidate != getPrimaryLayer() && "1".equals(candidate.getLayerId())
							&& candidate.getCrownClosure() >= 0.0 && candidate.getCrownClosure() <= 5.0) {

						String rankCode = candidate.getRankCode();
						if (rankCode == null && candidate.doesHeightExceed(Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT)
								&& candidate.getSp0sAsSupplied().size() > 0) {
							veteranLayer = candidate;
							break;
						}
					}
				}

				if (veteranLayer != null) {
					veteranLayer.setDoIncludeWithProjection(true);
					veteranLayer.setVDYP7LayerCode(ProjectionTypeCode.VETERAN);

					logger.debug(
							"Layer {} passes all criteria and will be processed as a veteran layer",
							veteranLayer.getLayerId()
					);
				} else {
					logger.warn(
							"No layer with id \"1\" that is not the primary layer, has 0 <= CC <= 5.0, no rank code"
									+ " and height at least {}m was found; hence, there is no identified veteran layer",
							Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT
					);
				}
			}
		}

		return veteranLayer;
	}

	private Layer selectVriVeteranLayer() {

		Layer veteranLayer = null;

		if (getPrimaryLayer() == null) {
			logger.debug("Stand contains no primary layer, therefore stand can not contain a veteran layer.");
		} else if (getVeteranLayer() == null) {

			if (getTargetedVeteranLayer() != null) {
				veteranLayer = getVeteranLayer();
				logger.debug(
						"Targeted veteran layer '{}' identified as veteran layer",
						getVeteranLayer().getLayerId()
				);
			} else {
				for (Layer candidate : getLayers().values()) {

					if (candidate != getPrimaryLayer() && "1".equals(candidate.getRankCode())) {

						Species leadSp64 = candidate.determineLeadingSp64(0);
						if (leadSp64 != null) {
							logger.debug(
									"Testing Layer '{}' to see if it meets the veteran Layer thresholds...",
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
									&& candidate.getTreesPerHectare() <= Vdyp7Constants.MIN_VETERAN_LAYER_TPH
									&& candidate.getTreesPerHectare() < Vdyp7Constants.MAX_VETERAN_LAYER_TPH_EXCLUSIVE
									&& dbh >= Vdyp7Constants.MIN_VETERAN_LAYER_DBH
									&& candidate.doesHeightExceed(Vdyp7Constants.MIN_VETERAN_LAYER_HEIGHT)) {

								veteranLayer = candidate;
								break;
							} else {
								logger.debug(
										"Layer '{}' did not pass the tests to be a veteran layer.",
										candidate.getLayerId()
								);
							}
						} else {
							logger.debug("Layer '{}' does not have a leading species...", candidate.getLayerId());
						}
					} else {
						logger.debug(
								"Layer '{}' is either the primary layer or does not have layer id \"1\".",
								candidate.getLayerId()
						);
					}
				}
			}
		} else {
			veteranLayer = getVeteranLayer();
			logger.debug(
					"Layer {} was already identified as a veteran layer: '{}'", getVeteranLayer().getLayerId()
			);
		}

		return veteranLayer;
	}
	
	public void disableProjectionsOfType(ProjectionTypeCode layerProjectionType) {
		doAllowProjectionOfType.put(layerProjectionType, false);
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
