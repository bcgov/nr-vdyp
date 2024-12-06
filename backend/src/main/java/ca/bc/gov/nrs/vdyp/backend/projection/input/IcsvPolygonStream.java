package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class IcsvPolygonStream extends AbstractPolygonStream {

	public IcsvPolygonStream(InputStream icsvInputStream) {
		// TODO Auto-generated constructor stub
	}

	@Override
	Polygon getNextPolygon() {
		throw new UnsupportedOperationException("ICSV input files not (yet) supported.");
	}

	@Override
	public boolean hasNextPolygon() {
		return false;
	}
}
