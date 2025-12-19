package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionUnauthorizedException extends ProjectionServiceException {
	public ProjectionUnauthorizedException(UUID projectionGuid, UUID actingUserGuid) {
		super(
				String.format("Projection %s cannot be accessed by user %s", projectionGuid, actingUserGuid),
				projectionGuid, actingUserGuid
		);
	}
}
