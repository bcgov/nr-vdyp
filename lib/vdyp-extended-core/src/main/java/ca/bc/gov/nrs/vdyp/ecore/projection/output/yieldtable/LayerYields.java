package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

/** Contains the yields of a particular layer at a particular age. */
public record LayerYields(
		/**
		 * Indicates whether or not yields were predicted for the year in question by VDYP7. If not, then manual growth
		 * information may have been derived using species input parameters but no yield information will be there.
		 */
		boolean bYieldsPredicted,

		/**
		 * Indicates whether or not the yields correspond to the dominant SP0 in the layer at the specified stand
		 * age/year. This will always be FALSE if stand summary information is requested.
		 */
		boolean isDominantSp0,

		/**
		 * Identifies which Species Code (Sp0) within the stand this yield information corresponds to.
		 * <p>
		 * If null, indicates the entire stand is summarized in this structure.
		 */
		String sp0Name,

		/** The stand calendar year at which these yields apply. */
		int standYear,

		/** The total age (ytbh + yabh) of the species at the calendar year. */
		double speciesAge,

		/** The dominant height of the layer. */
		double dominantHeight,

		/** The Lorey height of the layer. */
		double loreyHeight,

		/** The site index of the layer. */
		double siteIndex,

		/** The diameter of the stand at this layer. */
		double diameter,

		/** Trees-per-hectare of the stand at this layer. */
		double treesPerHectare,

		/** Whole stem volume of the stand at this layer. */
		double wholeStemVolume,

		/** Close utilization volume of the stand at this layer. */
		double closeUtilizationVolume,

		/** Close utilization volume, less decay, of the stand at this layer. */
		double cuVolumeLessDecay,

		/** Close utilization volume, less decay and wastage, of the stand at this layer. */
		double cuVolumeLessDecayWastage,

		/** Close utilization volume, less decay, wastage and breakage, of the stand at this layer. */
		double cuVolumeLessDecayWastageBreakage,

		/** The Basal Area at Utilization Level 7.5cm+. */
		double basalArea75cm,

		/** The Basal Area at Utilization Level 7.5cm+. */
		double basalArea125cm,

		/**
		 * The individual SP0 Stand Percent of specified SP0.
		 * <p>
		 * If the iSP0Index is 0, this should always be 100%.
		 */
		double reportedStandPercent,

		/**
		 * Indicates the specific Site Index Curve used to generate the volumes for the requested species group. If the
		 * requested yields correspond to a stand summary, this data member will be null.
		 */
		Integer siteIndexCurve
) {
}
