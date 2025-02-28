package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class PolygonDisableProjectionsOfTypeTest {

	private Polygon polygon;

	@BeforeEach
	void beforeEach() throws IOException, PolygonValidationException, ProjectionRequestValidationException {
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
	void testDisablingProjectionsOfType() {
		Assert.assertTrue(polygon.doAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			Assert.assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}

		polygon.disableProjectionsOfType(ProjectionTypeCode.PRIMARY);
		Assert.assertTrue(polygon.doAllowProjection());
		Assert.assertFalse(polygon.doAllowProjectionOfType(ProjectionTypeCode.PRIMARY));
	}

	@Test
	void testDisablingProjection() {
		Assert.assertTrue(polygon.doAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			Assert.assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}

		polygon.disableProjectionsOfType(ProjectionTypeCode.UNKNOWN);
		Assert.assertFalse(polygon.doAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			Assert.assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}
	}
}
