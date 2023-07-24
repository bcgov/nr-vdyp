package ca.bc.gov.nrs.vdyp.model;

import java.util.Collections;
import java.util.Map;

public class BaseVdypLayer<S extends BaseVdypSpecies> {

	protected final String polygonIdentifier;
	protected final Layer layer;
	protected float ageTotal;
	protected float height;
	protected float yearsToBreastHeight;
	Map<String, S> species = Collections.emptyMap();

	public BaseVdypLayer(
			String polygonIdentifier, Layer layer, float ageTotal, float height, float yearsToBreastHeight
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layer = layer;
		this.ageTotal = ageTotal;
		this.height = height;
		this.yearsToBreastHeight = yearsToBreastHeight;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public Layer getLayer() {
		return layer;
	}

	public float getAgeTotal() {
		return ageTotal;
	}

	public float getHeight() {
		return height;
	}

	public float getYearsToBreastHeight() {
		return yearsToBreastHeight;
	}

	public void setAgeTotal(float ageTotal) {
		this.ageTotal = ageTotal;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setYearsToBreastHeight(float yearsToBreastHeight) {
		this.yearsToBreastHeight = yearsToBreastHeight;
	}

	public Map<String, S> getSpecies() {
		return species;
	}

	public void setSpecies(Map<String, S> species) {
		this.species = species;
	}

}
