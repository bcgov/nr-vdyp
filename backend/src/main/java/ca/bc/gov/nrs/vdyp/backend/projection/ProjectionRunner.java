package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class ProjectionRunner implements IProjectionRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionRunner.class);

	private final ProjectionContext context;

	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters, Boolean isTrialRun)
			throws ProjectionRequestValidationException {
		this.context = new ProjectionContext(kind, projectionId, parameters, isTrialRun);
	}

	@Override
	public void run(Map<String, InputStream> streams)
			throws ProjectionRequestValidationException, ProjectionInternalExecutionException {
		context.getProgressLog().addMessage("Running Projection of type {0}", context.getRequestKind());

		logger.debug("{}", context.getValidatedParams().toString());
		logApplicationMetadata();

		AbstractPolygonStream polygonStream = AbstractPolygonStream.build(context, streams);

		IComponentRunner componentRunner;
		if (context.isTrialRun()) {
			componentRunner = new StubComponentRunner();
		} else {
			componentRunner = new ComponentRunner();
		}

		while (polygonStream.hasNextPolygon()) {

			try {
				var polygon = polygonStream.getNextPolygon();
				if (polygon.doAllowProjection()) {
					logger.info("Starting the projection of feature \"{}\"", polygon);
					var polygonProjectionRunner = new PolygonProjectionRunner(polygon, context, componentRunner);
					polygonProjectionRunner.project();
				} else {
					logger.info("Skipping the projection of feature \"{}\" on request", polygon);
				}
			} catch (PolygonValidationException e) {
				IMessageLog errorLog = context.getErrorLog();
				for (ValidationMessage m : e.getValidationMessages()) {
					errorLog.addMessage(m.getKind().template, m.getArgs());
				}
			} catch (PolygonExecutionException e) {
				IMessageLog errorLog = context.getErrorLog();
				for (ValidationMessage m : e.getValidationMessages()) {
					errorLog.addMessage(m.getKind().template, m.getArgs());
				}
			}
		}
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	@Override
	public InputStream getYieldTable() throws ProjectionInternalExecutionException {
		if (context.isTrialRun()) {
			return new ByteArrayInputStream(new byte[0]);
		} else {
			// TODO: For now...
			try {
				return FileHelper.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "Output_YldTbl.csv");
			} catch (IOException e) {
				throw new ProjectionInternalExecutionException(e);
			}
		}
	}

	@Override
	public InputStream getProgressStream() {
		return context.getProgressLog().getAsStream();
	}

	@Override
	public InputStream getErrorStream() {
		return context.getErrorLog().getAsStream();
	}
}
