package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;

public class StubProjectionRunner implements IProjectionRunner {

	private final ProjectionState state;

	public StubProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters)
			throws ProjectionRequestValidationException {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog().addMessage("Running Projection");

		ProjectionRequestParametersValidator.validate(state);

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);
		
		var validationMessages = new ArrayList<ValidationMessage>();
		
		while (polygonStream.hasNextPolygon()) {
			try {
				polygonStream.getNextPolygon();
			} catch (PolygonValidationException e) {
				validationMessages.addAll(e.getValidationMessages());
			}
		}
		
		if (! validationMessages.isEmpty()) {
			throw new ProjectionRequestValidationException(validationMessages);
		}
	}

	@Override
	public ProjectionState getState() {
		return state;
	}

	@Override
	public InputStream getYieldTable() throws ProjectionInternalExecutionException {
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
