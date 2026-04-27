package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;

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

		unit.setGrowthModel(ProjectionTypeCode.VETERAN, GrowthModelCode.FIP, ProcessingModeCode.FIP_FipStart);
		unit.setGrowthModel(ProjectionTypeCode.DEAD, GrowthModelCode.FIP, ProcessingModeCode.FIP_FipStart);

		assertThat(unit.getGrowthModel(ProjectionTypeCode.VETERAN), is(GrowthModelCode.FIP));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.VETERAN), is(ProcessingModeCode.FIP_FipStart));
		assertThat(unit.getGrowthModel(ProjectionTypeCode.DEAD), is(GrowthModelCode.FIP));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.DEAD), is(ProcessingModeCode.FIP_FipStart));

		unit.modifyAllProjectionTypeGrowthModels(GrowthModelCode.VRI, ProcessingModeCode.VRI_VriYoung);

		assertThat(unit.getGrowthModel(ProjectionTypeCode.VETERAN), is(GrowthModelCode.VRI));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.VETERAN), is(ProcessingModeCode.VRI_VriYoung));
		assertThat(unit.getGrowthModel(ProjectionTypeCode.DEAD), is(GrowthModelCode.VRI));
		assertThat(unit.getProcessingMode(ProjectionTypeCode.DEAD), is(ProcessingModeCode.VRI_VriYoung));

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

	public static Stream<Arguments> tranlationModeCode() {
		return Stream.of(
				Arguments.of(ProcessingModeCode.FIP_FipStart, PolygonMode.START, GrowthModelCode.FIP),
				Arguments.of(ProcessingModeCode.FIP_FipYoung, PolygonMode.YOUNG, GrowthModelCode.FIP),
				Arguments.of(ProcessingModeCode.VRI_VriStart, PolygonMode.START, GrowthModelCode.VRI),
				Arguments.of(ProcessingModeCode.VRI_VriYoung, PolygonMode.YOUNG, GrowthModelCode.VRI),
				Arguments.of(ProcessingModeCode.VRI_Minimal, PolygonMode.BATN, GrowthModelCode.VRI),
				Arguments.of(ProcessingModeCode.FIP_DoNotProcess, PolygonMode.DONT_PROCESS, GrowthModelCode.FIP),
				Arguments.of(ProcessingModeCode.VRI_DoNotProcess, PolygonMode.DONT_PROCESS, GrowthModelCode.VRI)
		);
	}

	@ParameterizedTest
	@MethodSource("tranlationModeCode")
	void testProcessingModeCodeTranslation(
			ProcessingModeCode mode, PolygonMode polygonMode, GrowthModelCode growthMode
	) {
		assertEquals(mode, ProcessingModeCode.translatePolygonMode(growthMode, polygonMode));
	}
}
