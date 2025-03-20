package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public class ComponentRunner implements IComponentRunner {

	private static Logger logger = LoggerFactory.getLogger(ComponentRunner.class);

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try (var fipStartApplication = new FipStart()) {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "FIPSTART.CTR");
			fipStartApplication.doMain(controlFilePath.toAbsolutePath().toString());
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, 0, -99);
		} catch (VdypApplicationInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (VdypApplicationProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		try (var vriStartApplication = new VriStart()) {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VRISTART.CTR");
			vriStartApplication.doMain(controlFilePath.toAbsolutePath().toString());
			state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, 0, -99);
		} catch (VdypApplicationInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (VdypApplicationProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionType, PolygonProjectionState state)
			throws ProjectionInternalExecutionException {

		// ADJUST is currently not being run; we just copy the input to output.

		logger.info("{} {}: ADJUST is operating as a pass-through", polygon, projectionType);

		copyAdjustInputFilesToOutput(polygon, state, projectionType, state.getExecutionFolder());

		state.setProcessingResults(ProjectionStageCode.Adjust, projectionType, 0, -99);
	}

	private void copyAdjustInputFilesToOutput(
			Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType, Path rootExecutionFolder
	) throws ProjectionInternalExecutionException {

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

		} catch (IOException e) {
			throw new ProjectionInternalExecutionException(
					MessageFormat
							.format("{0}: encountered exception while running copyAdjustInputFilesToOutput", polygon),
					e
			);
		}
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VDYP.CTR");

			VdypForwardApplication
					.main(Optional.empty(), Optional.empty(), controlFilePath.toAbsolutePath().toString());
			state.setProcessingResults(ProjectionStageCode.Forward, projectionTypeCode, 0, -99);
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running ForwardApplication", e);
		}
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			// TODO: BACK is not supported yet.

			@SuppressWarnings("unused")
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VDYPBACK.CTR");

			// VdypBackApplication app = new VdypBackApplication();
			// app.doMain(controlFilePath.toAbsolutePath().toString());
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running BackApplication", e);
		}
	}

	@Override
	public void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException {

		var yieldTable = context.getYieldTable();

		boolean doGenerateDetailedTableHeader = true;

		ValidatedParameters params = context.getValidatedParams();

		if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				&& (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
						|| params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS))) {

			yieldTable.generateYieldTableForPolygon(polygon, state, doGenerateDetailedTableHeader);
			doGenerateDetailedTableHeader = false;

			logger.debug("{}: generated polygon-level yield table", polygon);
		}

		if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				&& params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {

			yieldTable.generateCfsBiomassTableForPolygon(polygon, state, doGenerateDetailedTableHeader);
			doGenerateDetailedTableHeader = false;

			logger.debug("{}: generated polygon-level CFS biomass table", polygon);
		}

		if (params.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)) {

			for (var layerReportingInfo : polygon.getReportingInfo().getLayerReportingInfos().values()) {

				var layer = layerReportingInfo.getLayer();
				if (state.layerWasProjected(layer)) {

					doGenerateDetailedTableHeader = true;

					if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
							|| params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

						yieldTable.generateYieldTableForPolygonLayer(
								polygon, state, layerReportingInfo, doGenerateDetailedTableHeader
						);
						doGenerateDetailedTableHeader = false;

						logger.debug("{}: generated yield table", layerReportingInfo.getLayer());
					}

					if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
						if (!layerReportingInfo.isDeadStemLayer()) {

							context.getYieldTable().generateCfsBiomassTable(
									polygon, state, layerReportingInfo, doGenerateDetailedTableHeader
							);
							doGenerateDetailedTableHeader = false;

							logger.debug("{}: generated CFS biomass table", layer);
						} else {
							context.addMessage(
									"{0}: Suppressing CFS biomass output for dead layer. The yield table will not be produced",
									layer
							);
						}
					}
				} else {
					context.addMessage(
							"{0}: both Forward and Back not executed. Yield Table will not be produced.", layer
					);
				}
			}
		}
	}
}
