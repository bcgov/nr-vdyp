package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class StubProjectionRunner implements IProjectionRunner {

	private final ProjectionState state;

	public StubProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters) {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog().addMessage("Running Projection");

		ProjectionRequestParametersValidator.validate(state);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state.getRequestKind(), streams);
		
		while (polygonStream.hasNextPolygon()) {
			
		}
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
