package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;

import ca.bc.gov.nrs.vdyp.common.Computed;

public class VdypLayer extends BaseVdypLayer<VdypSpecies> {

	float breastHeightAge;
	String primaryGenus;

	Coefficients baseAreaByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1);

	public VdypLayer(String polygonIdentifier, Layer layer) {
		super(polygonIdentifier, layer);
	}

	public float getBreastHeightAge() {
		return breastHeightAge;
	}

	public void setBreastHeightAge(float breastHeightAge) {
		this.breastHeightAge = breastHeightAge;
	}

	public String getPrimaryGenus() {
		return primaryGenus;
	}

	public void setPrimaryGenus(String primaryGenus) {
		this.primaryGenus = primaryGenus;
	}

	@Computed
	public VdypSpecies getPrimarySpeciesRecord() {
		return getSpecies().get(primaryGenus);
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		assert baseAreaByUtilization.indexFrom == -1 : "baseAreaByUtilization must be indexed from -1";
		assert baseAreaByUtilization.size() == 6 : "baseAreaByUtilization must have index -1 - 4";
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

}
