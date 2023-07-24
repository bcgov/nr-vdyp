package ca.bc.gov.nrs.vdyp.model;

import java.util.Map;

public class VdypSpecies extends BaseVdypSpecies {

	float height; // LVCOM/HL

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

}
