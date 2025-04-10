package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.output.IMessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.MessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.NullMessageLog;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTable;

public class ProjectionContext {

	private final String projectionId;

	private final ProjectionRequestKind requestKind;
	private final boolean isTrailRun;

	private Parameters params;
	private ValidatedParameters vparams;

	private Path rootFolder;
	private Path executionFolder;

	private final IMessageLog progressLog;
	private final IMessageLog errorLog;
	private final YieldTable yieldTable;

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

		yieldTable = YieldTable.of(this);
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

	public YieldTable getYieldTable() {
		return yieldTable;
	}

	public void buildExecutionFolder(Path rootFolder) throws IOException {
		if (this.executionFolder != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".buildExecutionFolder: executionFolder has already been set"
			);
		}

		this.rootFolder = rootFolder;
		this.executionFolder = Files.createTempDirectory(rootFolder, projectionId + '-');
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
