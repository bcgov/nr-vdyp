package ca.bc.gov.nrs.vdyp.model;

import java.util.Collections;
import java.util.Map;

public class BaseVdypPolygon<L extends BaseVdypLayer<?>, PA> {

	String polygonIdentifier; // FIP_P/POLYDESC
	PA percentAvailable; // FIP_P2/PCTFLAND
	Map<Layer, L> layers = Collections.emptyMap();

	public BaseVdypPolygon(String polygonIdentifier, PA percentAvailable) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.percentAvailable = percentAvailable;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public void setPolygonIdentifier(String polygonIdentifier) {
		this.polygonIdentifier = polygonIdentifier;
	}

	public Map<Layer, L> getLayers() {
		return layers;
	}

	public void setLayers(Map<Layer, L> layers) {
		this.layers = layers;
	}

	public PA getPercentAvailable() {
		return percentAvailable;
	}

	public void setPercentAvailable(PA percentAvailable) {
		this.percentAvailable = percentAvailable;
	}
}
