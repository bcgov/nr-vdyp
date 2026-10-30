package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.opencsv.CSVReader;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

class ITProjectionRunner {

	private enum Failure {
		NONE, YIELD_TABLE, UNEXPECTED
	}

	@ParameterizedTest
	@EnumSource(Failure.class)
	void vdyp1453YieldTableFailurePreservesOtherPolygonsInChunk(Failure failure) throws Exception {
		var parameters = new Parameters().yearStart(2013).yearEnd(2014)
				.outputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_ERROR_LOGGING)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				.addExcludedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);

		try (
				var runner = new ProjectionRunner(ProjectionRequestKind.HCSV, "VDYP-1453", parameters, false);
				InputStream polygons = getClass().getResourceAsStream("/test-data/vdyp-1453/polygons.csv");
				InputStream layers = getClass().getResourceAsStream("/test-data/vdyp-1453/layers.csv")
		) {
			var streams = Map.of(
					ParameterNames.HCSV_POLYGON_INPUT_DATA, polygons, ParameterNames.HCSV_LAYERS_INPUT_DATA, layers
			);
			if (failure == Failure.NONE) {
				runner.run(streams);
			} else {
				// Keep real projection and output for the neighbours; fail only this polygon's yield generation.
				runner.run(streams, new RealComponentRunner() {
					@Override
					public void generateYieldTables(
							ProjectionContext context, Polygon polygon, PolygonProjectionState state
					) throws YieldTableGenerationException {
						if (polygon.getFeatureId() == 23116780L) {
							if (failure == Failure.YIELD_TABLE) {
								throw new YieldTableGenerationException(
										polygon.getFeatureId(), "VDYP-1453 regression failure"
								);
							}
							throw new IllegalStateException("VDYP-1453 regression failure");
						}
						super.generateYieldTables(context, polygon, state);
					}
				});
			}

			String errors;
			try (var errorStream = runner.getErrorStream()) {
				errors = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
			}
			assertEquals(
					failure == Failure.NONE ? 0L : 1L,
					errors.lines().filter(
							line -> line.contains("Polygon 23116780:") && line.contains("VDYP-1453 regression failure")
					).count(), errors
			);
			assertEquals(3, runner.getLastRunPolygonsSeen());
			assertEquals(3, runner.getLastRunPolygonsProcessed());
			assertEquals(0, runner.getLastRunPolygonsSkipped());

			try (
					var yieldStream = runner.getContext().getYieldTables().get(0).getAsStream();
					var csv = new CSVReader(new InputStreamReader(yieldStream, StandardCharsets.UTF_8))
			) {
				var header = List.of(csv.readNext());
				int featureIdColumn = header.indexOf("FEATURE_ID");
				assertTrue(featureIdColumn >= 0, header.toString());
				var rowsByPolygon = csv.readAll().stream()
						.collect(Collectors.groupingBy(row -> row[featureIdColumn], Collectors.counting()));
				var expectedRows = failure == Failure.NONE ? Map.of("13919428", 2L, "23116780", 2L, "23116781", 2L)
						: Map.of("13919428", 2L, "23116781", 2L);
				assertEquals(expectedRows, rowsByPolygon, errors);
			}
		}
	}
}
