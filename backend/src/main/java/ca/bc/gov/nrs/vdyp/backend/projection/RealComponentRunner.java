package ca.bc.gov.nrs.vdyp.backend.projection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public class RealComponentRunner implements ComponentRunner {

	private static Logger logger = LoggerFactory.getLogger(RealComponentRunner.class);

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
			} catch (VdypApplicationException e) {
				throwInterpretedException(fipStartApplication, polygon, e);
			} catch (Exception e) {
				throwInterpretedException(fipStartApplication, polygon, e);
			} catch (Error e) {
				throwInterpretedException(fipStartApplication, polygon, e);
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
			} catch (VdypApplicationException e) {
				throwInterpretedException(vriStartApplication, polygon, e);
			} catch (Exception e) {
				throwInterpretedException(vriStartApplication, polygon, e);
			} catch (Error e) {
				throwInterpretedException(vriStartApplication, polygon, e);
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
			} catch (VdypApplicationInitializationException e) {
				throwInterpretedException(forwardApplication, polygon, e);
			} catch (Exception e) {
				throwInterpretedException(forwardApplication, polygon, e);
			} catch (Error e) {
				throwInterpretedException(forwardApplication, polygon, e);
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
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running BACK", e);
		} catch (Error e) {
			throw new PolygonExecutionException("Encountered error while running BACK", e);
		}
	}

	@Override
	public void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException {

		var yieldTable = context.getYieldTable();

		boolean doGenerateDetailedTableHeader = true;

		ValidatedParameters params = context.getParams();

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

							yieldTable.generateCfsBiomassTable(
									polygon, state, layerReportingInfo, doGenerateDetailedTableHeader
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

	private void throwInterpretedException(VdypApplication app, Polygon polygon, VdypApplicationException e)
			throws PolygonExecutionException {
		if (e.getCause() instanceof Exception pe) {
			throw new PolygonExecutionException(buildMessage(app, polygon, "running", pe), pe);
		} else {
			throw new PolygonExecutionException(buildMessage(app, polygon, "running", e), e);
		}
	}

	private void throwInterpretedException(VdypApplication app, Polygon polygon, Exception e)
			throws PolygonExecutionException {
		throw new PolygonExecutionException(buildMessage(app, polygon, "running", e), e);
	}

	private void throwInterpretedException(VdypApplication app, Polygon polygon, Error e)
			throws PolygonExecutionException {
		throw new PolygonExecutionException(buildMessage(app, polygon, "running", e), e);
	}

	private String buildMessage(VdypApplication app, Polygon polygon, String verb, Throwable e) {
		return MessageFormat.format(
				"Polygon {0}: encountered error in {1} when {2} polygon{3}", polygon, app.getId(), verb,
				serializeCauses(e)
		);
	}

	/**
	 * Return the reasons of the cause chain of <code>e</code>, making every effort to remove duplication as well as
	 * class names that prefix the individual exceptions.
	 *
	 * @param e the exception in question
	 * @return as described
	 */
	private String serializeCauses(Throwable e) {
		var messageList = new ArrayList<String>();
		return serializeCauses(new StringBuffer(), messageList, e).toString();
	}

	/**
	 * A regular expression that allows us to extract the message from a string possibly prefixed with a Java class name
	 * and possibly suffixed with a "." Group 3 applies if the message is suffixed with "." and group 4 otherwise. All
	 * messages -should- match this pattern, but if cases have been missed the method will simply take the whole
	 * message.
	 */
	private static final Pattern messagePattern = Pattern.compile("(^[a-zA-Z_0-9\\.]+: )?+((.+)\\.|(.*[^\\.]))$");

	private StringBuffer serializeCauses(StringBuffer s, List<String> messageList, Throwable e) {

		if (e.getMessage() != null) {
			var matcher = messagePattern.matcher(e.getMessage());

			String message;
			if (matcher.matches()) {
				if (matcher.group(3) != null) {
					message = matcher.group(3);
				} else {
					message = matcher.group(4);
				}
			} else {
				message = e.getMessage();
			}

			if (!StringUtils.isBlank(message) && !containsSuffix(messageList, message)) {
				s.append(": ").append(message);
				messageList.add(message);
			}
		}

		if (e.getCause() != null) {
			serializeCauses(s, messageList, e.getCause());
		}

		return s;
	}

	private boolean containsSuffix(List<String> messageList, String message) {
		for (var m : messageList) {
			if (m.endsWith(message)) {
				return true;
			}
		}
		return false;
	}
}
