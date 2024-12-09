package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;

public class ProjectionRequestValidator {

	public static void validate(ProjectionState state, Map<String, InputStream> streams)
			throws ProjectionRequestValidationException {

		validateState(state);
		validateStreams(streams);
	}

	private static void validateStreams(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
	}

	private static void validateState(ProjectionState state) throws ProjectionRequestValidationException {
	}
}
