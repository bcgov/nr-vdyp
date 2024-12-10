package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProjectionRequestKind;
import jakarta.validation.Valid;

public class StubProjectionRunner implements IProjectionRunner {

	private final ProjectionState state;

	public StubProjectionRunner(ProjectionRequestKind kind, String projectionId, @Valid Parameters parameters) {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog().addMessage("Running Projection");

		ProjectionRequestValidator.validate(state, streams);
	}

	@Override
	public ProjectionState getState() {
		return state;
	}

	@Override
	public InputStream getYieldTable() throws ProjectionExecutionException {
		// No projection was done; therefore, there's no yield table.
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public InputStream getProgressStream() {
		return state.getProgressLog().getAsStream();
	}

	@Override
	public InputStream getErrorStream() {
		return state.getErrorLog().getAsStream();
	}
}
