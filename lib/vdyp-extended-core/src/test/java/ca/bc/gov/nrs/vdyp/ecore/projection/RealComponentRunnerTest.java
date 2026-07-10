package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

class RealComponentRunnerTest {

	private final RealComponentRunner unit = new RealComponentRunner();

	@Test
	void testRunAdjustFailsWhenInputFilesAreMissing() throws Exception {

		var polygon = new Polygon.Builder().featureId(13919428).build();
		var state = new PolygonProjectionState();
		state.setExecutionFolder(Files.createTempDirectory("real-component-runner-test"));

		var ex = assertThrows(
				PolygonExecutionException.class, () -> unit.runAdjust(polygon, ProjectionTypeCode.PRIMARY, state)
		);
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	@Test
	void testRunBackFailsWhenExecutionFolderNotSet() {

		var polygon = new Polygon.Builder().featureId(13919428).build();
		var state = new PolygonProjectionState();

		var ex = assertThrows(
				PolygonExecutionException.class, () -> unit.runBack(polygon, ProjectionTypeCode.PRIMARY, state)
		);
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	@Test
	void testGenerateYieldTablesFailsWhenForwardControlFileIsMissing() throws Exception {

		var polygon = new Polygon.Builder().featureId(13919428).build();

		var params = new Parameters().ageStart(0).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		context.createYieldTables();

		var state = new PolygonProjectionState();
		state.setExecutionFolder(context.getExecutionFolder());
		state.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());

		var ex = assertThrows(
				YieldTableGenerationException.class, () -> unit.generateYieldTables(context, polygon, state)
		);
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}
}
