package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;

import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class DcsvPolygonStream extends AbstractPolygonStream {

	DcsvPolygonStream(ProjectionContext context, InputStream dcsvInputStream) {
		super(context);
	}

	@Override
	public Polygon getNextPolygon() {
		throw new UnsupportedOperationException("DCSV input files not (yet) supported.");
	}

	@Override
	public boolean hasNextPolygon() {
		return false;
	}
}
