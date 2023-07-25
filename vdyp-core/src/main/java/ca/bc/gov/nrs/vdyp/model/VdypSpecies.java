package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;
import java.util.Map;

public class VdypSpecies extends BaseVdypSpecies {

	float height; // LVCOM/HL
	Coefficients baseAreaByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1);

	public VdypSpecies(
			String polygonIdentifier, Layer layer, String genus, float percentGenus, float height,
			Map<String, Float> speciesPercent
	) {
		super(polygonIdentifier, layer, genus, percentGenus, speciesPercent);
		this.height = height;
	}

	public VdypSpecies(BaseVdypSpecies toCopy, float height) {
		super(toCopy);
		this.height = height;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
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
