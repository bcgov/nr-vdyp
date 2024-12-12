package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZone;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModel;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationMode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingMode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.UtilizationClass;

/**
 * This class is the internal representation of a Polygon to be projected.
 */
public class Polygon {

	/** Unique identifier for the Polygon */
	private UUID polygonId;

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

	/** The polygon's "polygon number" */
	private int polygonNumber;

	/** The inventory standard at which the polygon data was recorded. */
	private InventoryStandard inventoryStandard;

	/** The layer summarization mode applied to the layers within the polygon */
	private LayerSummarizationMode layerSummarizationMode;

	/** The reference year of the polygon */
	private int referenceYear;

	/** The year in which the stand suffered a significant kill */
	private int yearOfDeath;

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

	private Map<OtherVegetationType, Integer> otherVegetationTypes;
	private Map<NonVegetationType, Double> nonVegetationTypes;

	private GrowthModel growthModelToBeUsed;
	private Map<ProjectionType, GrowthModel> growthModelUsedByProjectionType;
	private Map<ProjectionType, ProcessingMode> processingModeUsedByProjectionType;
	private Map<ProjectionType, Double> percentForestedLandUsed;
	private Map<ProjectionType, Double> yieldFactorUsed;

	/** If false, projection is turned off globally for this polygon */
	private boolean doAllowProjection;

	/** If false for a given projection type, projection is turned off for that type */
	private Map<ProjectionType, Boolean> doAllowProjectionOfType;

	/** If true, layer adjustments were supplied in the input data */
	private boolean wereLayerAdjustmentsSupplied;

	/** The layers (to be) processed during this projection. */
	private Map<UUID, Layer> layers;

	/** Each entry in this map identifies the Layer for a given projection type */
	private Map<ProjectionType, Layer> layerByProjectionType;

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

	/** Polygon disturbance data. */
	private History history;

	/** The parameters used for the most recent projection of a layer of this polygon. */
	private ProjectionParameters projectionParameters;

	// Per-projection type information

	private Map<ProjectionType, GrowthModel> initialModelReturnCode;
	private Map<ProjectionType, Integer> initialProcessingResults;
	private Map<ProjectionType, Integer> adjustmentProcessingResults;
	private Map<ProjectionType, Integer> projectionProcessingResults;
	private Map<ProjectionType, Integer> firstYearValidYields;

	/** The messages generated during the projection of the polygon. */
	private List<PolygonMessage> messages;

	/** Initialize the Polygon, according to <code>V7Int_ResetPolyInfo</code>. */
	private Polygon() {
		polygonId = UUID.randomUUID();
		
		currentProcessingState = PolygonProcessingState.DEFINING_POLYGON;
		
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
				OtherVegetationType.Bryoid, 0, //
				OtherVegetationType.Herb, 0, //
				OtherVegetationType.Shrub, 0);
		nonVegetationTypes = Map.of(
				NonVegetationType.BurnedArea, Double.valueOf(0.0), //
				NonVegetationType.ExposedSoil, Double.valueOf(0.0), //
				NonVegetationType.Other, Double.valueOf(0.0), //
				NonVegetationType.Rock, Double.valueOf(0.0), //
				NonVegetationType.Snow, Double.valueOf(0.0), //
				NonVegetationType.Water, Double.valueOf(0.0)); //
		
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
		
		for (ProjectionType t: ProjectionType.values()) {
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
	}

	public UUID getPolygonId() {
		return polygonId;
	}

	public PolygonProcessingState getCurrentProcessingState() {
		return currentProcessingState;
	}

	public Map<String, UtilizationClass> getReportingLevelBySp0() {
		return reportingLevelBySp0;
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

	public Long getPolygonNumber() {
		return polygonNumber;
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

	public Map<OtherVegetationType, Integer> getOtherVegetationTypes() {
		return otherVegetationTypes;
	}

	public Map<NonVegetationType, Double> getNonVegetationTypes() {
		return nonVegetationTypes;
	}

	public GrowthModel getGrowthModelToBeUsed() {
		return growthModelToBeUsed;
	}

	public Map<ProjectionType, GrowthModel> getGrowthModelUsedByProjectionType() {
		return growthModelUsedByProjectionType;
	}

	public Map<ProjectionType, ProcessingMode> getProcessingModeUsedByProjectionType() {
		return processingModeUsedByProjectionType;
	}

	public Map<ProjectionType, Double> getPercentForestedLandUsed() {
		return percentForestedLandUsed;
	}

	public Map<ProjectionType, Double> getYieldFactorUsed() {
		return yieldFactorUsed;
	}

	public boolean isDoAllowProjection() {
		return doAllowProjection;
	}

	public Map<ProjectionType, Boolean> getDoAllowProjectionOfType() {
		return doAllowProjectionOfType;
	}

	public boolean isWereLayerAdjustmentsSupplied() {
		return wereLayerAdjustmentsSupplied;
	}

	public Map<UUID, Layer> getLayers() {
		return layers;
	}

	public Map<ProjectionType, Layer> getLayerByProjectionType() {
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

	public Map<ProjectionType, GrowthModel> getInitialModelReturnCode() {
		return initialModelReturnCode;
	}

	public Map<ProjectionType, Integer> getInitialProcessingResults() {
		return initialProcessingResults;
	}

	public Map<ProjectionType, Integer> getAdjustmentProcessingResults() {
		return adjustmentProcessingResults;
	}

	public Map<ProjectionType, Integer> getProjectionProcessingResults() {
		return projectionProcessingResults;
	}

	public Map<ProjectionType, Integer> getFirstYearValidYields() {
		return firstYearValidYields;
	}

	public List<PolygonMessage> getMessages() {
		return messages;
	}

	void setPolygonId(UUID polygonId) {
		this.polygonId = polygonId;
	}

	void setCurrentProcessingState(PolygonProcessingState currentProcessingState) {
		this.currentProcessingState = currentProcessingState;
	}

	void setReportingLevelBySp0(Map<String, UtilizationClass> reportingLevelBySp0) {
		this.reportingLevelBySp0 = reportingLevelBySp0;
	}

	void setDistrict(String district) {
		this.district = district;
	}

	void setMapSheet(String mapSheet) {
		this.mapSheet = mapSheet;
	}

	void setMapQuad(String mapQuad) {
		this.mapQuad = mapQuad;
	}

	void setMapSubQuad(String mapSubQuad) {
		this.mapSubQuad = mapSubQuad;
	}

	void setPolygonNumber(Long polygonNumber) {
		this.polygonNumber = polygonNumber;
	}

	void setInventoryStandard(InventoryStandard inventoryStandard) {
		this.inventoryStandard = inventoryStandard;
	}

	void setLayerSummarizationMode(LayerSummarizationMode layerSummarizationMode) {
		this.layerSummarizationMode = layerSummarizationMode;
	}

	void setReferenceYear(Integer referenceYear) {
		this.referenceYear = referenceYear;
	}

	void setYearOfDeath(Integer yearOfDeath) {
		this.yearOfDeath = yearOfDeath;
	}

	void setIsCoastal(boolean isCoastal) {
		this.isCoastal = isCoastal;
	}

	void setForestInventoryZone(String forestInventoryZone) {
		this.forestInventoryZone = forestInventoryZone;
	}

	void setBecZone(String becZone) {
		this.becZone = becZone;
	}

	void setCfsEcoZone(CfsEcoZone cfsEcoZone) {
		this.cfsEcoZone = cfsEcoZone;
	}

	void setNonProductiveDescriptor(String nonProductiveDescriptor) {
		this.nonProductiveDescriptor = nonProductiveDescriptor;
	}

	void setPercentStockable(Double percentStockable) {
		this.percentStockable = percentStockable;
	}

	void setPercentStockableDead(Double percentStockableDead) {
		this.percentStockableDead = percentStockableDead;
	}

	void setYieldFactor(Double yieldFactor) {
		this.yieldFactor = yieldFactor;
	}

	void setOtherVegetationTypes(Map<OtherVegetationType, Integer> otherVegetationTypes) {
		this.otherVegetationTypes = otherVegetationTypes;
	}

	void setNonVegetationTypes(Map<NonVegetationType, Double> nonVegetationTypes) {
		this.nonVegetationTypes = nonVegetationTypes;
	}

	void setGrowthModelToBeUsed(GrowthModel growthModelToBeUsed) {
		this.growthModelToBeUsed = growthModelToBeUsed;
	}

	void setGrowthModelUsedByProjectionType(Map<ProjectionType, GrowthModel> growthModelUsedByProjectionType) {
		this.growthModelUsedByProjectionType = growthModelUsedByProjectionType;
	}

	void setProcessingModeUsedByProjectionType(Map<ProjectionType, ProcessingMode> processingModeUsedByProjectionType) {
		this.processingModeUsedByProjectionType = processingModeUsedByProjectionType;
	}

	void setPercentForestedLandUsed(Map<ProjectionType, Double> percentForestedLandUsed) {
		this.percentForestedLandUsed = percentForestedLandUsed;
	}

	void setYieldFactorUsed(Map<ProjectionType, Double> yieldFactorUsed) {
		this.yieldFactorUsed = yieldFactorUsed;
	}

	void setDoAllowProjection(boolean doAllowProjection) {
		this.doAllowProjection = doAllowProjection;
	}

	void setDoAllowProjectionOfType(Map<ProjectionType, Boolean> doAllowProjectionOfType) {
		this.doAllowProjectionOfType = doAllowProjectionOfType;
	}

	void setWereLayerAdjustmentsSupplied(boolean wereLayerAdjustmentsSupplied) {
		this.wereLayerAdjustmentsSupplied = wereLayerAdjustmentsSupplied;
	}

	void setLayers(Map<UUID, Layer> layers) {
		this.layers = layers;
	}

	void setLayerByProjectionType(Map<ProjectionType, Layer> layerByProjectionType) {
		this.layerByProjectionType = layerByProjectionType;
	}

	void setLastReferencedLayer(Layer lastReferencedLayer) {
		this.lastReferencedLayer = lastReferencedLayer;
	}

	void setTargetedPrimaryLayer(Layer targetedPrimaryLayer) {
		this.targetedPrimaryLayer = targetedPrimaryLayer;
	}

	void setTargetedVeteranLayer(Layer targetedVeteranLayer) {
		this.targetedVeteranLayer = targetedVeteranLayer;
	}

	void setPrimaryLayer(Layer primaryLayer) {
		this.primaryLayer = primaryLayer;
	}

	void setVeteranLayer(Layer veteranLayer) {
		this.veteranLayer = veteranLayer;
	}

	void setResidualLayer(Layer residualLayer) {
		this.residualLayer = residualLayer;
	}

	void setRegenerationLayer(Layer regenerationLayer) {
		this.regenerationLayer = regenerationLayer;
	}

	void setDeadLayer(Layer deadLayer) {
		this.deadLayer = deadLayer;
	}

	void setRank1Layer(Layer rank1Layer) {
		this.rank1Layer = rank1Layer;
	}

	void setHistory(History history) {
		this.history = history;
	}

	void setProjectionParameters(ProjectionParameters projectionParameters) {
		this.projectionParameters = projectionParameters;
	}

	void setInitialModelReturnCode(Map<ProjectionType, GrowthModel> initialModelReturnCode) {
		this.initialModelReturnCode = initialModelReturnCode;
	}

	void setInitialProcessingResults(Map<ProjectionType, Integer> initialProcessingResults) {
		this.initialProcessingResults = initialProcessingResults;
	}

	void setAdjustmentProcessingResults(Map<ProjectionType, Integer> adjustmentProcessingResults) {
		this.adjustmentProcessingResults = adjustmentProcessingResults;
	}

	void setProjectionProcessingResults(Map<ProjectionType, Integer> projectionProcessingResults) {
		this.projectionProcessingResults = projectionProcessingResults;
	}

	void setFirstYearValidYields(Map<ProjectionType, Integer> firstYearValidYields) {
		this.firstYearValidYields = firstYearValidYields;
	}

	void setMessages(List<PolygonMessage> messages) {
		this.messages = messages;
	}

	public static class Builder {
		private Polygon polygon = new Polygon();

		public Polygon.Builder polygonId(UUID polygonId) {
			polygon.setPolygonId(polygonId);
			return this;
		}

		public Polygon.Builder currentProcessingState(PolygonProcessingState currentProcessingState) {
			polygon.setCurrentProcessingState(currentProcessingState);
			return this;
		}

		public Polygon.Builder reportingLevelBySp0(Map<String, UtilizationClass> reportingLevelBySp0) {
			polygon.setReportingLevelBySp0(reportingLevelBySp0);
			return this;
		}

		public Polygon.Builder district(String district) {
			polygon.setDistrict(district);
			return this;
		}

		public Polygon.Builder mapSheet(String mapSheet) {
			polygon.setMapSheet(mapSheet);
			return this;
		}

		public Polygon.Builder mapQuad(String mapQuad) {
			polygon.setMapQuad(mapQuad);
			return this;
		}

		public Polygon.Builder mapSubQuad(String mapSubQuad) {
			polygon.setMapSubQuad(mapSubQuad);
			return this;
		}

		public Polygon.Builder polygonNumber(Long polygonNumber) {
			polygon.setPolygonNumber(polygonNumber);
			return this;
		}

		public Polygon.Builder inventoryStandard(InventoryStandard inventoryStandard) {
			polygon.setInventoryStandard(inventoryStandard);
			return this;
		}

		public Polygon.Builder layerSummarizationMode(LayerSummarizationMode layerSummarizationMode) {
			polygon.setLayerSummarizationMode(layerSummarizationMode);
			return this;
		}

		public Polygon.Builder referenceYear(Integer referenceYear) {
			polygon.setReferenceYear(referenceYear);
			return this;
		}

		public Polygon.Builder yearOfDeath(Integer yearOfDeath) {
			polygon.setYearOfDeath(yearOfDeath);
			return this;
		}

		public Polygon.Builder coastal(boolean isCoastal) {
			polygon.setIsCoastal(isCoastal);
			return this;
		}

		public Polygon.Builder forestInventoryZone(String forestInventoryZone) {
			polygon.setForestInventoryZone(forestInventoryZone);
			return this;
		}

		public Polygon.Builder becZone(String becZone) {
			polygon.setBecZone(becZone);
			return this;
		}

		public Polygon.Builder cfsEcoZone(CfsEcoZone cfsEcoZone) {
			polygon.setCfsEcoZone(cfsEcoZone);
			return this;
		}

		public Polygon.Builder nonProductiveDescriptor(String nonProductiveDescriptor) {
			polygon.setNonProductiveDescriptor(nonProductiveDescriptor);
			return this;
		}

		public Polygon.Builder percentStockable(Double percentStockable) {
			polygon.setPercentStockable(percentStockable);
			return this;
		}

		public Polygon.Builder percentStockableDead(Double percentStockableDead) {
			polygon.setPercentStockableDead(percentStockableDead);
			return this;
		}

		public Polygon.Builder yieldFactor(Double yieldFactor) {
			polygon.setYieldFactor(yieldFactor);
			return this;
		}

		public Polygon.Builder otherVegetationTypes(Map<OtherVegetationType, Integer> otherVegetationTypes) {
			polygon.setOtherVegetationTypes(otherVegetationTypes);
			return this;
		}

		public Polygon.Builder nonVegetationTypes(Map<NonVegetationType, Double> nonVegetationTypes) {
			polygon.setNonVegetationTypes(nonVegetationTypes);
			return this;
		}

		public Polygon.Builder growthModelToBeUsed(GrowthModel growthModelToBeUsed) {
			polygon.setGrowthModelToBeUsed(growthModelToBeUsed);
			return this;
		}

		public Polygon.Builder
				growthModelUsedByProjectionType(Map<ProjectionType, GrowthModel> growthModelUsedByProjectionType) {
			polygon.setGrowthModelUsedByProjectionType(growthModelUsedByProjectionType);
			return this;
		}

		public Polygon.Builder processingModeUsedByProjectionType(
				Map<ProjectionType, ProcessingMode> processingModeUsedByProjectionType
		) {
			polygon.setProcessingModeUsedByProjectionType(processingModeUsedByProjectionType);
			return this;
		}

		public Polygon.Builder percentForestedLandUsed(Map<ProjectionType, Double> percentForestedLandUsed) {
			polygon.setPercentForestedLandUsed(percentForestedLandUsed);
			return this;
		}

		public Polygon.Builder yieldFactorUsed(Map<ProjectionType, Double> yieldFactorUsed) {
			polygon.setYieldFactorUsed(yieldFactorUsed);
			return this;
		}

		public Polygon.Builder doAllowProjection(boolean doAllowProjection) {
			polygon.setDoAllowProjection(doAllowProjection);
			return this;
		}

		public Polygon.Builder doAllowProjectionOfType(Map<ProjectionType, Boolean> doAllowProjectionOfType) {
			polygon.setDoAllowProjectionOfType(doAllowProjectionOfType);
			return this;
		}

		public Polygon.Builder wereLayerAdjustmentsSupplied(boolean wereLayerAdjustmentsSupplied) {
			polygon.setWereLayerAdjustmentsSupplied(wereLayerAdjustmentsSupplied);
			return this;
		}

		public Polygon.Builder layers(Map<UUID, Layer> layers) {
			polygon.setLayers(layers);
			return this;
		}

		public Polygon.Builder layerByProjectionType(Map<ProjectionType, Layer> layerByProjectionType) {
			polygon.setLayerByProjectionType(layerByProjectionType);
			return this;
		}

		public Polygon.Builder lastReferencedLayer(Layer lastReferencedLayer) {
			polygon.setLastReferencedLayer(lastReferencedLayer);
			return this;
		}

		public Polygon.Builder targetedPrimaryLayer(Layer targetedPrimaryLayer) {
			polygon.setTargetedPrimaryLayer(targetedPrimaryLayer);
			return this;
		}

		public Polygon.Builder targetedVeteranLayer(Layer targetedVeteranLayer) {
			polygon.setTargetedVeteranLayer(targetedVeteranLayer);
			return this;
		}

		public Polygon.Builder primaryLayer(Layer primaryLayer) {
			polygon.setPrimaryLayer(primaryLayer);
			return this;
		}

		public Polygon.Builder veteranLayer(Layer veteranLayer) {
			polygon.setVeteranLayer(veteranLayer);
			return this;
		}

		public Polygon.Builder residualLayer(Layer residualLayer) {
			polygon.setResidualLayer(residualLayer);
			return this;
		}

		public Polygon.Builder regenerationLayer(Layer regenerationLayer) {
			polygon.setRegenerationLayer(regenerationLayer);
			return this;
		}

		public Polygon.Builder deadLayer(Layer deadLayer) {
			polygon.setDeadLayer(deadLayer);
			return this;
		}

		public Polygon.Builder rank1Layer(Layer rank1Layer) {
			polygon.setRank1Layer(rank1Layer);
			return this;
		}

		public Polygon.Builder history(History history) {
			polygon.setHistory(history);
			return this;
		}

		public Polygon.Builder projectionParameters(ProjectionParameters projectionParameters) {
			polygon.setProjectionParameters(projectionParameters);
			return this;
		}

		public Polygon.Builder initialModelReturnCode(Map<ProjectionType, GrowthModel> initialModelReturnCode) {
			polygon.setInitialModelReturnCode(initialModelReturnCode);
			return this;
		}

		public Polygon.Builder initialProcessingResults(Map<ProjectionType, Integer> initialProcessingResults) {
			polygon.setInitialProcessingResults(initialProcessingResults);
			return this;
		}

		public Polygon.Builder
				adjustmentProcessingResults(Map<ProjectionType, Integer> adjustmentProcessingResults) {
			polygon.setAdjustmentProcessingResults(adjustmentProcessingResults);
			return this;
		}

		public Polygon.Builder
				projectionProcessingResults(Map<ProjectionType, Integer> projectionProcessingResults) {
			polygon.setProjectionProcessingResults(projectionProcessingResults);
			return this;
		}

		public Polygon.Builder firstYearValidYields(Map<ProjectionType, Integer> firstYearValidYields) {
			polygon.setFirstYearValidYields(firstYearValidYields);
			return this;
		}

		public Polygon.Builder messages(List<PolygonMessage> messages) {
			polygon.setMessages(messages);
			return this;
		}

		public Polygon build() {
			return polygon;
		}
	}
}
