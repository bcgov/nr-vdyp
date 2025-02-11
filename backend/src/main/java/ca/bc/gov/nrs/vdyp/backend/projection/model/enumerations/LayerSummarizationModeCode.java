package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

/**
 * Indicates the layer summarization mode used to combine layers into the specific VDYP7 model.
 */
public enum LayerSummarizationModeCode {
	
	Unknown,
	
	/**
	 * The layer labelled or determined to be the Rank 1 layer is the only layer processed. This processing mode is no
	 * longer used and was replaced by 2 Layer Processing.
	 */
	RankOneOnly,

	/** Describes the Two layer Processing as described in IPSCB460 */
	TwoLayer;

	public static LayerSummarizationModeCode getDefault() {
		return Unknown;
	}
}
