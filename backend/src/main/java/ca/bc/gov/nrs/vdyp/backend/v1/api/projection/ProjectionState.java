package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import org.slf4j.event.Level;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.messaging.IMessageLog;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.messaging.MessageLog;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.messaging.NullMessageLog;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProjectionRequestKind;

public class ProjectionState {

	private final String projectionId;
	private final ProjectionRequestKind kind;

	// This field will be updated once the parameters have been validated.
	private Parameters params;

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
		this.params = validatedParams;
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
