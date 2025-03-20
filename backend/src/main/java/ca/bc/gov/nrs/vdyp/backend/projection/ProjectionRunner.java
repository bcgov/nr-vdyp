package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.output.IMessageLog;

public class ProjectionRunner {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionRunner.class);

	private final ProjectionContext context;

	public ProjectionRunner(ProjectionRequestKind kind, String projectionId, Parameters parameters, Boolean isTrialRun)
			throws ProjectionRequestException {
		this.context = new ProjectionContext(kind, projectionId, parameters, isTrialRun);
	}

	public void run(Map<String, InputStream> streams) throws ProjectionRequestValidationException,
			ProjectionInternalExecutionException, YieldTableGenerationException {

		context.startRun();
		try {

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
						PolygonProjectionRunner.of(polygon, context, componentRunner).project();
					} else {
						logger.info("By request, the projection of feature \"{}\" has been skipped", polygon);
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
		} finally {
			context.endRun();
		}
	}

	private void logApplicationMetadata() {
		// TODO: mimic VDYP7's Console_LogMetadata
	}

	public InputStream getYieldTable() throws ProjectionInternalExecutionException {
		if (context.isTrialRun()) {
			return new ByteArrayInputStream(new byte[0]);
		} else {
			try {
				return context.getYieldTable().getAsStream();
			} catch (YieldTableGenerationException e) {
				throw new ProjectionInternalExecutionException(e);
			}
		}
	}

	public InputStream getProgressStream() {
		return context.getProgressLog().getAsStream();
	}

	public InputStream getErrorStream() {
		return context.getErrorLog().getAsStream();
	}
}
