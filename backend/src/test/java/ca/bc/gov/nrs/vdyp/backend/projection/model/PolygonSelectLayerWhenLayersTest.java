package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class PolygonSelectLayerWhenLayersTest {

	private Polygon polygon;

	@BeforeEach
	void beforeEach() throws IOException, PolygonValidationException, ProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		var streams = new HashMap<String, InputStream>();
		var polygonStreamFile = FileHelper
				.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_POLY.csv");
		var layersStreamFile = FileHelper
				.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_LAYER.csv");

		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
		streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layersStreamFile);

		var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
		Assert.assertTrue(polygonStream.hasNextPolygon());

		polygon = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon);
	}

	@Test
	void testSelectLayerWhenParameterNull() throws IOException {

		Assert.assertThrows(IllegalArgumentException.class, () -> polygon.findSpecificLayer(null));
	}

	@Test
	void testSelectSpanningLayer() {
		try {
			var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_SPANNING);
			Assert.assertNull(selectedLayer);
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}

	@Test
	void testSelectPrimaryLayer() {
		try {
			var selectedLayer = polygon.findSpecificLayer(Vdyp7Constants.VDYP7_LAYER_ID_PRIMARY);
			Assert.assertEquals("13919428:1", selectedLayer.toString());
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}

	@Test
	void testSelectDeadLayer() {
		try {
			var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.DEAD);
			Assert.assertEquals("13919428:D", selectedLayer.toString());
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}

	@Test
	void testSelectResidualLayer() {
		try {
			var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.RESIDUAL);
			Assert.assertNull(selectedLayer);
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}

	@Test
	void testSelectRegenerationLayer() {
		try {
			var selectedLayer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.REGENERATION);
			Assert.assertEquals("13919428:2", selectedLayer.toString());
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}

	@Test
	void testSelectNamedLayer() {
		try {
			var selectedLayer = polygon.findSpecificLayer("1");
			Assert.assertNotNull(selectedLayer);
		} catch (PolygonValidationException e) {
			Assert.fail();
		}
	}
}
