package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.output.IMessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.MessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.NullMessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.backend.utils.ProjectionUtils;

public class ProjectionContext {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionContext.class);

	private final String projectionId;
	private long startTime_ms;

	private final ProjectionRequestKind requestKind;
	private final boolean isTrailRun;

	private Parameters params;
	private ValidatedParameters vparams;

	private Path rootFolder;
	private Path executionFolder;

	private final IMessageLog progressLog;
	private final IMessageLog errorLog;
	private Optional<YieldTable> yieldTable;

	public ProjectionContext(
			ProjectionRequestKind requestKind, String projectionId, Parameters params, boolean isTrialRun
	) throws ProjectionRequestException {

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

		this.params = params;

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

		ProjectionRequestParametersValidator.validate(this);

		buildProjectionExecutionStructure();

		yieldTable = Optional.empty();
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
			} catch (IOException | YieldTableGenerationException e) {
				logger.error("Encountered exception closing the yield table of projection " + projectionId, e);
			}
		}
	}

	public Parameters getRawParams() {
		return params;
	}

	void setValidatedParams(ValidatedParameters validatedParams) {
		this.vparams = validatedParams;
	}

	public ValidatedParameters getValidatedParams() {
		return vparams;
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

	private void buildProjectionExecutionStructure() throws ProjectionInternalExecutionException {

		if (this.executionFolder != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".buildExecutionFolder: executionFolder has already been set"
			);
		}

		try {
			URL rootUrl = ProjectionUtils.class.getClassLoader().getResource("ca/bc/gov/nrs/vdyp/template");

			this.rootFolder = Path.of(rootUrl.toURI());
			this.executionFolder = Files.createTempDirectory(rootFolder, projectionId + '-');

		} catch (IOException | URISyntaxException e) {
			throw new ProjectionInternalExecutionException(e);
		}
	}

	public Path getExecutionFolder() {
		if (this.executionFolder == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".getExecutionFolder: executionFolder has not been set"
			);
		}
		return executionFolder;
	}

	public Path getRootFolder() {
		if (this.rootFolder == null) {
			throw new IllegalStateException(this.getClass().getName() + ".getRootFolder: rootFolder has not been set");
		}
		return rootFolder;
	}

	public void addMessage(String message, Object... args) {
		String messageText = MessageFormat.format(message, args);
		getErrorLog().addMessage(messageText);
		PolygonProjectionRunner.logger.debug(messageText);
	}
}
