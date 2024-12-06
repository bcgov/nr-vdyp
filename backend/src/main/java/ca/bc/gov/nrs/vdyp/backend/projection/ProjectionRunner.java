package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class ProjectionRunner implements IProjectionRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionRunner.class);
	
	private final ProjectionState state;
	
	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters) {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog().addMessage(MessageFormat.format("Running Projection of type {0}", state.getRequestKind()));

		ProjectionRequestParametersValidator.validate(state);

		logger.debug("{0}", state.getValidatedParams().toString());
		logApplicationMetadata();
		
		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state.getRequestKind(), streams);

		project(polygonStream);
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	@Override
	public ProjectionState getState() {
		return state;
	}

	private void project(AbstractPolygonStream polygonStream) {
		
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
