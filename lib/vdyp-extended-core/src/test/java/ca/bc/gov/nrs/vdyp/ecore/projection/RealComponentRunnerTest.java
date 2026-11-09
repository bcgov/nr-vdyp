package ca.bc.gov.nrs.vdyp.ecore.projection;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
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
		long reportTitleCount = content.lines().filter(line -> line.contains("VDYP Yield Table")).count();
		assertThat(reportTitleCount, is(1L));
		assertThat(content, containsString("CFS Biomass"));
	}

	@ParameterizedTest
	@EnumSource(value = ProjectionTypeCode.class, names = { "DO_NOT_PROJECT", "UNKNOWN" })
	void testGenerateYieldTablesReportsUnselectedLayer(ProjectionTypeCode projectionType) throws Exception {
		var params = new Parameters().ageStart(180).ageEnd(181)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		var polygon = buildTestPolygon(context);
		var layerInfos = polygon.getReportingInfo().getLayerReportingInfos();
		var layer = layerInfos.values().iterator().next().getLayer();
		LayerReportingInfo layerInfo = EasyMock.createMock(LayerReportingInfo.class);
		EasyMock.expect(layerInfo.getSourceLayerID()).andStubReturn(1);
		EasyMock.expect(layerInfo.getLayer()).andStubReturn(layer);
		EasyMock.expect(layerInfo.getProcessedAsVDYP7Layer()).andStubReturn(projectionType);
		EasyMock.replay(layerInfo);
		layerInfos.clear();
		layerInfos.put(layer.getLayerId(), layerInfo);
		context.createYieldTables();
		int originalMessageCount = polygon.getMessages().size();

		// No execution folder: attempting to read projection results for this layer would fail.
		assertDoesNotThrow(() -> unit.generateYieldTables(context, polygon, new PolygonProjectionState()));

		var messages = polygon.getMessages().subList(originalMessageCount, polygon.getMessages().size());
		assertThat(messages.size(), is(1));
		var message = messages.get(0);
		assertThat(message.getKind(), is(PolygonMessageKind.NO_YIELD_TABLE_FOR_UNSELECTED_LAYER));
		assertThat(message.getSeverity(), is(MessageSeverityCode.INFORMATION));
		assertThat(message.getReturnCode(), is(ReturnCode.SUCCESS));
		assertThat(message.getLayer(), sameInstance(layer));
		assertThat(
				message.getFormattedMessage(),
				is("Layer '1' is not the Primary, Veteran or a Targeted Layer. Yield Table will not be produced.")
		);
		EasyMock.verify(layerInfo);
		for (var yieldTable : context.getYieldTables()) {
			yieldTable.close();
		}
	}

	@Nested
	class RunApp {

		@Test
		void testSuccess() throws Exception {
			var em = EasyMock.createControl();
			var runner = new RealComponentRunner();

			Polygon polygon = em.createMock(Polygon.class);
			ProjectionTypeCode projectionTypeCode = ProjectionTypeCode.PRIMARY;
			PolygonProjectionState state = em.createMock(PolygonProjectionState.class);
			VdypApplicationIdentifier appToUse = VdypApplicationIdentifier.FIP_START;
			List<String> controlFiles = List.of("Test.CTR");
			VdypApplication<?> app = em.createMock(VdypApplication.class);

			Supplier<VdypApplication<?>> getApp = () -> app;
			Consumer<VdypApplication<?>> after = em.createMock(Consumer.class);

			EasyMock.expect(state.getExecutionFolder()).andStubReturn(Path.of("TestPath"));

			// Run the main processing
			app.doMain(EasyMock.eq(Path.of("TestPath", "PRIMARY", "Test.CTR")));
			EasyMock.expectLastCall().once(); // Success

			// Handle success
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
			EasyMock.expectLastCall().once();

			// Do any postprocessing
			after.accept(app);
			EasyMock.expectLastCall().once();

			// it should clean up after itself
			app.close();
			EasyMock.expectLastCall().once();

			em.replay();

			runner.runApp(polygon, projectionTypeCode, state, appToUse, controlFiles, getApp, after);

			em.verify();
		}

		@Test
		void testInitializationException() throws Exception {
			var em = EasyMock.createControl();
			var runner = new RealComponentRunner();

			Polygon polygon = em.createMock(Polygon.class);
			ProjectionTypeCode projectionTypeCode = ProjectionTypeCode.PRIMARY;
			PolygonProjectionState state = em.createMock(PolygonProjectionState.class);
			VdypApplicationIdentifier appToUse = VdypApplicationIdentifier.FIP_START;
			List<String> controlFiles = List.of("Test.CTR");
			VdypApplication<?> app = em.createMock(VdypApplication.class);

			var rootCause = new RuntimeException("Test Exception");
			var appCause = new VdypApplicationInitializationException(rootCause);

			Supplier<VdypApplication<?>> getApp = () -> app;
			Consumer<VdypApplication<?>> after = em.createMock(Consumer.class);

			EasyMock.expect(state.getExecutionFolder()).andStubReturn(Path.of("TestPath"));

			// Run the main processing
			app.doMain(EasyMock.eq(Path.of("TestPath", "PRIMARY", "Test.CTR")));
			EasyMock.expectLastCall().andThrow(appCause).once();

			// Handle Failure
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.of(rootCause));
			EasyMock.expectLastCall().once();

			// it should clean up after itself
			app.close();
			EasyMock.expectLastCall().once();

			em.replay();

			runner.runApp(polygon, projectionTypeCode, state, appToUse, controlFiles, getApp, after);

			em.verify();
		}

		@Test
		void testProcessingException() throws Exception {
			var em = EasyMock.createControl();
			var runner = new RealComponentRunner();

			Polygon polygon = em.createMock(Polygon.class);
			ProjectionTypeCode projectionTypeCode = ProjectionTypeCode.PRIMARY;
			PolygonProjectionState state = em.createMock(PolygonProjectionState.class);
			VdypApplicationIdentifier appToUse = VdypApplicationIdentifier.FIP_START;
			List<String> controlFiles = List.of("Test.CTR");
			VdypApplication<?> app = em.createMock(VdypApplication.class);

			var rootCause = new RuntimeException("Test Exception");
			var appCause = new VdypApplicationProcessingException(rootCause);

			Supplier<VdypApplication<?>> getApp = () -> app;
			Consumer<VdypApplication<?>> after = em.createMock(Consumer.class);

			EasyMock.expect(state.getExecutionFolder()).andStubReturn(Path.of("TestPath"));

			// Run the main processing
			app.doMain(EasyMock.eq(Path.of("TestPath", "PRIMARY", "Test.CTR")));
			EasyMock.expectLastCall().andThrow(appCause).once();

			// Handle Failure
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.of(rootCause));
			EasyMock.expectLastCall().once();

			// it should clean up after itself
			app.close();
			EasyMock.expectLastCall().once();

			em.replay();

			runner.runApp(polygon, projectionTypeCode, state, appToUse, controlFiles, getApp, after);

			em.verify();
		}

		@Test
		void testRuntimeException() throws Exception {
			var em = EasyMock.createControl();
			var runner = new RealComponentRunner();

			Polygon polygon = em.createMock(Polygon.class);
			ProjectionTypeCode projectionTypeCode = ProjectionTypeCode.PRIMARY;
			PolygonProjectionState state = em.createMock(PolygonProjectionState.class);
			VdypApplicationIdentifier appToUse = VdypApplicationIdentifier.FIP_START;
			List<String> controlFiles = List.of("Test.CTR");
			VdypApplication<?> app = em.createMock(VdypApplication.class);

			var rootCause = new RuntimeException("Test Exception");

			Supplier<VdypApplication<?>> getApp = () -> app;
			Consumer<VdypApplication<?>> after = em.createMock(Consumer.class);

			EasyMock.expect(state.getExecutionFolder()).andStubReturn(Path.of("TestPath"));

			// Run the main processing
			app.doMain(EasyMock.eq(Path.of("TestPath", "PRIMARY", "Test.CTR")));
			EasyMock.expectLastCall().andThrow(rootCause).once();

			// Store exception result
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.of(rootCause));
			EasyMock.expectLastCall().once();

			// it should clean up after itself
			app.close();
			EasyMock.expectLastCall().once();

			polygon.addMessage(EasyMock.<Consumer<PolygonMessage.Builder>>anyObject());
			EasyMock.expectLastCall().once();

			em.replay();

			var ex = assertThrows(
					PolygonExecutionException.class,
					() -> runner.runApp(polygon, projectionTypeCode, state, appToUse, controlFiles, getApp, after)
			);

			assertThat(ex, causedBy(sameInstance(rootCause)));

			em.verify();
		}

		@Test
		void testExceptionWhileClosing() throws Exception {
			var em = EasyMock.createControl();
			var runner = new RealComponentRunner();

			Polygon polygon = em.createMock(Polygon.class);
			ProjectionTypeCode projectionTypeCode = ProjectionTypeCode.PRIMARY;
			PolygonProjectionState state = em.createMock(PolygonProjectionState.class);
			VdypApplicationIdentifier appToUse = VdypApplicationIdentifier.FIP_START;
			List<String> controlFiles = List.of("Test.CTR");
			VdypApplication<?> app = em.createMock(VdypApplication.class);

			var rootCause = new RuntimeException("Test Exception");

			Supplier<VdypApplication<?>> getApp = () -> app;
			Consumer<VdypApplication<?>> after = em.createMock(Consumer.class);

			EasyMock.expect(state.getExecutionFolder()).andStubReturn(Path.of("TestPath"));

			// Run the main processing
			app.doMain(EasyMock.eq(Path.of("TestPath", "PRIMARY", "Test.CTR")));
			EasyMock.expectLastCall().once();

			// Handle Success
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
			EasyMock.expectLastCall().once();

			// Do any postprocessing
			after.accept(app);
			EasyMock.expectLastCall().once();

			// Error while cleaning up
			app.close();
			EasyMock.expectLastCall().andThrow(rootCause).once();

			// Store exception result
			state.setProcessingResults(ProjectionStageCode.of(appToUse), projectionTypeCode, Optional.of(rootCause));
			EasyMock.expectLastCall().once();

			polygon.addMessage(EasyMock.<Consumer<PolygonMessage.Builder>>anyObject());
			EasyMock.expectLastCall().once();

			em.replay();

			var ex = assertThrows(
					PolygonExecutionException.class,
					() -> runner.runApp(polygon, projectionTypeCode, state, appToUse, controlFiles, getApp, after)
			);

			assertThat(ex, causedBy(sameInstance(rootCause)));

			em.verify();
		}
	}
}
