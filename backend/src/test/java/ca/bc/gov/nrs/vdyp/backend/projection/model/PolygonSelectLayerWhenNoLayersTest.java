package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.io.ByteArrayInputStream;
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

public class PolygonSelectLayerWhenNoLayersTest {

	private Polygon polygon;

	@BeforeEach
	void beforeEach() throws PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		var streams = new HashMap<String, InputStream>();
		var polygonStreamFile = FileHelper
				.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");

		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
		streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
		Assert.assertTrue(polygonStream.hasNextPolygon());

		polygon = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon);
	}

	@Test
	void testSelectLayerWhenParameterNull() {

		try {
			polygon.findSpecificLayer(null);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("findSpecificLayer: layerId is null", e.getLocalizedMessage());
		}
	}

	@Test
	void testSelectSpanningLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING);
		Assert.assertNull(selectedLayer);
	}

	@Test
	void testSelectPrimaryLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY);
		Assert.assertNull(selectedLayer);
	}

	@Test
	void testSelectNamedLayer() {
		var selectedLayer = polygon.findSpecificLayer("layer");
		Assert.assertNull(selectedLayer);
	}
}
