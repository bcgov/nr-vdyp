package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.io.write.FipStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.io.write.VriStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ComponentReturnCodes;
import ca.bc.gov.nrs.vdyp.backend.projection.IComponentRunner;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.NonVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean.OtherVegCoverDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZoneCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingStateCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.UtilizationClassCode;
import ca.bc.gov.nrs.vdyp.backend.utils.NullMath;
import ca.bc.gov.nrs.vdyp.backend.utils.ProjectionUtils;

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
	private PolygonProcessingStateCode currentProcessingState;

	/** The reporting levels for each of the possible SP0s when predicting yields. */
	private Map<String, UtilizationClassCode> reportingLevelBySp0;

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

	/** The messages generated during the projection of the polygon. */
	private List<PolygonMessage> messages;

	/** This polygon's reporting information, including that of all child layers */
	private PolygonReportingInfo reportingInfo;

	private Map<OtherVegetationTypeCode, OtherVegCoverDetails> otherVegetationTypes;
	private Map<NonVegetationTypeCode, NonVegCoverDetails> nonVegetationTypes;

	// MUTABLE fields - the value of these fields will change over the lifetime of the object

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

	/** Initialize the Polygon, according to <code>V7Int_ResetPolyInfo</code>. */
	private Polygon() {
		currentProcessingState = PolygonProcessingStateCode.DEFINING_POLYGON;

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
		Layer compositeLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY).build();
		layers.put(compositeLayer.getLayerId(), compositeLayer);
		Layer spanningLayer = new Layer.Builder().layerId(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING).build();
		layers.put(spanningLayer.getLayerId(), spanningLayer);

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

	public PolygonProcessingStateCode getCurrentProcessingState() {
		return currentProcessingState;
	}

	public Map<String, UtilizationClassCode> getReportingLevelBySp0() {
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

	public LayerSummarizationModeCode getLayerSummarizationMode() {
		return layerSummarizationMode;
	}

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
	Integer getMeasurementYear() {
		Integer result = null;

		if (getReferenceYear() != null) {
			result = getReferenceYear();
		}

		if (getYearOfDeath() != null && (result == null || result < getYearOfDeath())) {
			result = getYearOfDeath();
		}

		return result;
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

	public PolygonReportingInfo getReportingInfo() {
		return reportingInfo;
	}

	public List<PolygonMessage> getMessages() {
		return messages;
	}

	// MUTABLE data - this may vary over the lifetime of the object.

	public void setLayerSummarizationMode(LayerSummarizationModeCode layerSummarizationMode) {
		this.layerSummarizationMode = layerSummarizationMode;
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

	void setLayerByProjectionType(ProjectionTypeCode layerProjectionType, Layer layer) {
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

		public Builder reportingLevelBySp0(Map<String, UtilizationClassCode> reportingLevelBySp0) {
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

	private void performInitialProcessing(IComponentRunner componentRunner, PolygonProjectionState state)
			throws PolygonExecutionException {

		if (currentProcessingState != PolygonProcessingStateCode.POLYGON_DEFINED) {
			throw new IllegalStateException(
					MessageFormat.format(
							"performInitialProcessing expects Polygon {0} to be in state"
									+ " PolygonProcessingState.POLYGON_DEFINED but it is in state {1}",
							this, currentProcessingState
					)
			);
		}

		try {
			this.determineInitialProcessingModel(state);
		} catch (PolygonValidationException e) {
			// Convert any validation exceptions into execution exceptions at this point.
			throw new PolygonExecutionException(e);
		}

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (this.getLayerByProjectionType().get(projectionType) == null) {
				logger.debug(
						"No layers defined for projection type {}; skipping initial processing for this type",
						projectionType
				);
				continue;
			}

			logger.debug(
					"Polygon {}: layer exists for projection type {} (growth model {}; processing mode {}); initial processing will occur for this type"
					, this, projectionType, state.getGrowthModel(projectionType), state.getProcessingMode(projectionType)
			);

			switch (state.getGrowthModel(projectionType)) {
			case FIP: {

				createFipInputData(projectionType, state);

				componentRunner.runFipStart(this, projectionType, state);
				logger.debug(
						"Performed FIP Model; FIPSTART return code: {}", state.getProcessingResultsCode(projectionType)
				);

				if (state.getInitialModelReturnCode().get(projectionType)
						.equals(ComponentReturnCodes.FIP_RETRY_USING_VRI_START)) {
					logger.debug("Falling through to VRI Model");
					state.modifyGrowthModel(projectionType, GrowthModelCode.VRI, ProcessingModeCode.VRI_VriYoung);
				} else {
					break;
				}
			}

			case VRI: {

				createVriInputData(projectionType, state);

				componentRunner.runVriStart(this, projectionType, state);
				logger.debug(
						"Performed VRI Model; VRISTART return code: {}", state.getProcessingResultsCode(projectionType)
				);

				break;
			}

			default:
				logger.error("Unrecognized growth model {}", state.getGrowthModel(projectionType));

				addMessage(
						state.getContext(), "Attempt to process an unrecognized growth model {0}",
						state.getGrowthModel(projectionType)
				);
			}
		}
	}

	private void createFipInputData(ProjectionTypeCode projectionType, PolygonProjectionState state)
			throws PolygonExecutionException {
		
		Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());
		
		try {
			Path polygonFile = Path.of(executionFolder.toString(), "fip_p01.dat");
			FileOutputStream polygonOutputStream = new FileOutputStream(polygonFile.toFile());
			
			Path layersFile = Path.of(executionFolder.toString(), "fip_l01.dat");
			FileOutputStream layersOutputStream = new FileOutputStream(layersFile.toFile());
			
			Path speciesFile = Path.of(executionFolder.toString(), "fip_ls01.dat");
			FileOutputStream speciesOutputStream = new FileOutputStream(speciesFile.toFile());
			
			try (
					var outputWriter = new FipStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream
							)
					) {
				outputWriter.writePolygon(this, projectionType, state);
				outputWriter.writePolygonLayers(getLayerByProjectionType().get(projectionType));
			}
		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}
	
	private void createVriInputData(ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionTypeCode.toString());

		try {
			Path polygonFile = Path.of(executionFolder.toString(), "virnp01.dat");
			FileOutputStream polygonOutputStream = new FileOutputStream(polygonFile.toFile());

			Path layersFile = Path.of(executionFolder.toString(), " vrinl01.dat");
			FileOutputStream layersOutputStream = new FileOutputStream(layersFile.toFile());

			Path speciesFile = Path.of(executionFolder.toString(), "vrinsp01.dat");
			FileOutputStream speciesOutputStream = new FileOutputStream(speciesFile.toFile());

			Path siteIndexFile = Path.of(executionFolder.toString(), "vrinsi01.dat");
			FileOutputStream siteIndexOutputStream = new FileOutputStream(siteIndexFile.toFile());

			try (
					var outputWriter = new VriStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream, siteIndexOutputStream
					)
			) {
				outputWriter.writePolygon(this, projectionTypeCode, state);
				outputWriter.writePolygonLayers(getLayerByProjectionType().get(projectionTypeCode));
			}
		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}

	private void performAdjustProcessing(IComponentRunner componentRunner, PolygonProjectionState state) {

	}

	private void performForwardProcessing(IComponentRunner componentRunner, PolygonProjectionState state) {

	}

	private void performBackProcessing(IComponentRunner componentRunner, PolygonProjectionState state) {

	}

	/**
	 * Determine the processing model to which the stand will be initially subject.
	 * 
	 * @return as described
	 * @throws PolygonValidationException if the polygon definition contains errors
	 */
	private void determineInitialProcessingModel(PolygonProjectionState state) throws PolygonValidationException {

		GrowthModelCode growthModel = null;
		ProcessingModeCode processingMode = null;

		for (ProjectionTypeCode pt : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			// Iterate until we find a projection type with a primary layer; that will determine
			// the (initial) model for all projection types.
			
			if (growthModel == null && processingMode == null) {
				
				// Determine the primary layer of the stand.
				Layer ptPrimaryLayer = findPrimaryLayerByProjectionType(pt);
				if (ptPrimaryLayer != null) {
	
					logger.debug("Projection type {0}: primary layer determined to be: {}", pt, ptPrimaryLayer);
	
					// Check if the stand is non-productive.
					if (getNonProductiveDescriptor() != null) {
						if (ptPrimaryLayer.getSp0sAsSupplied().size() > 0) {
							logger.debug(
									"Stand labelled with Non-Productive code {}, but also contains a stand description.",
									getNonProductiveDescriptor()
							);
						} else {
							disableProjectionsOfType(pt);
	
							addMessage(
									state.getContext(), "Layer {0} is not completely, or is not consistently, defined",
									ptPrimaryLayer
							);
						}
					}
	
					Stand leadingSp0 = ptPrimaryLayer.determineLeadingSp0(0 /* leading */);
					if (leadingSp0 == null) {
						disableProjectionsOfType(pt);
						addMessage(
								state.getContext(), "Unable to locate a leading species for primary layer {}",
								ptPrimaryLayer
						);
					} else {
						logger.debug(
								"Primary layer determined to be {} (percentage {})", ptPrimaryLayer,
								leadingSp0.getSpeciesGroup().getSpeciesPercent()
						);
					}
	
					if (getInventoryStandard().equals(InventoryStandard.Silviculture)) {
						logger.debug("Implemented Classification Rule 1: SILV Standard Inventory treated as FIPSTART");
						growthModel = GrowthModelCode.FIP;
						processingMode = ProcessingModeCode.FIP_Default;
					} else if (ptPrimaryLayer.getBasalArea() > 0.0 && ptPrimaryLayer.getTreesPerHectare() > 0.0) {
						logger.debug(
								"Implemented Classification Rule 2: BA and TPH both greater than 0 ({}, {})",
								ptPrimaryLayer.getBasalArea(), ptPrimaryLayer.getTreesPerHectare()
						);
						growthModel = GrowthModelCode.VRI;
						processingMode = ProcessingModeCode.VRI_Default;
					} else if (getInventoryStandard().equals(InventoryStandard.FIP)) {
						logger.debug("Implemented Classification Rule 3: FIP Standard Inventory");
						growthModel = GrowthModelCode.FIP;
						processingMode = ProcessingModeCode.FIP_Default;
					} else {
						logger.debug("Implemented Classification Rule 4: all other cases");
						growthModel = GrowthModelCode.VRI;
						processingMode = ProcessingModeCode.VRI_Default;
					}
				}
			}
			
			if (growthModel != null && processingMode != null) {
				state.setGrowthModel(pt, growthModel, processingMode);
				logger.trace("Polygon {}: growth model {}; processing mode {}", this, growthModel, processingMode);
			} else {
				throw new PolygonValidationException(
						new ValidationMessage(ValidationMessageKind.PRIMARY_LAYER_NOT_FOUND, this)
				);
			}
		}
	}

	public Layer findSpecificLayer(String layerId) throws PolygonValidationException {

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

	private final String POLYGON_DESCRIPTOR_FORMAT = "%-7s%10d%3s%5d";

	public String buildPolygonDescriptor() {

		String mapSheet = this.mapSheet.length() > 7 ? this.mapSheet.substring(0, 7) : this.mapSheet;
		String district = this.district == null ? ""
				: this.district.length() > 3 ? this.district.substring(0, 3) : this.district;
		int measurementYear = this.getMeasurementYear();
		return String.format(POLYGON_DESCRIPTOR_FORMAT, mapSheet, polygonNumber, district, measurementYear);
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

		yearOfDeath = NullMath.max(yearOfDeath, newDeadLayer.getYearOfDeath(), (a, b) -> Math.max(a, b), -9);
		yearOfDeath = NullMath.max(yearOfDeath, getYearOfDeath(), (a, b) -> Math.max(a, b), -9);

		if (yearOfDeath == null) {
			yearOfDeath = getReferenceYear();
		}

		logger.debug("assignDeadLayer: using year-of-death {}", yearOfDeath);

		percentStockKilled = NullMath.max(percentStockableDead, percentStockKilled, (a, b) -> Math.max(a, b), -9.0);
		percentStockKilled = NullMath
				.max(newDeadLayer.getPercentStockable(), percentStockKilled, (a, b) -> Math.max(a, b), -9.0);

		logger.debug("Percent Stockable Land Killed to use: {}", percentStockKilled);

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

	public void doCompleteDefinition() throws PolygonValidationException {

		if (currentProcessingState != PolygonProcessingStateCode.DEFINING_POLYGON) {
			throw new IllegalStateException(
					"Cannot call Polygon.doCompleteDefinition unless the Polygon is in DEFINING_POLYGON state"
			);
		}

		// Assign the projection type of each of the layers. This is set at construction time to
		// UNKNOWN.

		for (Layer layer : layers.values()) {
			layer.doCompleteDefinition(this);
		}

		currentProcessingState = PolygonProcessingStateCode.POLYGON_DEFINED;
	}

	public void project(IComponentRunner componentRunner, PolygonProjectionState state)
			throws PolygonExecutionException, ProjectionInternalExecutionException {

		buildProjectionExecutionStructure(state);

		performInitialProcessing(componentRunner, state);

		// VRI ADJUST is not supported at this time, so this code doesn't need to be written:
		// defineAdjustmentSeeds(state);

		determineAgeRange(state);

		performAdjustProcessing(componentRunner, state);

		performForwardProcessing(componentRunner, state);

		performBackProcessing(componentRunner, state);

		generateYieldTables(state);

		currentProcessingState = PolygonProcessingStateCode.PROJECTED;
	}

	Layer findPrimaryLayerByProjectionType(ProjectionTypeCode projectionType) throws PolygonValidationException {

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
			primaryLayer = determineDeadLayer();
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

	private Layer determinePrimaryLayer() throws PolygonValidationException {

		Layer primaryLayer;

		if (getPrimaryLayer() != null) {
			primaryLayer = getPrimaryLayer();
		} else {
			mergeLayers(ProjectionTypeCode.UNKNOWN);
			primaryLayer = getPrimaryLayer();
		}

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

		return veteranLayer;
	}

	private Layer determineDeadLayer() {

		Layer deadLayer = null;

		if (getDeadLayer() != null) {
			deadLayer = getDeadLayer();
		}

		return deadLayer;
	}

	private void mergeLayers(ProjectionTypeCode projectionType) throws PolygonValidationException {

		if (projectionType == ProjectionTypeCode.PRIMARY || projectionType == ProjectionTypeCode.UNKNOWN) {

			InventoryStandard inventoryStandard = getInventoryStandard();
			switch (inventoryStandard) {
			case FIP:
			case Silviculture: {
				setLayerSummarizationMode(determineFipMergeModel());

				logger.debug("Selected FIP Inventory Layer Summarization Mode: {}", getLayerSummarizationMode());

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
									ValidationMessageKind.UNRECOGNIZED_LAYER_SUMMARIZATION_MODE, this,
									getLayerSummarizationMode()
							)
					);
				}
				break;
			}
			case VRI:
				setLayerSummarizationMode(determineVriMergeModel());

				logger.debug("Selected VRI Inventory Layer Summarization Mode: {}", getLayerSummarizationMode());

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
									ValidationMessageKind.UNRECOGNIZED_LAYER_SUMMARIZATION_MODE, this,
									getLayerSummarizationMode()
							)
					);
				}
				break;
			case Unknown:
			default:
				throw new PolygonValidationException(
						new ValidationMessage(
								ValidationMessageKind.UNRECOGNIZED_INVENTORY_STANDARD_CODE, this, inventoryStandard
						)
				);
			}
		}

		logger.debug(
				"Selected primary layer: {}", getPrimaryLayer() == null ? "none" : getPrimaryLayer().toDetailedString()
		);
		logger.debug(
				"Selected veteran layer: {}", getVeteranLayer() == null ? "none" : getVeteranLayer().toDetailedString()
		);
	}

	private LayerSummarizationModeCode determineFipMergeModel() {
		// RankOneOnly is legacy and supported only for backwards compatibility. TwoLayer is
		// the mode to be used going forward.
		return LayerSummarizationModeCode.TwoLayer;
	}

	private LayerSummarizationModeCode determineVriMergeModel() {
		// RankOneOnly is legacy and supported only for backwards compatibility. TwoLayer is
		// the mode to be used going forward.
		return LayerSummarizationModeCode.TwoLayer;
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

		Layer selectedPrimaryLayer = null;

		logger.debug("Determining VDYP primary layer");

		Layer deadLayer = determineDeadLayer();
		logger.debug("Polygon dead layer is {}", deadLayer == null ? "not set" : deadLayer.getLayerId());

		if (getTargetedPrimaryLayer() != null) {

			selectedPrimaryLayer = getTargetedPrimaryLayer();
			logger.debug("Layer {} is identified/targeted as the primary layer", selectedPrimaryLayer);
		} else if (getRank1Layer() != null && getRank1Layer() != deadLayer) {

			selectedPrimaryLayer = getRank1Layer();
			logger.debug("Layer {} is primary layer due to being the rank 1 layer", selectedPrimaryLayer);
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

			for (Layer candidate : getLayers().values().stream().filter(l -> !l.getIsDeadLayer()).toList()) {
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
						selectedPrimaryLayer = candidate;
						break;
					}
				}
			}

			if (selectedPrimaryLayer == null) {
				selectedPrimaryLayer = fallbackChoice;
			}

			if (selectedPrimaryLayer != null) {
				var message = new PolygonMessage.Builder().setLayer(selectedPrimaryLayer)
						.setErrorCode(ReturnCode.SUCCESS)
						.setMessage(
								new ValidationMessage(
										ValidationMessageKind.NO_PRIMARY_LAYER_SUPPLIED, this,
										selectedPrimaryLayer.getLayerId()
								)
						).build();
				getMessages().add(message);
			}
		}

		return selectedPrimaryLayer;
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
					veteranLayer.setVdyp7LayerCode(ProjectionTypeCode.VETERAN);

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

		Layer selectedVeteranLayer = null;

		if (getPrimaryLayer() == null) {
			logger.debug("Stand contains no primary layer, therefore stand can not contain a veteran layer.");
		} else if (getVeteranLayer() == null) {

			if (getTargetedVeteranLayer() != null) {
				selectedVeteranLayer = getVeteranLayer();
				logger.debug("Targeted veteran layer '{}' identified as veteran layer", getVeteranLayer().getLayerId());
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

								selectedVeteranLayer = candidate;
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
			selectedVeteranLayer = getVeteranLayer();
			logger.debug("Layer {} was already identified as a veteran layer: '{}'", getVeteranLayer().getLayerId());
		}

		return selectedVeteranLayer;
	}

	private void generateYieldTables(PolygonProjectionState state) {
		// TODO Auto-generated method stub

	}

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
			polygonPercentStockable = 85.0; /* where does this come from??? */
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

				residualPercentStockable = deadPercentStockable <= 1.0 ? 1.0 : deadPercentStockable;
				primaryPercentStockable -= residualPercentStockable;
			}

		} else if (primaryLayer != null) {
			primaryPercentStockable = polygonPercentStockable - veteranPercentStockable - deadPercentStockable
					- regenerationPercentStockable;
		} else if (residualLayer != null) {
			residualPercentStockable = polygonPercentStockable - veteranPercentStockable - deadPercentStockable
					- regenerationPercentStockable;
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

	private void determineAgeRange(PolygonProjectionState state) throws PolygonExecutionException {

		ProjectionContext context = state.getContext();

		try {
			Integer measurementYear = getMeasurementYear();
			long standAgeAtMeasurementYear;
			standAgeAtMeasurementYear = Math.round(determineStandAgeAtYear(measurementYear));

			logger.debug("Determined age is {} at measurement year {}", standAgeAtMeasurementYear, measurementYear);

			// Determine the year range of the projection.

			// Calculate the starting age over which the yield table is to be produced.
			//
			// We are guaranteed that one of the yearStart, ageStart or one of the
			// force year parameters has been supplied. That leaves us with the
			// cases:
			//
			// 1. Both yearStart and ageStart are supplied:
			// startAge is the maximum of the age corresponding to the start year
			// and the supplied ageStart.
			//
			// 2. yearStart parameter is supplied only:
			// standAge is the stand age corresponding at the specified year.
			//
			// 3. ageStart parameter is supplied only:
			// ageStart is the same as this parameter.
			//
			Long startAge = null;

			{
				Long ageAtYear = null;
				if (context.getValidatedParams().getYearStart() != null) {
					Layer layer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
					ageAtYear = Math.round(layer.determineLayerAgeAtYear(context.getValidatedParams().getYearStart()));
				}

				if (ageAtYear != null && context.getValidatedParams().getAgeStart() != null) {
					startAge = ageAtYear > context.getValidatedParams().getAgeStart()
							? context.getValidatedParams().getAgeStart() : ageAtYear;
				} else if (ageAtYear != null) {
					startAge = ageAtYear;
				} else {
					startAge = context.getValidatedParams().getAgeStart().longValue();
				}

				logger.debug("Starting age of yield table has been determined to be {}", startAge);
			}

			// Calculate the finishing age over which the yield table is to be produced.
			//
			// We are guaranteed that one of the yearEnd or ageEnd parameters has been supplied.
			// That leaves us with cases:
			//
			// 1. Both are supplied:
			// endAge is the lesser of the age corresponding to the yearEnd and the supplied ageEnd.
			//
			// 2. Only yearEnd is supplied:
			// endAge is the stand age corresponding at the specified year.
			//
			// 3. Only ageEnd is supplied:
			// endAge is the same as this parameter.
			Long endAge = null;

			{
				Long ageAtYear = null;
				if (context.getValidatedParams().getYearEnd() != null) {
					Layer layer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
					ageAtYear = Math.round(layer.determineLayerAgeAtYear(context.getValidatedParams().getYearEnd()));
				}

				if (ageAtYear != null && context.getValidatedParams().getAgeEnd() != null) {
					endAge = ageAtYear > context.getValidatedParams().getAgeEnd()
							? context.getValidatedParams().getAgeEnd() : ageAtYear;
				} else if (ageAtYear != null) {
					endAge = ageAtYear;
				} else {
					endAge = context.getValidatedParams().getAgeEnd().longValue();
				}

				logger.debug("Ending age of yield table has been determined to be {}", endAge);
			}

			// We need to make an allowance for the reference year and the current year. We need to
			// check if they extend the range of years to be projected.

			if (context.getValidatedParams().getSelectedExecutionOptions()
					.contains(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				if (startAge == null || standAgeAtMeasurementYear < startAge) {
					startAge = standAgeAtMeasurementYear;
				}

				if (endAge == null || standAgeAtMeasurementYear > endAge) {
					endAge = standAgeAtMeasurementYear;
				}

				logger.debug("start and end age after considering reference year: {}, {}", startAge, endAge);
			}

			if (context.getValidatedParams().getSelectedExecutionOptions()
					.contains(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				long standAgeAtCurrentYear = Double.valueOf(determineStandAgeAtYear(LocalDate.now().getYear()))
						.longValue();
				if (startAge == null || standAgeAtCurrentYear < startAge) {
					startAge = standAgeAtCurrentYear;
				}

				if (endAge == null || standAgeAtCurrentYear > endAge) {
					endAge = standAgeAtCurrentYear;
				}

				logger.debug("start and end age after considering current year: {}, {}", startAge, endAge);
			}

			if (context.getValidatedParams().getForceYear() != null) {

				long standAgeAtGivenYear = Double
						.valueOf(determineStandAgeAtYear(context.getValidatedParams().getForceYear())).longValue();
				if (startAge == null || standAgeAtGivenYear < startAge) {
					startAge = standAgeAtGivenYear;
				}

				if (endAge == null || standAgeAtGivenYear > endAge) {
					endAge = standAgeAtGivenYear;
				}

				logger.debug("start and end age after considering special year: {}, {}", startAge, endAge);
			}

			state.setProjectionRange(startAge, endAge);

		} catch (PolygonValidationException pve) {
			throw new PolygonExecutionException(pve);
		}
	}

	private double determineStandAgeAtYear(Integer year) throws PolygonValidationException {
		Layer primaryLayer = findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
		return primaryLayer.determineLayerAgeAtYear(year);
	}

	private void addMessage(ProjectionContext state, String message, Object... args) {
		String messageText = MessageFormat.format(message, args);
		state.getErrorLog().addMessage(messageText);
		logger.debug(messageText);
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

	private void buildProjectionExecutionStructure(PolygonProjectionState state)
			throws ProjectionInternalExecutionException {

		URL rootUrl = ProjectionUtils.class.getClassLoader().getResource("ca/bc/gov/nrs/vdyp/template");
		try {
			String jobName = toString() + '-'
					+ LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(":", "-");

			Path rootFolder = Path.of(rootUrl.toURI());
			Path executionFolder = Files.createTempDirectory(rootFolder, jobName + '-');

			for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

				if (getLayerByProjectionType().get(projectionType) != null) {
					ProjectionUtils.logger.debug("Populating execution folder for projectionType {}", projectionType);
					ProjectionUtils.prepareProjectionTypeFolder(
							rootFolder, executionFolder, projectionType.toString(), "FIPSTART.CTR", "VRISTART.CTR",
							"VRIADJST.CTR", "VDYP.CTR", "VDYPBACK.CTR"
					);
				}
			}

			state.setExecutionFolder(executionFolder);
		} catch (IOException | URISyntaxException e) {
			throw new ProjectionInternalExecutionException(e);
		}
	}
}
