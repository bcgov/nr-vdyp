package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.v1.utils.FileHelper;

public class ProjectionRunner implements IProjectionRunner {

	private final ProjectionState state;

	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters) {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog().addMessage("Running Projection");

		ProjectionRequestValidator.validate(state, streams);

		createInputStream(streams.get("polygon"), streams.get("layers"));

		project();
	}

	@Override
	public ProjectionState getState() {
		return state;
	}

	private void createInputStream(InputStream polyStream, InputStream layersStream) {
		// TODO Auto-generated method stub
	}

	private void project() {
		// TODO Auto-generated method stub
	}

	@Override
	public InputStream getYieldTable() throws ProjectionExecutionException {
		// TODO: For now...
		try {
			return FileHelper.getStubResourceFile("Output_YldTbl.csv");
		} catch (IOException e) {
			throw new ProjectionExecutionException(e);
		}
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
