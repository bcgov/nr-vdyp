package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractPolygonProjectionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.output.IMessageLog;

public class ProjectionRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionRunner.class);

	public static final String FORWARD_POLYGON_ENTRY_NAME = "ForwardPolygon";
	public static final String FORWARD_SPECIES_ENTRY_NAME = "ForwardSpecies";
	public static final String FORWARD_UTILIZATION_ENTRY_NAME = "ForwardUtilization";
	public static final String FORWARD_COMPATIBILITY_ENTRY_NAME = "ForwardCompatibility";
	public static final String BACK_POLYGON_ENTRY_NAME = "BackPolygon";
	public static final String BACK_SPECIES_ENTRY_NAME = "BackSpecies";
	public static final String BACK_UTILIZATION_ENTRY_NAME = "BackUtilization";

	private static final Map<String, String> entryNameToFileNameMap = new HashMap<>();

	static {
		entryNameToFileNameMap.put(FORWARD_POLYGON_ENTRY_NAME, "vp_grow.dat");
		entryNameToFileNameMap.put(FORWARD_SPECIES_ENTRY_NAME, "vs_grow.dat");
		entryNameToFileNameMap.put(FORWARD_UTILIZATION_ENTRY_NAME, "vu_grow.dat");
		entryNameToFileNameMap.put(FORWARD_COMPATIBILITY_ENTRY_NAME, "vc_grow.dat");
		entryNameToFileNameMap.put(BACK_POLYGON_ENTRY_NAME, "vp_back_grow.dat");
		entryNameToFileNameMap.put(BACK_SPECIES_ENTRY_NAME, "vs_back_grow.dat");
		entryNameToFileNameMap.put(BACK_UTILIZATION_ENTRY_NAME, "vu_back_grow.dat");
	}

	private final ProjectionContext context;

	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters, Boolean isTrialRun)
			throws AbstractProjectionRequestException {
		this.context = new ProjectionContext(kind, projectionId, parameters, isTrialRun);
	}

	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException,
			ProjectionInternalExecutionException, YieldTableGenerationException {

		context.startRun();

		logger.debug("{}", context.getValidatedParams().toString());
		logApplicationMetadata();

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, streams);

		IComponentRunner componentRunner;
		if (context.isTrialRun()) {
			componentRunner = new StubComponentRunner();
		} else {
			componentRunner = new ComponentRunner();
		}

		while (polygonStream.hasNextPolygon()) {

			try {
				try {
					var polygon = polygonStream.getNextPolygon();
					if (polygon.doAllowProjection()) {
						logger.info("Starting the projection of feature \"{}\"", polygon);
						PolygonProjectionRunner.of(polygon, context, componentRunner).project();
					} else {
						logger.info("By request, the projection of feature \"{}\" has been skipped", polygon);
					}
				} catch (PolygonExecutionException e) {
					IMessageLog errorLog = context.getErrorLog();
					for (ValidationMessage m : e.getValidationMessages()) {
						errorLog.addMessage(m.getKind().template, m.getArgs());
					}

					if (e.getCause() instanceof PolygonValidationException pve) {
						throw pve;
					}
				}
			} catch (PolygonValidationException e) {
				IMessageLog errorLog = context.getErrorLog();
				for (ValidationMessage m : e.getValidationMessages()) {
					errorLog.addMessage(m.getKind().template, m.getArgs());
				}
			}
		}
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	public InputStream getYieldTable() throws ProjectionInternalExecutionException {
		if (context.isTrialRun()) {
			return new ByteArrayInputStream(new byte[0]);
		} else {
			try {
				return context.getYieldTable().getAsStream();
			} catch (YieldTableGenerationException e) {
				throw new ProjectionInternalExecutionException(e);
			}
		}
	}

	public static record ProjectionResultsKey(String polygonId, String projectionType, String entryName) {
		@Override
		public String toString() {
			return polygonId + '-' + projectionType + '-' + entryName;
		}
	};

	public Map<ProjectionResultsKey, InputStream> getProjectionResults() {
		var projectionResults = new HashMap<ProjectionResultsKey, InputStream>();

		if (context.getValidatedParams().containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_FILES)) {

			try (var polygonFolders = Files.list(context.getExecutionFolder()).filter(e -> Files.isDirectory(e))) {
				for (var polygonFolder : polygonFolders.toList()) {

					try (var projectionTypeFolders = Files.list(polygonFolder).filter(e -> Files.isDirectory(e))) {
						for (var projectionTypeFolder : projectionTypeFolders.toList()) {

							for (var entry : entryNameToFileNameMap.entrySet()) {

								Path expectedFileName = Path.of(projectionTypeFolder.toString(), entry.getValue());
								if (Files.exists(expectedFileName) && Files.isRegularFile(expectedFileName)) {
									var polygonId = polygonFolder.getName(polygonFolder.getNameCount() - 1);
									var projectionType = projectionTypeFolder
											.getName(projectionTypeFolder.getNameCount() - 1);
									var key = new ProjectionResultsKey(
											polygonId.toString(), projectionType.toString(), entry.getKey()
									);

									projectionResults
											.put(key, Files.newInputStream(expectedFileName, StandardOpenOption.READ));
								}
							}
						}
					}
				}
			} catch (IOException e) {
				logger.warn("Unable to create Projection Results map", e);
			}
		}

		return projectionResults;
	}

	public InputStream getProgressStream() {
		return context.getProgressLog().getAsStream();
	}

	public InputStream getErrorStream() {
		return context.getErrorLog().getAsStream();
	}

	public ProjectionContext getContext() {
		return context;
	}

	/**
	 * Call this in a finally {} block after executing run() in the try {} block.
	 */
	public void endRun() {
		context.endRun();
	}
}
