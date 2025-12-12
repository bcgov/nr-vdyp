package ca.bc.gov.nrs.api.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.ProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardDataStreamReader;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class TestProjectionResultsReader implements ProjectionResultsReader {

	private TestHelper testHelper;

	private InputStream polygonStream;
	private InputStream speciesStream;
	private InputStream utilizationsStream;

	public TestProjectionResultsReader(
			TestHelper testHelper, InputStream polygonStream, InputStream speciesStream, InputStream utilizationsStream
	) {
		this.testHelper = testHelper;

		this.polygonStream = polygonStream;
		this.speciesStream = speciesStream;
		this.utilizationsStream = utilizationsStream;
	}

	@Override
	public Map<Integer, VdypPolygon> read(Polygon polygon) throws YieldTableGenerationException {

		var projectionResultsByYear = new HashMap<Integer, VdypPolygon>();

		var expectedPolygonIdentifier = new PolygonIdentifier(
				polygon.getMapSheet(), polygon.getPolygonNumber(), polygon.getDistrict(), 0 /* expect any year */
		);

		try (
				ForwardDataStreamReader reader = testHelper
						.buildForwardDataStreamReader(polygonStream, speciesStream, utilizationsStream);
		) {

			var vdypPolygon = reader.readNextPolygon(false /* do not run post-create adjustments */);
			while (vdypPolygon.isPresent()
					&& expectedPolygonIdentifier.getBase().equals(vdypPolygon.get().getPolygonIdentifier().getBase())) {

				projectionResultsByYear.put(vdypPolygon.get().getPolygonIdentifier().getYear(), vdypPolygon.get());

				vdypPolygon = reader.readNextPolygon(false /* do not run post-create adjustments */);
			}

			if (vdypPolygon.isPresent()) {
				throw new YieldTableGenerationException(
						MessageFormat.format(
								"Expected exactly one polygon in the Forward output, but saw {1} as well",
								vdypPolygon.get().getPolygonIdentifier()
						)
				);
			}
		} catch (IOException | ProcessingException e) {
			throw new YieldTableGenerationException(e);
		}

		return projectionResultsByYear;
	}

}
