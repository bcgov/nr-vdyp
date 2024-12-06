package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZone;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModel;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.LayerSummarizationMode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationTypes;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationTypes;
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
	private double percentStockable;

	/** The amount of the polygon that has suffered significant kill. */
	private double percentStockableDead;

	/** The factor to multiply predicted yields by */
	private double yieldFactor;

	private Map<OtherVegetationTypes, Double> otherVegetationTypes;
	private Map<NonVegetationTypes, Double> nonVegetationTypes;

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
	private Map<ProjectionType, GrowthModel> initialProcessingResults;
	private Map<ProjectionType, GrowthModel> adjustmentProcessingResults;
	private Map<ProjectionType, GrowthModel> projectionProcessingResults;
	private Map<ProjectionType, GrowthModel> firstYearValidYields;

	/** The messages generated during the projection of the polygon. */
	private List<PolygonMessage> messages;
}
