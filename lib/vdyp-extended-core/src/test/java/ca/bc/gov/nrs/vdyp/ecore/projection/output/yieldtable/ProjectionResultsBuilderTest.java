package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class ProjectionResultsBuilderTest {

	private final Polygon polygon = new Polygon.Builder().featureId(13919428).build();

	private static VdypPolygon result() {
		return EasyMock.createMock(VdypPolygon.class);
	}

	@Test
	void testForwardResultTakesPrecedenceForYearReportedByBoth() throws Exception {

		var back1995 = result();
		var back2011 = result();
		var forward2011 = result();
		var forward2021 = result();

		var state = new PolygonProjectionState();
		state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());
		state.setProcessingResults(ProjectionStageCode.Back, ProjectionTypeCode.PRIMARY, Optional.empty());

		var results = ProjectionResultsBuilder.read(
				polygon, state, ProjectionTypeCode.PRIMARY, p -> Map.of(2011, forward2011, 2021, forward2021),
				p -> Map.of(1995, back1995, 2011, back2011)
		);

		assertThat(results, aMapWithSize(3));
		assertThat(results.get(1995), sameInstance(back1995));
		assertThat(results.get(2011), sameInstance(forward2011));
		assertThat(results.get(2021), sameInstance(forward2021));
	}

	@Test
	void testBackResultsIgnoredWhenBackNotRun() throws Exception {

		var state = new PolygonProjectionState();

		var results = ProjectionResultsBuilder
				.read(polygon, state, ProjectionTypeCode.PRIMARY, p -> Map.of(), p -> Map.of(1995, result()));

		assertThat(results, anEmptyMap());
	}
}
