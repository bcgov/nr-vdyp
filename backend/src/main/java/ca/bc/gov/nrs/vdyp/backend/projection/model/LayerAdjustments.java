package ca.bc.gov.nrs.vdyp.backend.projection.model;

/**
 * Contains the adjustments that could be applied to a single layer. These adjustments are designed to be run through
 * the VRIADJST process.
 */
public class LayerAdjustments {
	/**
	 * Indicates whether or not the adjustments records were initialized with default values or not.
	 */
	private boolean bContainsDefaults;

	/** Specifies the Basal Area of the layer at the 7.5cm+ utilization level. */
	private double fAdjBA075;
	/** Identifies the Lorey Height at the 7.5cm+ utilization level. */
	private double fAdjLH075;
	/** Specifies the Whole Stem Volume of the layer at the 7.5cm+ utilization level. */
	private double fAdjWSV075;
	/** Specifies the Basal Area of the layer at the 12.5cm+ utilization level. */
	private double fAdjBA125;
	/** Specifies the Whole Stem Volume of the layer at the 12.5cm+ utilization level. */
	private double fAdjWSV125;
	/** Specifies the Close Utilization Volume of the layer at the 12.5cm+ utilization level. */
	private double fAdjVCU125;
	/** Specifies the Close Utilization Volume, less Decay, of the layer at the 12.5cm+ utilization level. */
	private double fAdjVD125;
	/** Specifies the Close Utilization Volume, less Decay and Wastage of the layer at the 12.5cm+ utilization level. */
	private double fAdjVDW125;
}
