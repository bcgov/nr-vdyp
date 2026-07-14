package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.NullProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.ProjectionResultsBuilder;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.ProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.RealProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.ErrorMessageUtils;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public class RealComponentRunner implements ComponentRunner {

	private static final Logger logger = LoggerFactory.getLogger(RealComponentRunner.class);

	@FunctionalInterface
	private interface AppRunner<A extends VdypApplication<?>> {
		void accept(A app) throws VdypApplicationInitializationException, VdypApplicationProcessingException;
	}

	/**
	 * Creates a new app instance using the given constructor and runs it with the given runner, handling all exceptions
	 * appropriately.
	 *
	 * @param <A>                The application class
	 * @param polygon            Polygon to process
	 * @param projectionTypeCode Projection type
	 * @param state              Projection state to update
	 * @param appToUse           ID of the app being used
	 * @param getApp             Constructor for the app
	 * @param runApp             Use the app to process the polygon
	 * @throws PolygonExecutionException
	 */
	<A extends VdypApplication<?>> void runApp(
			Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state,
			VdypApplicationIdentifier appToUse, Supplier<A> getApp, AppRunner<A> runApp
	) throws PolygonExecutionException {
		try (A app = getApp.get()) {
			runApp.accept(app);
		} catch (Throwable t) {
			// Do we really want to catch and wrap Errors?
			if (t instanceof VdypApplicationException && t.getCause() != null) {
				state.setProcessingResults(
						ProjectionStageCode.of(appToUse), projectionTypeCode, Optional.of(t.getCause())
				);
				// VdypApplication exceptions are expected to be processed as a result returned from the VdypApplication
				// they came from do not rethrow
			} else {
				state.setProcessingResults(ProjectionStageCode.of(appToUse), projectionTypeCode, Optional.of(t));
				var message = ErrorMessageUtils.BuildVDYPApplicationErrorMessage(appToUse, polygon, "running", t);
				throw new PolygonExecutionException(message, t);
			}
		}
	}

	/**
	 * Creates a new app instance using the given constructor and runs it with the given control files, handling all
	 * exceptions appropriately. After running it runs the given consumer and passes the app instance to it.
	 *
	 * @param <A>                The application class
	 * @param polygon            Polygon to process
	 * @param projectionTypeCode Projection type
	 * @param state              Projection state to update
	 * @param appToUse           ID of the app being used
	 * @param controlFiles       List of control file ids
	 * @param getApp             Constructor for the app
	 * @param after              Actions to perform after running the app
	 * @throws PolygonExecutionException
	 */
	<A extends VdypApplication<?>> void runApp(
			Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state,
			VdypApplicationIdentifier appToUse, List<String> controlFiles, Supplier<A> getApp, Consumer<A> after
	) throws PolygonExecutionException {
		ProjectionStageCode stageCode = switch (appToUse) {
		case FIP_START -> ProjectionStageCode.Initial;
		case VDYP_BACK -> ProjectionStageCode.Back;
		case VDYP_FORWARD -> ProjectionStageCode.Forward;
		case VRI_ADJUST -> ProjectionStageCode.Adjust;
		case VRI_START -> ProjectionStageCode.Initial;
		default -> throw new UnsupportedOperationException();
		};
		runApp(polygon, projectionTypeCode, state, appToUse, getApp, app -> {
			var controFilePaths = controlFiles.stream().map(
					filename -> Path.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), filename)
			).toArray(Path[]::new);
			app.doMain(controFilePaths);
			state.setProcessingResults(stageCode, projectionTypeCode, Optional.empty());
			after.accept(app);
		});
	}

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		runApp(
				polygon, projectionTypeCode, state, VdypApplicationIdentifier.FIP_START, //
				List.of(Vdyp7Constants.FIP_START_CONTROL_FILE_NAME), FipStart::new, //
				app -> {
					state.modifyGrowthModel(
							projectionTypeCode, GrowthModelCode.FIP,
							ProcessingModeCode.translatePolygonMode(GrowthModelCode.FIP, app.getModeUsed())
					);
				}
		);
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		runApp(
				polygon, projectionTypeCode, state, VdypApplicationIdentifier.VRI_START, //
				List.of(Vdyp7Constants.VRI_START_CONTROL_FILE_NAME), VriStart::new, //
				app -> {
					state.modifyGrowthModel(
							projectionTypeCode, GrowthModelCode.VRI,
							ProcessingModeCode.translatePolygonMode(GrowthModelCode.VRI, app.getModeUsed())
					);
				}
		);
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionType, PolygonProjectionState state)
			throws PolygonExecutionException {

		// ADJUST is currently not being run; we just copy the input to output.

		logger.info("{} {}: ADJUST is operating as a pass-through", polygon, projectionType);

		copyAdjustInputFilesToOutput(polygon, state, projectionType, state.getExecutionFolder());

		state.setProcessingResults(ProjectionStageCode.Adjust, projectionType, Optional.empty());
	}

	private void copyAdjustInputFilesToOutput(
			Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType, Path rootExecutionFolder
	) throws PolygonExecutionException {

		Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

		try {
			Path polygonInputFile = Path.of(executionFolder.toString(), "vp_01.dat");
			Path polygonOutputFile = Path.of(executionFolder.toString(), "vp_adj.dat");
			Files.copy(polygonInputFile, polygonOutputFile);

			Path speciesInputFile = Path.of(executionFolder.toString(), "vs_01.dat");
			Path speciesOutputFile = Path.of(executionFolder.toString(), "vs_adj.dat");
			Files.copy(speciesInputFile, speciesOutputFile);

			Path utilizationsInputFile = Path.of(executionFolder.toString(), "vu_01.dat");
			Path utilizationsOutputFile = Path.of(executionFolder.toString(), "vu_adj.dat");
			Files.copy(utilizationsInputFile, utilizationsOutputFile);

		} catch (Exception | Error e) {
			throw new PolygonExecutionException(
					polygon.getFeatureId(),
					MessageFormat.format(
							"encountered {0} while running copyAdjustInputFilesToOutput{1}",
							e.getClass().getSimpleName(), e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					), e
			);
		}
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		runApp(
				polygon, projectionTypeCode, state, VdypApplicationIdentifier.VDYP_FORWARD, //
				List.of(Vdyp7Constants.FORWARD_CONTROL_FILE_NAME, Vdyp7Constants.STAND_FORWARD_CONTROL_FILE_NAME),
				VdypForwardApplication::new, app -> {
					// Nothing else to do
				}
		);
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			// TODO: BACK is not supported yet.

			@SuppressWarnings("unused")
			Path controlFilePath = Path.of(
					state.getExecutionFolder().toString(), projectionTypeCode.toString(),
					Vdyp7Constants.BACK_CONTROL_FILE_NAME
			);

			// VdypBackApplication app = new VdypBackApplication();
			// app.doMain(controlFilePath.toAbsolutePath().toString());

			state.setProcessingResults(ProjectionStageCode.Back, projectionTypeCode, Optional.empty());
		} catch (Exception | Error e) {
			throw new PolygonExecutionException(
					polygon.getFeatureId(),
					MessageFormat.format("Encountered {0} while running BACK", e.getClass().getSimpleName()), e
			);
		}
	}

	@Override
	public void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException {
		ValidatedParameters params = context.getParams();

		// When CSV or text report output includes both Volume and CFS Biomass, a single combined table is
		// generated by generateYieldTableForPolygon/Layer; skip the separate CFS-only table.
		boolean isCombinedBoth = (params.getOutputFormat() == OutputFormat.CSV_YIELD_TABLE
				|| params.getOutputFormat() == OutputFormat.TEXT_REPORT)
				&& params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				&& params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS);

		var tables = context.getYieldTables();
		for (YieldTable yieldTable : tables) {
			boolean doGenerateDetailedTableHeader = true;

			if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)) {

				var projectionResults = getProjectionResults(polygon, ProjectionTypeCode.PRIMARY, state);

				if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
						|| params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

					yieldTable.generateYieldTableForPolygon(
							polygon, projectionResults, state, doGenerateDetailedTableHeader
					);
					doGenerateDetailedTableHeader = false;

					logger.debug("{}: generated polygon-level yield table", polygon);
				}

				if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS) && !isCombinedBoth) {
					yieldTable.generateCfsBiomassTableForPolygon(
							polygon, projectionResults, state, doGenerateDetailedTableHeader
					);
					doGenerateDetailedTableHeader = false;

					logger.debug("{}: generated polygon-level CFS biomass table", polygon);
				}

			}

			if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)) {

				var unsortedLayerInfos = polygon.getReportingInfo().getLayerReportingInfos().values();
				// Try to line the ordering up with VDYP7 to make comparison/debugging easier.
				// Shouldn't slow things down too much but we can probably have an option to disable it to speed things
				// up.
				var sortedLayerInfos = ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST.stream()
						.map(
								type -> unsortedLayerInfos.stream().filter(li -> li.getProcessedAsVDYP7Layer() == type)
										.findFirst()
						).filter(Optional::isPresent).map(Optional::get).toList();

				for (var layerReportingInfo : sortedLayerInfos) {

					var layer = layerReportingInfo.getLayer();

					doGenerateDetailedTableHeader = true;

					var projectionResults = getProjectionResults(
							polygon, layerReportingInfo.getProcessedAsVDYP7Layer(), state
					);
					if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
							|| params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

						yieldTable.generateYieldTableForPolygonLayer(
								polygon, projectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader
						);
						doGenerateDetailedTableHeader = false;

						logger.debug("{}: generated yield table", layerReportingInfo.getLayer());
					}

					if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS) && !isCombinedBoth) {
						if (!layerReportingInfo.isDeadStemLayer()) {

							yieldTable.generateCfsBiomassTableForPolygonLayer(
									polygon, projectionResults, state, layerReportingInfo, doGenerateDetailedTableHeader
							);
							doGenerateDetailedTableHeader = false;

							logger.debug("{}: generated CFS biomass table", layer);
						} else {
							polygon.addMessage(
									new PolygonMessage.Builder().layer(layer)
											.details(
													ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
													PolygonMessageKind.NO_YIELD_TABLE_FOR_DEAD_LAYER
											).build()
							);
						}
					}
				}
			}
		}
	}

	private Map<Integer, VdypPolygon>
			getProjectionResults(Polygon polygon, ProjectionTypeCode projectionType, PolygonProjectionState state)
					throws YieldTableGenerationException {

		Path stepExecutionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());
		var vdypControlFileResolver = new FileSystemFileResolver(stepExecutionFolder);

		try {
			ProjectionResultsReader forwardReader = new NullProjectionResultsReader();

			if (state.didRunProjectionStage(ProjectionStageCode.Forward, projectionType)) {

				try (var fis = vdypControlFileResolver.resolveForInput(Vdyp7Constants.FORWARD_CONTROL_FILE_NAME)) {
					var forwardControlFileParser = new ProcessingControlParser();
					Map<String, Object> forwardControlMap = forwardControlFileParser
							.parse(fis, vdypControlFileResolver, new HashMap<>());
					forwardReader = new RealProjectionResultsReader(forwardControlMap);
				}
			}

			ProjectionResultsReader backReader = new NullProjectionResultsReader();

			if (state.didRunProjectionStage(ProjectionStageCode.Back, projectionType)) {

				try (var bis = vdypControlFileResolver.resolveForInput(Vdyp7Constants.BACK_CONTROL_FILE_NAME)) {
					// FIXME See VDYP-157
					// var backwardsControlFileParser = new ForwardControlParser();
					// Map<String, Object> backwardsControlMap = backwardsControlFileParser
					// .parse(bis, vdypControlFileResolver, new HashMap<>());
					// backReader = new RealProjectionResultsReader(backwardsControlMap);
				}
			}

			var projectionResults = ProjectionResultsBuilder
					.read(polygon, state, projectionType, forwardReader, backReader);

			return projectionResults;
		} catch (ResourceParseException | IOException e) {
			throw new YieldTableGenerationException(polygon.getFeatureId(), e);
		}
	}

}
