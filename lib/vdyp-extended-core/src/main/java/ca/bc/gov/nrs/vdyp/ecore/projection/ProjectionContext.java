package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.IMessageLog;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.MessageLog;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.NullMessageLog;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

public class ProjectionContext {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionContext.class);

	public static final int EXECUTION_FOLDER_RETENTION_TIME_m = 30;

	private record ProjectionDetails(int startYear, int firstRequestedYear) {
	}

	private final String projectionId;
	private long startTime_ms;

	private final ProjectionRequestKind requestKind;
	private final boolean isTrailRun;

	private ValidatedParameters validatedParams;

	private Path executionFolder;

	private final IMessageLog progressLog;
	private final IMessageLog errorLog;
	private Optional<YieldTable> yieldTable;
	private Set<YieldTable.Category> yieldTableCategories;

	private ExecutorService executorService;
	private FileSystem resourceFileSystem;

	private Map<Long, Map<ProjectionTypeCode, ProjectionDetails>> projectionDetailsMap = new HashMap<>();

	public ProjectionContext(
			ProjectionRequestKind requestKind, String projectionId, Parameters params, boolean isTrialRun
	) throws AbstractProjectionRequestException {

		if (requestKind == null) {
			throw new IllegalArgumentException("kind cannot be null in constructor of ProjectionState");
		}
		if (projectionId == null || projectionId.isBlank()) {
			throw new IllegalArgumentException("projectionId cannot be empty in constructor of ProjectionState");
		}
		if (params == null) {
			throw new IllegalArgumentException("params cannot be null in constructor of ProjectionState");
		}

		this.projectionId = projectionId;
		this.isTrailRun = isTrialRun;
		this.requestKind = requestKind;

		var loggingParams = LoggingParameters.of(params);

		if (loggingParams.doEnableErrorLogging()) {
			errorLog = new MessageLog(Level.ERROR);
		} else {
			errorLog = new NullMessageLog(Level.ERROR);
		}

		if (loggingParams.doEnableProgressLogging()) {
			progressLog = new MessageLog(Level.INFO);
		} else {
			progressLog = new NullMessageLog(Level.INFO);
		}

		this.yieldTable = Optional.empty();
		this.executorService = Executors.newSingleThreadExecutor();

		this.validatedParams = ProjectionRequestParametersValidator.validate(params, this.getRequestKind());

		this.yieldTableCategories = new HashSet<YieldTable.Category>();

		addActiveCategories();

		applyVDYP7Limits();

		buildProjectionExecutionStructure();
	}

	private void addActiveCategories() {
		if (validatedParams.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				&& validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {
			yieldTableCategories.add(YieldTable.Category.LAYER_MOFBIOMASS);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				&& validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)) {
			yieldTableCategories.add(YieldTable.Category.LAYER_MOFVOLUMES);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
			yieldTableCategories.add(YieldTable.Category.CFSBIOMASS);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				&& validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)) {
			yieldTableCategories.add(YieldTable.Category.SPECIES_MOFVOLUME);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				&& validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {
			yieldTableCategories.add(YieldTable.Category.SPECIES_MOFBIOMASS);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)) {
			yieldTableCategories.add(YieldTable.Category.PROJECTION_MODE);
		}
		if (validatedParams.containsOption(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)) {
			yieldTableCategories.add(YieldTable.Category.POLYGON_ID);
		}
	}

	public void recordProjectionDetails(
			Polygon polygon, ProjectionTypeCode projectionType, int projectionStartYear, int firstRequestedYear
	) {

		projectionDetailsMap.putIfAbsent(polygon.getFeatureId(), new HashMap<ProjectionTypeCode, ProjectionDetails>());
		var polygonProjectionDetails = projectionDetailsMap.get(polygon.getFeatureId());

		if (polygonProjectionDetails.containsKey(projectionType)) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}: projectionDetailsMap already contains entry for projection type {1}", polygon,
							projectionType
					)
			);
		}

		polygonProjectionDetails.put(projectionType, new ProjectionDetails(projectionStartYear, firstRequestedYear));
	}

	public ProjectionDetails getProjectionDetails(Polygon polygon, ProjectionTypeCode projectionType) {
		if (!projectionDetailsMap.containsKey(polygon.getFeatureId())
				|| !projectionDetailsMap.get(polygon.getFeatureId()).containsKey(projectionType)) {
			throw new IllegalArgumentException(
					MessageFormat.format(
							"{0}: projectionDetailsMap does not contain entry for polygon and/or projection {1} of that polygon",
							polygon, projectionType
					)
			);
		}

		return projectionDetailsMap.get(polygon.getFeatureId()).get(projectionType);
	}

	public void startRun() {

		getProgressLog().addMessage("{0}: starting projection (type {1})", projectionId, getRequestKind());

		startTime_ms = System.currentTimeMillis();

		try {
			getYieldTable().startGeneration();
		} catch (YieldTableGenerationException e) {
			errorLog.addMessage(
					"Encountered error starting the generation of this projection's yield table{}",
					e.getMessage() != null ? ": " + e.getMessage() : ""
			);
		}
	}

	/**
	 * This method replicates the logic in VDYP7CORE_RunVDYPModel
	 */
	private void applyVDYP7Limits() {
		// TODO
	}

	private void buildProjectionExecutionStructure() throws PolygonExecutionException {

		if (this.executionFolder != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".buildExecutionFolder: executionFolder has already been set"
			);
		}

		// This hack registers NativeImageResourceFileSystemProvider & NativeImageResourceFileSystem when
		// ran via Native Image. It allows lookups using "resource:/" URIs which means calls like
		// Path.of(URI) will not fail.
		try {
			resourceFileSystem = FileSystems
					.newFileSystem(URI.create("resource:/"), Collections.singletonMap("create", "true"));
			logger.info("Created {} filesystem", resourceFileSystem.getClass().getSimpleName());
		} catch (Exception e) {
			// This will always happen outside of an native image; there no such thing as a "resource:/" file system
			// outside of native image
			logger.info("Not creating resource file system as not a native image.");
		}

		try {
			this.executionFolder = Files.createTempDirectory(projectionId + '-');

			logger.info("{}: execution folder is {}", projectionId, executionFolder);

		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}

	public void endRun() {
		try {
			try {
				getYieldTable().endGeneration();
			} catch (YieldTableGenerationException e) {
				errorLog.addMessage(
						"Encountered error starting the generation of this projection's yield table{}",
						e.getMessage() != null ? ": " + e.getMessage() : ""
				);
			}

			long endTime_ms = System.currentTimeMillis();

			getProgressLog().addMessage(
					"{0}: completing projection (type {1}); duration: {2}ms", projectionId, getRequestKind(),
					endTime_ms - startTime_ms
			);
		} finally {
			try {
				getYieldTable().close();
			} catch (YieldTableGenerationException e) {
				logger.error("Encountered exception closing the yield table of projection " + projectionId, e);
			}
		}
	}

	public void close() {

		// Close the fileSystem instance (possibly) opened in buildProjectionExecutionStructure
		Utils.close(resourceFileSystem, "resourceFileSystem");

		// Finally, delete the execution folder tree EXECUTION_FOLDER_RETENTION_TIME_m minutes from now.

		if (validatedParams.containsOption(ExecutionOption.DO_DELAY_EXECUTION_FOLDER_DELETION)) {
			logger.info(
					"Scheduling deletion of execution folder {} for {}m", executionFolder,
					LocalDateTime.now().plusMinutes(EXECUTION_FOLDER_RETENTION_TIME_m)
			);

			executorService.submit(
					new ExecutionFolderRemover(executionFolder, EXECUTION_FOLDER_RETENTION_TIME_m * 60 * 1000L)
			);
		} else {
			logger.info("Deleting execution folder {}", executionFolder);
			ExecutionFolderRemover.doRemove(executionFolder);
		}
	}

	private static class ExecutionFolderRemover implements Callable<Boolean> {

		private Path executionFolder;
		private long delay_ms = 0;

		public ExecutionFolderRemover(Path executionFolder, long delay_ms) {
			this.delay_ms = delay_ms;
			this.executionFolder = executionFolder;
		}

		@Override
		public Boolean call() throws InterruptedException {
			try {
				Thread.sleep(delay_ms);
			} catch (InterruptedException e) {
				logger.error("Saw unexpected InterruptedException during \"sleep\" of " + delay_ms + "ms", e);
				throw e;
			}

			doRemove(executionFolder);

			return true;
		}

		static void doRemove(Path executionFolder) {

			FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					if (e == null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed
						throw e;
					}
				}
			};

			try {
				Files.walkFileTree(executionFolder, visitor);
				logger.info("Deletion of execution folder {} completed", executionFolder);
			} catch (IOException e) {
				logger.info(
						"Deletion of execution folder {} failed{}", executionFolder,
						e.getCause() != null ? ". Reason: " + e.getCause() : ""
				);
			}
		}
	}

	public ValidatedParameters getParams() {
		return validatedParams;
	}

	public String getProjectionId() {
		return projectionId;
	}

	public ProjectionRequestKind getRequestKind() {
		return requestKind;
	}

	public boolean isTrialRun() {
		return isTrailRun;
	}

	public IMessageLog getProgressLog() {
		return progressLog;
	}

	public IMessageLog getErrorLog() {
		return errorLog;
	}

	public YieldTable getYieldTable() throws YieldTableGenerationException {

		if (yieldTable.isEmpty()) {
			yieldTable = Optional.of(YieldTable.of(this));
		}
		return yieldTable.get();
	}

	public Set<YieldTable.Category> getYieldTableCategories() {
		return yieldTableCategories;
	}

	public Path getExecutionFolder() {
		if (this.executionFolder == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".getExecutionFolder: executionFolder has not been set"
			);
		}
		return executionFolder;
	}
}
