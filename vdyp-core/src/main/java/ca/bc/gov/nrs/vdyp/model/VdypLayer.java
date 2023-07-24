package ca.bc.gov.nrs.vdyp.model;

import java.util.Map;

public class VdypLayer extends BaseVdypLayer<VdypSpecies> {

	float breastHeightAge;
	String primaryGenus;

	public VdypLayer(
			String polygonIdentifier, Layer layer, float ageTotal, float height, float yearsToBreastHeight,
			float breastHeightAge, Map<String, VdypSpecies> species, String primaryGenus
	) {
		super(polygonIdentifier, layer, ageTotal, height, yearsToBreastHeight);
		this.breastHeightAge = breastHeightAge;
		this.primaryGenus = primaryGenus;
		setSpecies(species);
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

	/**
	 * Convenience method to get the species record pointed to by primaryGenus
	 *
	 * @return
	 */
	public VdypSpecies getPrimarySpeciesRecord() {
		return getSpecies().get(primaryGenus);
	}
}
