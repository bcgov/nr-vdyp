package ca.bc.gov.nrs.vdyp.backend.projection.model;

/** Contains the yields of a particular layer at a particular age. */
public class LayerYields {
	/**
	 * Indicates whether or not yields were predicted for the year in question by VDYP7. If not, then manual growth
	 * information may have been derived using species input parameters but no yield information will be there.
	 */
	boolean bYieldsPredicted;

	/**
	 * Indicates whether or not the yields correspond to the dominant SP0 in the layer at the specified stand age/year.
	 * This will always be FALSE if stand summary information is requested.
	 */
	boolean bDominantSP0;

	/**
	 * Identifies which Species Code (Sp0) number within the stand this yield information corresponds to.
	 * <p>
	 * If 0, indicates the entire stand is summarized in this structure. Otherwise must range from 1 to the number of
	 * Species Groups in the polygon.
	 */
	int iSP0Index;

	/** The stand calendar year at which these yields apply. */
	int iStandYear;

	/** The age of the species at the calendar year. */
	double fSpeciesAge;

	/** The dominant height of the layer. */
	double fDomHeight;

	/** The Lorey height of the layer. */
	double fLoreyHeight;

	/** The site index of the layer. */
	double fSI;

	/** The diameter of the stand at this layer. */
	double fDiameter;

	/** Trees-per-hectare of the stand at this layer. */
	double fTPH;

	/** Whole stem volume of the stand at this layer. */
	double fVolumeWS;
	/** Close utilization volume of the stand at this layer. */
	double fVolumeCU;
	/** Close utilization volume, less decay, of the stand at this layer. */
	double fVolumeD;
	/** Close utilization volume, less decay and wastage, of the stand at this layer. */
	double fVolumeDW;
	/** Close utilization volume, less decay, wastage and breakage, of the stand at this layer. */
	double fVolumeDWB;
	/** The Basal Area at Utilization Level 7.5cm+. */
	double fBasalArea075;
	/** The Basal Area at Utilization Level 7.5cm+. */
	double fBasalArea125;

	/**
	 * The individual SP0 Stand Percent of specified SP0.
	 * <p>
	 * If the iSP0Index is 0, this should always be 100%.
	 */
	double fReportedStandPercent;

	/**
	 * Indicates the specific Site Index Curve used to generate the volumes for the requested species group. If the
	 * requested yields correspond to a stand summary, this data member will be null.
	 */
	Integer iSICurveUsed;
}
