package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionNotFoundException extends ProjectionServiceException {
	public ProjectionNotFoundException(UUID projectionGuid) {
		super(String.format("Projection %s could not be found", projectionGuid), projectionGuid);
	}
}
