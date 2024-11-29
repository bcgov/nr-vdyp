package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionExecutionException;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;

public interface IProjectionRunner {

	void run(Map<String, InputStream> inputStreams) throws ProjectionRequestValidationException, ProjectionExecutionException;

	ProjectionState getState();

	InputStream getYieldTable() throws ProjectionExecutionException;

	InputStream getProgressStream() throws ProjectionExecutionException;

	InputStream getErrorStream() throws ProjectionExecutionException;

}