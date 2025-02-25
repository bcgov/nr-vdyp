package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;

import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class ScsvPolygonStream extends AbstractPolygonStream {

	public ScsvPolygonStream(
			ProjectionContext context, InputStream polygonStream, InputStream layersStream, InputStream historyStream,
			InputStream nonVegetationStream, InputStream otherVegetationStream, InputStream polygonIdStream,
			InputStream speciesStream, InputStream vriAdjustStream
	) {
		super(context);
	}

	@Override
	public Polygon getNextPolygon() {
		throw new UnsupportedOperationException("SCSV input files not (yet) supported.");
	}

	@Override
	public boolean hasNextPolygon() {
		return false;
	}
}
