package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

public class PolygonSelectLayerWhenLayersTest {

	private Polygon polygon;

	@BeforeEach
	void beforeEach() throws PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		var streams = new HashMap<String, InputStream>();
		var polygonStreamFile = FileHelper
				.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_POLY.csv");
		var layersStreamFile = FileHelper
				.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_LAYER.csv");

		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
		streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layersStreamFile);

		var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
		assertTrue(polygonStream.hasNextPolygon());

		polygon = polygonStream.getNextPolygon();
		assertNotNull(polygon);
	}

	@Test
	void testSelectLayerWhenParameterNull() {

		assertThrows(IllegalArgumentException.class, () -> polygon.findSpecificLayer(null));
	}

	@Test
	void testSelectSpanningLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING);
		assertNull(selectedLayer);
	}

	@Test
	void testSelectPrimaryLayer() {
		var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY);
		assertEquals("13919428:1", selectedLayer.toString());
	}

	@Test
	void testSelectDeadLayer() {
		var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.DEAD);
		if (selectedLayer == null) {
			fail();
		} else {
			assertEquals("13919428:D", selectedLayer.toString());
		}
	}

	@Test
	void testSelectResidualLayer() {
		var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.RESIDUAL);
		assertNull(selectedLayer);
	}

	@Test
	void testSelectRegenerationLayer() {
		var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.REGENERATION);
		if (selectedLayer == null) {
			fail();
		} else {
			assertEquals("13919428:2", selectedLayer.toString());
		}
	}

	@Test
	void testSelectNamedLayer() {
		var selectedLayer = polygon.findSpecificLayer("1");
		assertNotNull(selectedLayer);
	}
}
