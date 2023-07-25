package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;

public class VdypSpecies extends BaseVdypSpecies {

	Coefficients baseAreaByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1);
	Coefficients loreyHeightByUtilization = new Coefficients(Arrays.asList(0f, 0f), -1); // LVCOM/HL

	public VdypSpecies(
			String polygonIdentifier, Layer layer, String genus
	) {
		super(polygonIdentifier, layer, genus);
	}
	
	
	public VdypSpecies(BaseVdypSpecies toCopy) {
		super(toCopy);
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
		assert baseAreaByUtilization.size() == 2 : "baseAreaByUtilization must have index -1 - 0";
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
		assert loreyHeightByUtilization.indexFrom == -1 : "loreyHeightByUtilization must be indexed from -1";
		assert loreyHeightByUtilization.size() == 2 : "loreyHeightByUtilization must have index -1 - 0";
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

}
