package ca.bc.gov.nrs.vdyp.model;

import ca.bc.gov.nrs.vdyp.common.Utils;

/**
 * Common accessors for utilization vectors shared by Layer and Species
 *
 * @author Kevin Smith, Vivid Solutions
 */
public interface VdypUtilizationHolder {

	/**
	 * Close utilization volume net of decay, waste and breakage for utilization index -1 through 4
	 */
	void setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
			UtilizationVector closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	);

	/**
	 * Close utilization volume net of decay, waste and breakage for utilization index -1 through 4
	 */
	UtilizationVector getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization();

	/**
	 * Close utilization volume net of decay and waste for utilization index -1 through 4
	 */
	void setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
			UtilizationVector closeUtilizationVolumeNetOfDecayAndWasteByUtilization
	);

	/**
	 * Close utilization volume net of decay and waste for utilization index -1 through 4
	 */
	UtilizationVector getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization();

	/**
	 * Close utilization volume net of decay for utilization index -1 through 4
	 */
	void setCloseUtilizationVolumeNetOfDecayByUtilization(
			UtilizationVector closeUtilizationNetVolumeOfDecayByUtilization
	);

	/**
	 * Close utilization volume net of decay for utilization index -1 through 4
	 */
	UtilizationVector getCloseUtilizationVolumeNetOfDecayByUtilization();

	/**
	 * Close utilization volume for utilization index -1 through 4
	 */
	void setCloseUtilizationVolumeByUtilization(UtilizationVector closeUtilizationVolumeByUtilization);

	/**
	 * Close utilization volume for utilization index -1 through 4
	 */
	UtilizationVector getCloseUtilizationVolumeByUtilization();

	/**
	 * Whole stem volume for utilization index -1 through 4
	 */
	void setWholeStemVolumeByUtilization(UtilizationVector wholeStemVolumeByUtilization);

	/**
	 * Whole stem volume for utilization index -1 through 4
	 */
	UtilizationVector getWholeStemVolumeByUtilization();

	/**
	 * Trees per hectare for utilization index -1 through 4
	 */
	void setTreesPerHectareByUtilization(UtilizationVector treesPerHectareByUtilization);

	/**
	 * Trees per hectare for utilization index -1 through 4
	 */
	UtilizationVector getTreesPerHectareByUtilization();

	/**
	 * Quadratic mean of diameter for utilization index -1 through 4
	 */
	void setQuadraticMeanDiameterByUtilization(UtilizationVector quadraticMeanDiameterByUtilization);

	/**
	 * Quadratic mean of diameter for utilization index -1 through 4
	 */
	UtilizationVector getQuadraticMeanDiameterByUtilization();

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	void setLoreyHeightByUtilization(UtilizationVector loreyHeightByUtilization);

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	UtilizationVector getLoreyHeightByUtilization();

	/**
	 * Base area for utilization index -1 through 4
	 */
	void setBaseAreaByUtilization(UtilizationVector baseAreaByUtilization);

	/**
	 * Base area for utilization index -1 through 4
	 */
	UtilizationVector getBaseAreaByUtilization();

	static UtilizationVector emptyUtilization() {
		return new UtilizationVector(0f, 0f, 0f, 0f, 0f, 0f);
	}

	static UtilizationVector emptyLoreyHeightUtilization() {
		return new UtilizationVector(0f, 0f);
	}

	static interface Builder {

		public void loreyHeight(UtilizationVector vector);

		public void baseArea(UtilizationVector vector);

		public void treesPerHectare(UtilizationVector utilizationVector);

		public void quadMeanDiameter(UtilizationVector utilizationVector);

		public void wholeStemVolume(UtilizationVector utilizationVector);

		public void closeUtilizationVolumeByUtilization(UtilizationVector utilizationVector);

		public void closeUtilizationVolumeNetOfDecayByUtilization(UtilizationVector utilizationVector);

		public void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(UtilizationVector utilizationVector);

		public void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(UtilizationVector utilizationVector);

		// The following methods are syntactic sugar for the preceding

		public default void loreyHeight(float height) {
			this.loreyHeight(Utils.heightVector(0, height));
		}

		public default void loreyHeight(float small, float height) {
			this.loreyHeight(Utils.heightVector(small, height));
		}

		public default void baseArea(float small, float u1, float u2, float u3, float u4) {
			this.baseArea(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void baseArea(float baseArea) {
			this.baseArea(Utils.utilizationVector(baseArea));
		}

		public default void treesPerHectare(float small, float u1, float u2, float u3, float u4) {
			this.treesPerHectare(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void treesPerHectare(float height) {
			this.treesPerHectare(Utils.utilizationVector(height));
		}

		public default void quadMeanDiameter(float small, float uAll, float u1, float u2, float u3, float u4) {
			this.quadMeanDiameter(Utils.utilizationVector(small, uAll, u1, u2, u3, u4));
		}

		public default void quadMeanDiameter(float height) {
			this.quadMeanDiameter(Utils.utilizationVector(height));
		}

		public default void wholeStemVolume(float small, float u1, float u2, float u3, float u4) {
			this.wholeStemVolume(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void wholeStemVolume(float volume) {
			this.wholeStemVolume(Utils.utilizationVector(volume));
		}

		public default void closeUtilizationVolumeByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationVolumeByUtilization(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void closeUtilizationVolumeByUtilization(float volume) {
			this.closeUtilizationVolumeByUtilization(Utils.utilizationVector(volume));
		}

		public default void
				closeUtilizationVolumeNetOfDecayByUtilization(float small, float u1, float u2, float u3, float u4) {
			this.closeUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void closeUtilizationVolumeNetOfDecayByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayByUtilization(Utils.utilizationVector(volume));
		}

		public default void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
				float small, float u1, float u2, float u3, float u4
		) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(small, u1, u2, u3, u4));
		}

		public default void closeUtilizationVolumeNetOfDecayAndWasteByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(Utils.utilizationVector(volume));
		}

		public default void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
				float small, float u1, float u2, float u3, float u4
		) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(small, u1, u2, u3, u4)
			);
		}

		public default void closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(float volume) {
			this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(Utils.utilizationVector(volume));
		}

	}
}
