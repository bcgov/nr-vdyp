package ca.bc.gov.nrs.vdyp.model;

import java.util.Map;

public abstract class BaseVdypSpecies {
	final String polygonIdentifier; // FIP_P/POLYDESC
	final Layer layer; // This is also represents the distinction between data stored in
	// FIPL_1(A) and FIP_V(A). Where VDYP7 stores both and looks at certain values
	// to determine if a layer is "present". VDYP8 stores them in a map keyed by
	// this value

	final String genus; // FIPSA/SP0V

	float percentGenus; // FIPS/PCTVOLV

	// This is computed from percentGenus, but VDYP7 computes it in a way that might
	// lead to a slight difference so it's stored separately and can be modified.
	float fractionGenus; // RFBASP0/FR

	Map<String, Float> speciesPercent; // Map from

	protected BaseVdypSpecies(
			String polygonIdentifier, Layer layer, String genus, float percentGenus, Map<String, Float> speciesPercent
	) {
		this.polygonIdentifier = polygonIdentifier;
		this.layer = layer;
		this.genus = genus;

		this.setPercentGenus(percentGenus);

		this.speciesPercent = speciesPercent;
	}

	protected BaseVdypSpecies(BaseVdypSpecies toCopy) {
		this(
				toCopy.getPolygonIdentifier(), //
				toCopy.getLayer(), //
				toCopy.getGenus(), //
				toCopy.getPercentGenus(), //
				toCopy.getSpeciesPercent() //
		);
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public Layer getLayer() {
		return layer;
	}

	public float getPercentGenus() {
		return percentGenus;
	}

	public float getFractionGenus() {
		return fractionGenus;
	}

	public void setPercentGenus(float percentGenus) {
		this.percentGenus = percentGenus;
		this.fractionGenus = percentGenus / 100f;
	}

	public void setFractionGenus(float fractionGenus) {
		this.fractionGenus = fractionGenus;
	}

	public Map<String, Float> getSpeciesPercent() {
		return speciesPercent;
	}

	public void setSpeciesPercent(Map<String, Float> speciesPercent) {
		this.speciesPercent = speciesPercent;
	}

	public String getGenus() {
		return genus;
	}

}
