package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

/**
 * Tests the fallback in {@link Polygon#getLayerByProjectionType(ProjectionTypeCode)} that allows a layer whose
 * per-hectare yields are suppressed to be processed as the Primary layer.
 */
class PolygonGetLayerByProjectionTypeTest {

	private final Polygon polygon = new Polygon.Builder().featureId(13919428).build();

	private Layer buildLayer(String layerId, Boolean doSuppressPerHAYields, ProjectionTypeCode assignedType) {
		return new Layer.Builder().layerId(layerId).polygon(polygon).doSuppressPerHAYields(doSuppressPerHAYields)
				.assignedProjectionType(assignedType).build();
	}

	@Test
	void testRegisteredLayerIsReturned() {
		var layer = buildLayer("1", false, ProjectionTypeCode.UNKNOWN);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), sameInstance(layer));
	}

	@Test
	void testSuppressedPrimaryLayerIsReturnedForPrimaryType() {
		var layer = buildLayer("1", true, ProjectionTypeCode.UNKNOWN);
		polygon.setPrimaryLayer(layer);

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), sameInstance(layer));
	}

	@Test
	void testSuppressedTargetedPrimaryLayerIsUsedWhenThereIsNoPrimaryLayer() {
		var layer = buildLayer("1", true, ProjectionTypeCode.UNKNOWN);
		polygon.setTargetedPrimaryLayer(layer);

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), sameInstance(layer));
	}

	@Test
	void testPrimaryLayerIsPreferredToTargetedPrimaryLayer() {
		var primaryLayer = buildLayer("1", true, ProjectionTypeCode.UNKNOWN);
		var targetedPrimaryLayer = buildLayer("2", true, ProjectionTypeCode.UNKNOWN);
		polygon.setPrimaryLayer(primaryLayer);
		polygon.setTargetedPrimaryLayer(targetedPrimaryLayer);

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), sameInstance(primaryLayer));
	}

	@Test
	void testNoLayerWhenThereIsNoPrimaryLayer() {
		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), nullValue());
	}

	@Test
	void testNoLayerWhenPrimaryLayerIsNotSuppressed() {
		polygon.setPrimaryLayer(buildLayer("1", false, ProjectionTypeCode.UNKNOWN));

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), nullValue());
	}

	@Test
	void testNoLayerWhenSuppressionIsNotSet() {
		polygon.setPrimaryLayer(buildLayer("1", null, ProjectionTypeCode.UNKNOWN));

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), nullValue());
	}

	@Test
	void testNoLayerWhenSuppressedPrimaryLayerHasAnAssignedType() {
		polygon.setPrimaryLayer(buildLayer("1", true, ProjectionTypeCode.VETERAN));

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.PRIMARY), nullValue());
	}

	@Test
	void testSuppressedPrimaryLayerIsNotReturnedForOtherTypes() {
		polygon.setPrimaryLayer(buildLayer("1", true, ProjectionTypeCode.UNKNOWN));

		assertThat(polygon.getLayerByProjectionType(ProjectionTypeCode.VETERAN), nullValue());
	}
}
