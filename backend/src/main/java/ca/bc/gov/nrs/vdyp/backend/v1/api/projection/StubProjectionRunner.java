package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.v1.utils.FileHelper;

public class StubProjectionRunner implements IProjectionRunner {

	private final ProjectionState state;

	public StubProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters) {
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
		try {
			return FileHelper.getStubResourceFile("Output_YldTbl.csv");
		} catch (IOException e) {
			throw new ProjectionExecutionException(e);
		}
	}

	@Override
	public InputStream getProgressStream() throws ProjectionExecutionException {
		try {
			return FileHelper.getStubResourceFile("Output_Log.txt");
		} catch (IOException e) {
			throw new ProjectionExecutionException(e);
		}
	}

	@Override
	public InputStream getErrorStream() throws ProjectionExecutionException {
		try {
			return FileHelper.getStubResourceFile("Output_Error.txt");
		} catch (IOException e) {
			throw new ProjectionExecutionException(e);
		}
	}
}
