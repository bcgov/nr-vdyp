package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import java.io.InputStream;

import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;

public class IcsvPolygonStream extends AbstractPolygonStream {

	public IcsvPolygonStream(ProjectionContext context, InputStream icsvInputStream) {
		super(context);
	}

	@Override
	public Polygon getNextPolygon() {
		throw new UnsupportedOperationException("ICSV input files not (yet) supported.");
	}

	@Override
	public boolean hasNextPolygon() {
		return false;
	}
}
