package ca.bc.gov.nrs.vdyp.backend.projection;

import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.backend.api.v1.messaging.IMessageLog;
import ca.bc.gov.nrs.vdyp.backend.api.v1.messaging.MessageLog;
import ca.bc.gov.nrs.vdyp.backend.api.v1.messaging.NullMessageLog;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;

public class ProjectionState {

	private final String projectionId;
	private final ProjectionRequestKind kind;

	private Parameters params;
	private ValidatedParameters vparams;
	
	private final IMessageLog progressLog;
	private final IMessageLog errorLog;

	public ProjectionState(ProjectionRequestKind kind, String projectionId, Parameters params) {

		if (kind == null) {
			throw new IllegalArgumentException("kind cannot be null in constructor of ProjectionState");
		}
		if (projectionId == null || projectionId.isBlank()) {
			throw new IllegalArgumentException("projectionId cannot be empty in constructor of ProjectionState");
		}
		if (params == null) {
			throw new IllegalArgumentException("params cannot be null in constructor of ProjectionState");
		}

		this.projectionId = projectionId;
		this.params = params;
		this.kind = kind;

		var loggingParams = LoggingParameters.of(params);

		if (loggingParams.doEnableErrorLogging) {
			errorLog = new MessageLog(Level.ERROR);
		} else {
			errorLog = new NullMessageLog(Level.ERROR);
		}

		if (loggingParams.doEnableProgressLogging) {
			progressLog = new MessageLog(Level.INFO);
		} else {
			progressLog = new NullMessageLog(Level.INFO);
		}
	}

	public Parameters getParams() {
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

	public ProjectionRequestKind getKind() {
		return kind;
	}

	public IMessageLog getProgressLog() {
		return progressLog;
	}

	public IMessageLog getErrorLog() {
		return errorLog;
	}
}
