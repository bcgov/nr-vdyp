package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

public class PolygonObjectTest {

	private Polygon polygon1;
	private Polygon polygon2;

	@BeforeEach
	void beforeEach() throws PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
			assertTrue(polygonStream.hasNextPolygon());

			polygon1 = polygonStream.getNextPolygon();
			assertNotNull(polygon1);
		}

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
			assertTrue(polygonStream.hasNextPolygon());

			polygon2 = polygonStream.getNextPolygon();
			assertNotNull(polygon2);
		}
	}

	@Test
	void testObjectMethods() {

		assertEquals(polygon1, polygon2);
		assertTrue(polygon2.hashCode() == polygon1.hashCode());
		assertTrue(polygon1.compareTo(polygon2) == 0);

		assertEquals(Long.valueOf(polygon1.getFeatureId()).toString(), polygon1.toString());
		assertEquals(Long.valueOf(polygon1.getFeatureId()).toString(), polygon1.toDetailedString());
	}
}
