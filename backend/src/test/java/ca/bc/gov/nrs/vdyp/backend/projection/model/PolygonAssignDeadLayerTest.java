package ca.bc.gov.nrs.vdyp.backend.projection.model;

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
import ca.bc.gov.nrs.vdyp.backend.utils.NullMath;

public class PolygonAssignDeadLayerTest {

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
		Assert.assertTrue(polygonStream.hasNextPolygon());

		polygon = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon);
	}

	@Test
	void testAssignDeadLayer() {
		var deadLayer = polygon.getDeadLayer();
		Assert.assertNotNull(deadLayer);

		var maxYearOfDeath = NullMath
				.max(deadLayer.getYearOfDeath(), polygon.getYearOfDeath(), (a, b) -> Math.max(a, b), -9);
		var maxPercentStockable = NullMath.max(
				deadLayer.getPercentStockable(), polygon.getPercentStockable(), (a, b) -> Math.max(a, b),
				Vdyp7Constants.EMPTY_DECIMAL
		);

		Assert.assertNotNull(maxYearOfDeath);
		Assert.assertNotNull(maxPercentStockable);

		polygon.assignDeadLayer(deadLayer, maxYearOfDeath + 1, maxPercentStockable + 1);

		Assert.assertEquals(deadLayer, polygon.getDeadLayer());
		Assert.assertEquals(Integer.valueOf(maxYearOfDeath + 1), deadLayer.getYearOfDeath());
		Assert.assertEquals(Double.valueOf(maxPercentStockable + 1), deadLayer.getPercentStockable());
	}
}
