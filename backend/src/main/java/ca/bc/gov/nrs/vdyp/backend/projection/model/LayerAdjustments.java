package ca.bc.gov.nrs.vdyp.backend.projection.model;

/**
 * Contains the adjustments that could be applied to a single layer. These adjustments are designed to be run through
 * the VRIADJST process.
 */
public class LayerAdjustments {
	/**
	 * Indicates whether or not the adjustments records were initialized with default values or not.
	 */
	private boolean doesContainDefaults;

	/** Specifies the Basal Area adjustment of the layer at the 7.5cm+ utilization level. */
	private Double adjustmentBA075;
	/** Identifies the Lorey Height adjustmentat the 7.5cm+ utilization level. */
	private Double adjustmentLH075;
	/** Specifies the Whole Stem Volume adjustmentof the layer at the 7.5cm+ utilization level. */
	private Double adjustmentWSV075;
	/** Specifies the Basal Area adjustmentof the layer at the 12.5cm+ utilization level. */
	private Double adjustmentBA125;
	/** Specifies the Whole Stem Volume adjustmentof the layer at the 12.5cm+ utilization level. */
	private Double adjustmentWSV125;
	/** Specifies the Close Utilization Volume adjustment of the layer at the 12.5cm+ utilization level. */
	private Double adjustmentVCU125;
	/** Specifies the Close Utilization Volume, less Decay, adjustment of the layer at the 12.5cm+ utilization level. */
	private Double adjustmentVD125;
	
	/**
	 * Specifies the Close Utilization Volume, less Decay and Wastage, adjustment of the layer at the 12.5cm+
	 * utilization level.
	 */
	private Double adjustmentVDW125;

	public boolean isDoesContainDefaults() {
		return doesContainDefaults;
	}

	public Double getAdjustmentBA075() {
		return adjustmentBA075;
	}

	public Double getAdjustmentLH075() {
		return adjustmentLH075;
	}

	public Double getAdjustmentWSV075() {
		return adjustmentWSV075;
	}

	public Double getAdjustmentBA125() {
		return adjustmentBA125;
	}

	public Double getAdjustmentWSV125() {
		return adjustmentWSV125;
	}

	public Double getAdjustmentVCU125() {
		return adjustmentVCU125;
	}

	public Double getAdjustmentVD125() {
		return adjustmentVD125;
	}

	public Double getAdjustmentVDW125() {
		return adjustmentVDW125;
	}
	
	public static class Builder {
		private LayerAdjustments layerAdjustments = new LayerAdjustments();

		public Builder setDoesContainDefaults(boolean doesContainDefaults) {
			layerAdjustments.doesContainDefaults = doesContainDefaults;
			return this;
		}
	
		public Builder setAdjustmentBA075(Double adjustmentBA075) {
			layerAdjustments.adjustmentBA075 = adjustmentBA075;
			return this;
		}
	
		public Builder setAdjustmentLH075(Double adjustmentLH075) {
			layerAdjustments.adjustmentLH075 = adjustmentLH075;
			return this;
		}
	
		public Builder setAdjustmentWSV075(Double adjustmentWSV075) {
			layerAdjustments.adjustmentWSV075 = adjustmentWSV075;
			return this;
		}
	
		public Builder setAdjustmentBA125(Double adjustmentBA125) {
			layerAdjustments.adjustmentBA125 = adjustmentBA125;
			return this;
		}
	
		public Builder setAdjustmentWSV125(Double adjustmentWSV125) {
			layerAdjustments.adjustmentWSV125 = adjustmentWSV125;
			return this;
		}
	
		public Builder setAdjustmentVCU125(Double adjustmentVCU125) {
			layerAdjustments.adjustmentVCU125 = adjustmentVCU125;
			return this;
		}
	
		public Builder setAdjustmentVD125(Double adjustmentVD125) {
			layerAdjustments.adjustmentVD125 = adjustmentVD125;
			return this;
		}
	
		public Builder setAdjustmentVDW125(Double adjustmentVDW125) {
			layerAdjustments.adjustmentVDW125 = adjustmentVDW125;
			return this;
		}
	}
}
