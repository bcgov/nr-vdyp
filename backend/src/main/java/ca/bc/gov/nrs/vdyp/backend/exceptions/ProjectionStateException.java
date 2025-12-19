package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionStateException extends ProjectionServiceException {
	public ProjectionStateException(UUID projectionGuid, String action, String state) {
		super(
				String.format(
						"Cannot perform action %s on projection %s as it is in state %s", action, projectionGuid, state
				), projectionGuid
		);
	}
}
