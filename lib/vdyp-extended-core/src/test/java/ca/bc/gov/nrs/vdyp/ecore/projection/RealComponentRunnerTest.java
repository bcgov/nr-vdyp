package ca.bc.gov.nrs.vdyp.ecore.projection;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

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

	private Polygon buildTestPolygon(ProjectionContext context) throws Exception {
		var polygonInputStream = TestUtils.makeInputStream(
				POLYGON_CSV_HEADER_LINE,
				"13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,NP,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,"
		);
		var layersInputStream = TestUtils.makeInputStream(
				LAYER_CSV_HEADER_LINE,
				"13919428,14321066,093C090,94833422,1,P,,1,,,,20,10.000010,300,PLI,60.00,SX,40.00,,,,,,,,,180,18.00,180,23.00,,,,,,,,"
		);
		return new HcsvPolygonStream(context, polygonInputStream, layersInputStream).getNextPolygon();
	}

	@Test
	void testGenerateYieldTablesSkipsSeparateCfsTableWhenCsvOutputCombinesBoth() throws Exception {

		var params = new Parameters().ageStart(180).ageEnd(181)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS);
		params.setYearStart(2013);
		params.setYearEnd(2014);
		params.setOutputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		var polygon = buildTestPolygon(context);

		var state = new PolygonProjectionState();
		state.setExecutionFolder(context.getExecutionFolder());

		context.createYieldTables();
		for (var yieldTable : context.getYieldTables()) {
			yieldTable.startGeneration();
		}

		assertDoesNotThrow(() -> unit.generateYieldTables(context, polygon, state));

		for (var yieldTable : context.getYieldTables()) {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(context.getYieldTables().get(0).getAsStream().readAllBytes());
		assertThat(content, containsString("PRJ_VOL_WS"));
		assertThat(content, containsString("PRJ_CFS_BIO_STEM"));
	}

	@Test
	void testGenerateYieldTablesCombinesVolumeAndBiomassForTextReportBoth() throws Exception {

		var params = new Parameters().ageStart(180).ageEnd(181)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS);
		params.setYearStart(2013);
		params.setYearEnd(2014);
		params.setOutputFormat(Parameters.OutputFormat.TEXT_REPORT);

		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		var polygon = buildTestPolygon(context);

		var state = new PolygonProjectionState();
		state.setExecutionFolder(context.getExecutionFolder());

		context.createYieldTables();
		for (var yieldTable : context.getYieldTables()) {
			yieldTable.startGeneration();
		}

		assertDoesNotThrow(() -> unit.generateYieldTables(context, polygon, state));

		for (var yieldTable : context.getYieldTables()) {
			yieldTable.endGeneration();
			yieldTable.close();
		}

		var content = new String(context.getYieldTables().get(0).getAsStream().readAllBytes());
		long reportTitleCount = content.lines().filter(line -> line.contains("VDYP Yield Table Report")).count();
		assertThat(reportTitleCount, is(1L));
		assertThat(content, containsString("CFS Biomass"));
	}
}
