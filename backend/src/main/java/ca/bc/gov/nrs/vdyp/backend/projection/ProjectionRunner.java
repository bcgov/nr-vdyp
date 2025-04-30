package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.IMessageLog;

public class ProjectionRunner implements Closeable {

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

	public void run(Map<String, InputStream> streams)
			throws ProjectionRequestValidationException, YieldTableGenerationException {

		context.startRun();

		try {
			logger.debug("{}", context.getParams().toString());
			logApplicationMetadata();

			AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, streams);

			ComponentRunner componentRunner;
			if (context.isTrialRun()) {
				componentRunner = new StubComponentRunner();
			} else {
				componentRunner = new RealComponentRunner();
			}

			Integer progressPeriod = context.getParams().getProgressFrequency().getIntValue();
			boolean reportProgressByPeriod = progressPeriod != null;

			Polygon polygon = null;
			String lastMapsheet = "";
			String lastMaintainer = "";

			int nPolygonsProcessed = 0;
			int nPolygonsSkipped = 0;

			while (polygonStream.hasNextPolygon()) {
				try {
					polygon = polygonStream.getNextPolygon();

					if (ProgressFrequency.MAPSHEET.equals(context.getParams().getProgressFrequency())
							&& !lastMapsheet.equals(polygon.getMapSheet())
							&& !lastMaintainer.equals(polygon.getDistrict())) {

						lastMapsheet = polygon.getMapSheet();
						lastMaintainer = polygon.getDistrict();

						String message = MessageFormat
								.format("Processing Map Sheet: \"{0}\", \"{1}\"...", lastMaintainer, lastMapsheet);

						context.getProgressLog().addMessage(message);
						logger.debug(message);
					}

					try {
						if (polygon.getDoAllowProjection()) {

							if (ProgressFrequency.POLYGON.equals(context.getParams().getProgressFrequency())) {

								String message = MessageFormat.format(
										"Processing Polygon {0,number,#}: \"{1}\", \"{2}\"-{3,number,#}",
										polygon.getFeatureId(), lastMaintainer, lastMapsheet, polygon.getPolygonNumber()
								);

								context.getProgressLog().addMessage(message);
								logger.debug(message);
							}

							nPolygonsProcessed += 1;
							PolygonProjectionRunner.of(polygon, context, componentRunner).project();

						} else {

							if (ProgressFrequency.POLYGON.equals(context.getParams().getProgressFrequency())) {

								String message = MessageFormat.format(
										"Skipping Polygon {0,number,#}: \"{1}\", \"{2}\"-{3,number,#}",
										polygon.getFeatureId(), lastMaintainer, lastMapsheet, polygon.getPolygonNumber()
								);

								context.getProgressLog().addMessage(message);
								logger.debug(message);
							}

							nPolygonsSkipped += 1;
						}

						int nPolygonsSeen = nPolygonsProcessed + nPolygonsSkipped;
						if (reportProgressByPeriod && nPolygonsSeen % progressPeriod == 0) {
							String message = MessageFormat.format("Processed {0} polygons...", nPolygonsSeen);
							context.getProgressLog().addMessage(message);
							logger.debug(message);
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

					for (var message : polygon.getMessages()) {
						errorLog.addMessage(message.toString());
					}
				}
			}

			int nPolygonsSeen = nPolygonsProcessed + nPolygonsSkipped;
			if (reportProgressByPeriod && nPolygonsSeen % progressPeriod != 0) {
				String message = MessageFormat.format("Processed {0} polygons...", nPolygonsSeen);
				context.getProgressLog().addMessage(message);
			}

			if (!ProgressFrequency.NEVER.equals(context.getParams().getProgressFrequency())) {
				context.getProgressLog().addMessage(
						"Processing summary: {0} polygons processed + {1} skipped = {2} seen", nPolygonsProcessed,
						nPolygonsSkipped, nPolygonsProcessed + nPolygonsSkipped
				);
			}
		} finally {
			context.endRun();
		}
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	public InputStream getYieldTable() throws PolygonExecutionException {
		if (context.isTrialRun()) {
			return new ByteArrayInputStream(new byte[0]);
		} else {
			try {
				return context.getYieldTable().getAsStream();
			} catch (YieldTableGenerationException e) {
				throw new PolygonExecutionException(e);
			}
		}
	}

	public static record ProjectionResultsKey(String polygonId, String projectionType, String entryName) {
		@Override
		public String toString() {
			return polygonId + '-' + projectionType + '-' + entryName;
		}
	};

	public Iterator<Entry<ProjectionResultsKey, Path>> getProjectionResults() {

		var projectionResults = new HashMap<ProjectionResultsKey, Path>();

		if (context.getParams().containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_FILES)) {

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

									projectionResults.put(key, expectedFileName);
								}
							}
						}
					}
				}
			} catch (IOException e) {
				logger.warn("Unable to create Projection Results map", e);
			}
		}

		return projectionResults.entrySet().iterator();
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

	@Override
	public void close() {
		context.close();
	}
}
