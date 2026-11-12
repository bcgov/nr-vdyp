package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.back.io.parse.BackControlParser;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.ProjectionUtils;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;

class BackStandControlFileTest {

	@Test
	void testGenerateBackStandControlFileWritesTargetYear(@TempDir Path executionFolder) throws Exception {

		var path = PolygonProjectionRunner.generateBackStandControlFile(123456L, executionFolder, 1995);

		assertThat(path.getFileName().toString(), is(Vdyp7Constants.STAND_BACK_CONTROL_FILE_NAME));
		assertThat(Files.readString(path), containsString("101 1995   0   2   3   1   1"));
	}

	@Test
	void testGenerateBackStandControlFileFailsWhenFolderMissing(@TempDir Path tempFolder) {

		var missingFolder = tempFolder.resolve("missing");

		var ex = assertThrows(
				PolygonExecutionException.class,
				() -> PolygonProjectionRunner.generateBackStandControlFile(13919428L, missingFolder, 1995)
		);
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	/**
	 * Parses the VDYPBACK.CTR shipped in the execution folder template together with a generated per-stand file, the
	 * same way BACK does, and checks that BACK regresses to the requested year and writes its own output files.
	 */
	@Test
	void testTemplateBackControlFileParsesWithStandOverrides(@TempDir Path executionFolder) throws Exception {

		var projectionType = ProjectionTypeCode.PRIMARY.toString();
		try (var is = ProjectionUtils.class.getClassLoader().getResourceAsStream("ExecutionFolderTemplate.zip")) {
			ProjectionUtils.prepareProjectionTypeFolder(is, executionFolder, projectionType);
		}
		var projectionTypeFolder = executionFolder.resolve(projectionType);

		PolygonProjectionRunner.generateBackStandControlFile(123456L, projectionTypeFolder, 1995);

		var controlMap = new BackControlParser().parseByName(
				List.of(Vdyp7Constants.BACK_CONTROL_FILE_NAME, Vdyp7Constants.STAND_BACK_CONTROL_FILE_NAME),
				new FileSystemFileResolver(projectionTypeFolder), new HashMap<>()
		);

		var controlVariables = controlMap.get(ControlKey.VTROL.name());
		assertThat(controlVariables, instanceOf(ProcessingControlVariables.class));
		var targetYear = ((ProcessingControlVariables) controlVariables).getTargetYear();
		assertThat(targetYear.isRelative(), is(false));
		assertThat(targetYear.getValue(), is(1995));

		assertThat(controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name()).toString(), endsWith("vp_back.dat"));
		assertThat(
				controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name()).toString(), endsWith("vs_back.dat")
		);
		assertThat(
				controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name()).toString(),
				endsWith("vu_back.dat")
		);
	}
}
