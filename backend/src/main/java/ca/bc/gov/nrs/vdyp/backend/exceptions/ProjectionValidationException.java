package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionValidationException extends ProjectionServiceException {
	public ProjectionValidationException(String message, UUID projectionGuid) {
		super(message, projectionGuid);
	}
}
