package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;

public interface IProjectionRunner {

	void run(Map<String, InputStream> inputStreams)
			throws ProjectionRequestValidationException, ProjectionInternalExecutionException;

	ProjectionState getState();

	InputStream getYieldTable() throws ProjectionInternalExecutionException;

	InputStream getProgressStream() throws ProjectionInternalExecutionException;

	InputStream getErrorStream() throws ProjectionInternalExecutionException;

}