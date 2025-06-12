package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

public class PolygonDisableProjectionsOfTypeTest {

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
	void testDisablingProjectionsOfType() {
		assertTrue(polygon.getDoAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}

		polygon.disableProjectionsOfType(ProjectionTypeCode.PRIMARY);
		assertTrue(polygon.getDoAllowProjection());
		assertFalse(polygon.doAllowProjectionOfType(ProjectionTypeCode.PRIMARY));
	}

	@Test
	void testDisablingProjection() {
		assertTrue(polygon.getDoAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}

		polygon.disableProjectionsOfType(ProjectionTypeCode.UNKNOWN);
		assertFalse(polygon.getDoAllowProjection());

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
			assertTrue(polygon.doAllowProjectionOfType(projectionType));
		}
	}
}
