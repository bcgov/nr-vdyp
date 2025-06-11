package ca.bc.gov.nrs.vdyp.backend.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class PolygonProjectionStateTest {
	PolygonProjectionState unit;

	@BeforeEach
	void setup() {
		unit = new PolygonProjectionState();
	}

	@Test
	void testGrowthModel() {
		// These commented out casses cannot currently be hit because they are null checked and
		// They default to unknown unclear if the logic should be removed or updated to check for unknown
		/*
		 * assertThrows( IllegalStateException.class, () -> unit.modifyGrowthModel( ProjectionTypeCode.PRIMARY,
		 * GrowthModelCode.FIP, ProcessingModeCode.FIP_Default ) );
		 */

		// assertThrows(IllegalStateException.class, () -> unit.getGrowthModel(ProjectionTypeCode.PRIMARY));
		// assertThrows(IllegalStateException.class, () -> unit.getProcessingMode(ProjectionTypeCode.PRIMARY));

		unit.setGrowthModel(ProjectionTypeCode.PRIMARY, GrowthModelCode.FIP, ProcessingModeCode.FIP_FipStart);

		assertThrows(
				IllegalStateException.class,
				() -> unit
						.setGrowthModel(ProjectionTypeCode.PRIMARY, GrowthModelCode.FIP, ProcessingModeCode.FIP_Default)
		);

		assertThat(unit.getGrowthModel(ProjectionTypeCode.PRIMARY), is(GrowthModelCode.FIP));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.PRIMARY), is(ProcessingModeCode.FIP_FipStart));

		unit.modifyGrowthModel(ProjectionTypeCode.PRIMARY, GrowthModelCode.VRI, ProcessingModeCode.VRI_VriStart);

		assertThat(unit.getGrowthModel(ProjectionTypeCode.PRIMARY), is(GrowthModelCode.VRI));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.PRIMARY), is(ProcessingModeCode.VRI_VriStart));
	}

	@Test
	void testProcessingResults() {
		assertThrows(
				IllegalStateException.class,
				() -> unit.getProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY)
		);
		unit.setProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY, Optional.empty());
		assertThrows(
				IllegalStateException.class,
				() -> unit.setProcessingResults(
						ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY,
						Optional.of(new PolygonExecutionException("Test Fail"))
				)
		);
		assertThat(
				unit.getProcessingResults(ProjectionStageCode.Forward, ProjectionTypeCode.PRIMARY), is(Optional.empty())
		);
	}

	@Test
	void testProjectionRange() {
		assertThrows(
				IllegalStateException.class, () -> unit.updateProjectionRange(ProjectionTypeCode.PRIMARY, 0.0, 42.0)
		);
		assertThrows(IllegalStateException.class, () -> unit.getStartAge(ProjectionTypeCode.PRIMARY));
		assertThrows(IllegalStateException.class, () -> unit.getEndAge(ProjectionTypeCode.PRIMARY));

		unit.setProjectionRange(0.0, 42.0);

		assertThat(unit.getStartAge(ProjectionTypeCode.PRIMARY), is(0.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.PRIMARY), is(42.0));
		assertThat(unit.getStartAge(ProjectionTypeCode.VETERAN), is(0.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.VETERAN), is(42.0));

		unit.updateProjectionRange(ProjectionTypeCode.VETERAN, 10.0, 24.0);

		assertThat(unit.getStartAge(ProjectionTypeCode.PRIMARY), is(0.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.PRIMARY), is(42.0));
		assertThat(unit.getStartAge(ProjectionTypeCode.VETERAN), is(10.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.VETERAN), is(24.0));

		assertThrows(IllegalStateException.class, () -> unit.setProjectionRange(20.0, 100.0));

		assertThat(unit.getStartAge(ProjectionTypeCode.PRIMARY), is(0.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.PRIMARY), is(42.0));
		assertThat(unit.getStartAge(ProjectionTypeCode.VETERAN), is(10.0));
		assertThat(unit.getEndAge(ProjectionTypeCode.VETERAN), is(24.0));
	}

	@Test
	void testExecutionFolder() {
		assertThrows(IllegalStateException.class, () -> unit.getExecutionFolder());
		unit.setExecutionFolder(Path.of("Test", "Execution", "Path"));
		assertThrows(
				IllegalStateException.class, () -> unit.setExecutionFolder(Path.of("Different", "Execution", "Path"))
		);
		assertThat(unit.getExecutionFolder(), is(Path.of("Test", "Execution", "Path")));
	}
}
