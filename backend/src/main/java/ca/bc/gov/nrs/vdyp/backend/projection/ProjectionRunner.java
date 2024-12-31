package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.messaging.IMessageLog;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingState;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class ProjectionRunner implements IProjectionRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionRunner.class);

	private final ProjectionState state;

	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters)
			throws ProjectionRequestValidationException {
		this.state = new ProjectionState(kind, projectionId, parameters);
	}

	@Override
	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
		state.getProgressLog()
				.addMessage(MessageFormat.format("Running Projection of type {0}", state.getRequestKind()));

		logger.debug("{}", state.getValidatedParams().toString());
		logApplicationMetadata();

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(state, streams);

		projectAll(polygonStream);
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	@Override
	public ProjectionState getState() {
		return state;
	}

	private void projectAll(AbstractPolygonStream polygonStream) {

		while (polygonStream.hasNextPolygon()) {

			try {
				var polygonToProject = polygonStream.getNextPolygon();
				if (polygonToProject.doAllowProjection()) {
					logger.info("Starting the projection of feature \"{}\"", polygonToProject);
					project(polygonToProject);
				} else {
					logger.info("Skipping the projection of feature \"{}\" on request", polygonToProject);
				}
			} catch (PolygonValidationException e) {
				IMessageLog errorLog = state.getErrorLog();
				for (ValidationMessage m : e.getValidationMessages()) {
					errorLog.addMessage(m.getKind().template, m.getArgs());
				}
			} catch (PolygonExecutionException e) {
				IMessageLog errorLog = state.getErrorLog();
				for (ValidationMessage m : e.getValidationMessages()) {
					errorLog.addMessage(m.getKind().template, m.getArgs());
				}
			}
		}
	}

	private void project(Polygon polygon) throws PolygonExecutionException {
		if (polygon.getCurrentProcessingState() != PolygonProcessingState.POLYGON_DEFINED) {
			throw new IllegalStateException("Cannot call ProjectionRunner.project unless the Polygon is in DEFINING_POLYGON state");
		}

		// Begin implementation based on code starting at line 2088 (call to "V7Ext_GetPolygonInfo") in vdyp7console.c.
		// Note the funky error handling in this routine: "rtrnCode" is set to SUCCESS and is potentially set to
		// another value only when YldTable_GeneratePolygonYieldTables is called. "v7RtrnCode" is set to the result
		// of all the other routines and a negative result will sometimes inhibit the execution of a follow-on block
		// of code such as "V7Ext_ProjectStandByAge" and "YldTable_GeneratePolygonYieldTables" but not all. It's hard
		// to understand why things are done the way they are.

		polygon.doCompleteProjection();
	}

	@Override
	public InputStream getYieldTable() throws ProjectionInternalExecutionException {
		// TODO: For now...
		try {
			return FileHelper.getStubResourceFile("Output_YldTbl.csv");
		} catch (IOException e) {
			throw new ProjectionInternalExecutionException(e);
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
