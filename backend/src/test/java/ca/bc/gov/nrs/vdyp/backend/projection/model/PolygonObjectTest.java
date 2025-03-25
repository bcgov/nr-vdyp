package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class PolygonObjectTest {

	private Polygon polygon1;
	private Polygon polygon2;

	@BeforeEach
	void beforeEach() throws IOException, PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
			Assert.assertTrue(polygonStream.hasNextPolygon());

			polygon1 = polygonStream.getNextPolygon();
			Assert.assertNotNull(polygon1);
		}

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
			Assert.assertTrue(polygonStream.hasNextPolygon());

			polygon2 = polygonStream.getNextPolygon();
			Assert.assertNotNull(polygon2);
		}
	}

	@Test
	void testObjectMethods() {

		Assert.assertEquals(polygon1, polygon2);
		Assert.assertTrue(polygon2.hashCode() == polygon1.hashCode());
		Assert.assertTrue(polygon1.compareTo(polygon2) == 0);

		Assert.assertEquals(Long.valueOf(polygon1.getFeatureId()).toString(), polygon1.toString());
		Assert.assertEquals(Long.valueOf(polygon1.getFeatureId()).toString(), polygon1.toDetailedString());
	}
}
