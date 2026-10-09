package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.History;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

class FullReportYieldTableWriterTest {
	@ParameterizedTest
	@CsvSource({ "FIP, true", "VRI, false" })
	void testDefaultCrownClosureNote(InventoryStandard inventory, boolean expected) throws Exception {
		var polygon = new Polygon.Builder().featureId(1).inventoryStandard(inventory).percentStockable(100.0)
				.referenceYear(2024).becZone("MS").history(new History.Builder().build())
				.reportingInfo(new PolygonReportingInfo.Builder().build()).build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).percentStockable(100.0)
				.doSuppressPerHAYields(false).build();
		polygon.setPrimaryLayer(layer);
		var stand = new Stand.Builder().layer(layer).sp0Code("PL").build();
		stand.addSpeciesGroup(new Species.Builder().stand(stand).speciesCode("PL").speciesPercent(0).build(), 0);
		layer.addStand(stand);
		var species = new Species.Builder().stand(stand).speciesCode("PLI").speciesPercent(100.0).dominantHeight(20.0)
				.totalAge(80.0).build();
		stand.addSp64(species);
		layer.addSp64(species);
		polygon.getLayers().put("1", layer);
		var context = reportContext();
		polygon.setTargetedPrimaryLayer(layer);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		layer.estimateCrownClosure(context);
		assertThat(layer.getSuppliedCrownClosure(), is(nullValue()));
		assertTrue(layer.getCrownClosure() > 0);
		var output = writeTrailer(context, polygon);
		var note = "NOTE: Basal Area and Trees per HA computed using Default CC of " + layer.getCrownClosure() + "%";
		assertThat(output, expected ? containsString(note) : not(containsString(note)));
		assertThat(output, containsString("% Crown Closure Supplied. " + (expected ? "<None>" : "<Not Used>")));
	}

	@ParameterizedTest
	@CsvSource({ "false, 0, NONE", "true, 0, NO_YIELDS", "false, 15, FIRST_AGE", "true, 15, FIRST_AGE" })
	void testYieldAvailabilityNotes(boolean missingYields, int firstAge, String expected) throws Exception {
		var polygon = new Polygon.Builder().featureId(1).inventoryStandard(InventoryStandard.FIP)
				.percentStockable(100.0).build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).percentStockable(100.0).crownClosure((short) 40)
				.build();
		polygon.setPrimaryLayer(layer);
		layer.setAgeRequestedWithoutYields(missingYields);
		layer.setFirstAgeWithYields(firstAge);
		var output = writeTrailer(reportContext(), polygon);
		String noYields = "NOTE: No yields produced. Sufficient height was not achieved.";
		String firstYield = "NOTE: Yields are not predicted prior to age 15";
		assertThat(output, expected.equals("NO_YIELDS") ? containsString(noYields) : not(containsString(noYields)));
		assertThat(output, expected.equals("FIRST_AGE") ? containsString(firstYield) : not(containsString(firstYield)));
		assertThat(output, not(containsString("computed using Default CC")));
		assertThat(output, containsString("% Crown Closure Supplied. 40"));
	}

	@ParameterizedTest
	@CsvSource({ "-1, <None>", "0, 0", "40, 40" })
	void testSuppliedCrownClosureMetadata(short supplied, String text) throws Exception {
		var polygon = new Polygon.Builder().featureId(1).inventoryStandard(InventoryStandard.FIP)
				.percentStockable(100.0).build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).percentStockable(100.0).crownClosure(supplied)
				.build();
		polygon.setPrimaryLayer(layer);
		var output = writeTrailer(reportContext(), polygon);
		assertThat(output, containsString("% Crown Closure Supplied. " + text));
		assertThat(output, not(containsString("computed using Default CC")));
	}

	private ProjectionContext reportContext() throws Exception {
		return new ProjectionContext(
				ProjectionRequestKind.HCSV, "TEST",
				new Parameters().outputFormat(Parameters.OutputFormat.YIELD_TABLE).ageStart(0).ageEnd(100), false
		);
	}

	private String writeTrailer(ProjectionContext context, Polygon polygon) throws Exception {
		try (var writer = FullReportYieldTableWriter.of(context)) {
			writer.lastPolygonForTrailer = polygon;
			writer.recordPolygonProjectionState(new PolygonProjectionState());
			writer.writeTrailer();
		}
		return Files.readString(context.getExecutionFolder().resolve(FullReportYieldTableWriter.YIELD_TABLE_FILE_NAME));
	}

	@Test
	void testWrappedSpeciesHeaderPreservesEverySpecies() throws Exception {
		assertThat(
				writeSpeciesHeader(List.of("FDC", "EA", "EP", "PY"), List.of(50.0, 20.0, 15.0, 15.0)),
				is(
						List.of(
								"          Douglas Fir (Coastal) (50.0%), Common Paper Birch (20.0%), ",
								"                Silver Paper Birch (15.0%), Yellow Pine (15.0%)"
						)
				)
		);
	}

	@Test
	void testSpeciesHeaderFitsOnOneLine() throws Exception {
		assertThat(
				writeSpeciesHeader(List.of("EA", "EP", "PY"), List.of(70.0, 15.0, 15.0)),
				is(List.of("  Common Paper Birch (70.0%), Silver Paper Birch (15.0%), Yellow Pine (15.0%)"))
		);
	}

	private List<String> writeSpeciesHeader(List<String> codes, List<Double> percents) throws Exception {
		var polygon = new Polygon.Builder().featureId(13919428).build();
		var layer = new Layer.Builder().layerId("1").polygon(polygon).build();
		polygon.setPrimaryLayer(layer);
		for (int i = 0; i < codes.size(); i++) {
			var stand = new Stand.Builder().layer(layer).sp0Code(codes.get(i)).build();
			layer.addSp64(
					new Species.Builder().stand(stand).speciesCode(codes.get(i)).speciesPercent(percents.get(i)).build()
			);
		}
		var params = new Parameters().outputFormat(Parameters.OutputFormat.YIELD_TABLE)
				.reportTitle("Species header test").ageStart(0).ageEnd(100);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		try (var writer = FullReportYieldTableWriter.of(context)) {
			writer.writePolygonTableHeader(polygon, Optional.empty(), true, 1);
		}
		return Files.readString(context.getExecutionFolder().resolve(FullReportYieldTableWriter.YIELD_TABLE_FILE_NAME))
				.lines().dropWhile(line -> !line.contains("VDYP Yield Table")).skip(1)
				.takeWhile(line -> !line.isBlank()).toList();
	}

	@Test
	void testBuildActualStockableAreaUsedNoteWhenNotSupplied() {

		var polygon = new Polygon.Builder().featureId(1).percentStockable(84.0).build();
		var layer = new Layer.Builder().polygon(polygon).layerId("1").percentStockable(0.0).build();
		polygon.setPrimaryLayer(layer);

		var note = FullReportYieldTableWriter.buildActualStockableAreaUsedNote(polygon);

		assertThat(note, is("NOTE: Actual Percent Stockable Area Used : 84%"));
	}

	@Test
	void testBuildActualStockableAreaUsedNoteWhenSupplied() {

		var polygon = new Polygon.Builder().featureId(1).percentStockable(90.0).build();
		var layer = new Layer.Builder().polygon(polygon).layerId("1").percentStockable(90.0).build();
		polygon.setPrimaryLayer(layer);

		var note = FullReportYieldTableWriter.buildActualStockableAreaUsedNote(polygon);

		assertThat(note, is(nullValue()));
	}

	@Test
	void testWriteRecordFailsWhenRowContextIsIncomplete() throws Exception {

		var polygon = new Polygon.Builder().featureId(13919428).build();
		var params = new Parameters().outputFormat(Parameters.OutputFormat.YIELD_TABLE).ageStart(0).ageEnd(100);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);

		var writer = FullReportYieldTableWriter.of(context);
		var rowContext = YieldTableRowContext.of(context, polygon, new PolygonProjectionState(), null);

		writer.startNewRecord();

		var ex = assertThrows(YieldTableGenerationException.class, () -> writer.writeRecord(rowContext));
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	@Test
	void testToYieldTableGenerationExceptionIncludesFeatureIdWhenPolygonKnown() {

		var polygon = new Polygon.Builder().featureId(13919428).build();

		var ex = FullReportYieldTableWriter.toYieldTableGenerationException(polygon, new IOException("boom"));

		assertThat(ex.getMessage(), is("Polygon 13919428: boom"));
	}

	@Test
	void testToYieldTableGenerationExceptionOmitsFeatureIdWhenPolygonUnknown() {

		var ex = FullReportYieldTableWriter.toYieldTableGenerationException(null, new IOException("boom"));

		assertThat(ex.getMessage(), is("java.io.IOException: boom"));
	}
}
