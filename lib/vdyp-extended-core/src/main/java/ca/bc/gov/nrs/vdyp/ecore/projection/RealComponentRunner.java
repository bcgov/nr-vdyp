package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.NullProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.ProjectionResultsBuilder;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.ProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.RealProjectionResultsReader;
import ca.bc.gov.nrs.vdyp.ecore.utils.ErrorMessageUtils;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public class RealComponentRunner implements ComponentRunner {

	private final static Logger logger = LoggerFactory.getLogger(RealComponentRunner.class);

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try (var fipStartApplication = new FipStart()) {
			try {
				Path controlFilePath = Path.of(
						state.getExecutionFolder().toString(), projectionTypeCode.toString(),
						Vdyp7Constants.FIP_START_CONTROL_FILE_NAME
				);
				fipStartApplication.doMain(controlFilePath.toAbsolutePath().toString());
				state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
			} catch (Throwable t) {
				throwInterpretedException(fipStartApplication, polygon, projectionTypeCode, state, t);
			}
		}
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		try (var vriStartApplication = new VriStart()) {
			try {
				Path controlFilePath = Path.of(
						state.getExecutionFolder().toString(), projectionTypeCode.toString(),
						Vdyp7Constants.VRI_START_CONTROL_FILE_NAME
				);
				vriStartApplication.doMain(controlFilePath.toAbsolutePath().toString());
				state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
			} catch (Throwable t) {
				throwInterpretedException(vriStartApplication, polygon, projectionTypeCode, state, t);
			}
		}
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

		} catch (Exception e) {
			throw new PolygonExecutionException(
					MessageFormat.format(
							"{0}: encountered {1} while running copyAdjustInputFilesToOutput{2}", polygon,
							e.getClass().getSimpleName(), e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					), e
			);
		} catch (Error e) {
			throw new PolygonExecutionException(
					MessageFormat.format(
							"{0}: encountered {1} while running copyAdjustInputFilesToOutput{2}", polygon,
							e.getClass().getSimpleName(), e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					), e
			);
		}
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try (var forwardApplication = new VdypForwardApplication()) {
			try {
				Path controlFilePath = Path.of(
						state.getExecutionFolder().toString(), projectionTypeCode.toString(),
						Vdyp7Constants.FORWARD_CONTROL_FILE_NAME
				);

				Optional<Path> inputDir = Optional.empty();
				Optional<Path> outputDir = Optional.empty();
				forwardApplication.doMain(inputDir, outputDir, controlFilePath.toAbsolutePath().toString());

				state.setProcessingResults(ProjectionStageCode.Forward, projectionTypeCode, Optional.empty());
			} catch (Throwable t) {
				throwInterpretedException(forwardApplication, polygon, projectionTypeCode, state, t);
			}
		}
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
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running BACK", e);
		} catch (Error e) {
			throw new PolygonExecutionException("Encountered error while running BACK", e);
		}
	}

	@Override
	public void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException {
		ValidatedParameters params = context.getParams();

		var yieldTable = context.getYieldTable();
		boolean doGenerateDetailedTableHeader = true;

		if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)) {

			var projectionResults = getProjectionResults(polygon, ProjectionTypeCode.PRIMARY, state);

			if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
					|| params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

				yieldTable
						.generateYieldTableForPolygon(polygon, projectionResults, state, doGenerateDetailedTableHeader);
				doGenerateDetailedTableHeader = false;

				logger.debug("{}: generated polygon-level yield table", polygon);
			}

			if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
				yieldTable.generateCfsBiomassTableForPolygon(
						polygon, projectionResults, state, doGenerateDetailedTableHeader
				);
				doGenerateDetailedTableHeader = false;

				logger.debug("{}: generated polygon-level CFS biomass table", polygon);
			}

		} else if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)) {

			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {

				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {

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

					if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
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
				} else {
					polygon.addMessage(
							new PolygonMessage.Builder().layer(layer)
									.details(
											ReturnCode.SUCCESS, MessageSeverityCode.INFORMATION,
											PolygonMessageKind.LAYER_NOT_PROJECTED
									).build()
					);
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
					var forwardControlFileParser = new ForwardControlParser();
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
			throw new YieldTableGenerationException(e);
		}
	}

	private void throwInterpretedException(
			VdypApplication app, Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state,
			Throwable e
	) throws PolygonExecutionException {
		if (e instanceof VdypApplicationException && e.getCause() != null) {
			state.setProcessingResults(ProjectionStageCode.of(app), projectionTypeCode, Optional.of(e.getCause()));
			// VdypApplication exceptions are expected to be processed as a result returned from the VdypApplication
			// they came from do not rethrow
		} else {
			state.setProcessingResults(ProjectionStageCode.of(app), projectionTypeCode, Optional.of(e));
			var message = ErrorMessageUtils.BuildVDYPApplicationErrorMessage(app.getId(), polygon, "running", e);
			throw new PolygonExecutionException(message, e);
		}
	}

}
