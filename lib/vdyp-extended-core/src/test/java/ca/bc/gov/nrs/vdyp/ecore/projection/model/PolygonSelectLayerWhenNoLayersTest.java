package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
		assertTrue(polygonStream.hasNextPolygon());

		polygon = polygonStream.getNextPolygon();
		assertNotNull(polygon);
	}

	@Test
	void testSelectLayerWhenParameterNull() {

		try {
			polygon.findSpecificLayer(null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("findSpecificLayer: layerId is null", e.getLocalizedMessage());
		}
	}

	@Test
	void testSelectSpanningLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING);
		assertNull(selectedLayer);
	}

	@Test
	void testSelectPrimaryLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY);
		assertNull(selectedLayer);
	}

	@Test
	void testSelectNamedLayer() {
		var selectedLayer = polygon.findSpecificLayer("layer");
		assertNull(selectedLayer);
	}
}
