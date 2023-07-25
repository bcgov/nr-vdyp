package ca.bc.gov.nrs.vdyp.fip.model;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipSpecies extends BaseVdypSpecies {

	static final String POLYGON_IDENTIFIER = "POLYGON_IDENTIFIER"; // POLYDESC
	static final String LAYER = "LAYER"; // LAYER

	public FipSpecies(
			String polygonIdentifier, Layer layer, String genus	) {
		super(polygonIdentifier, layer, genus);
	}

}
