package ca.bc.gov.nrs.vdyp.model;

import java.util.Map;

public class VdypLayer extends BaseVdypLayer<VdypSpecies> {

	float breastHeightAge;

	public VdypLayer(
			String polygonIdentifier, Layer layer, float ageTotal, float height, float yearsToBreastHeight,
			float breastHeightAge, Map<String, VdypSpecies> species
	) {
		super(polygonIdentifier, layer, ageTotal, height, yearsToBreastHeight);
		this.breastHeightAge = breastHeightAge;
		setSpecies(species);
	}

	public float getBreastHeightAge() {
		return breastHeightAge;
	}

	public void setBreastHeightAge(float breastHeightAge) {
		this.breastHeightAge = breastHeightAge;
	}

}
