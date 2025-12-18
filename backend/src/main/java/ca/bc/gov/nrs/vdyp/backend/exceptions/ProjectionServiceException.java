package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionServiceException extends Exception {

	private final UUID projectionGuid;
	private final UUID actingUserGuid;

	public ProjectionServiceException(String message) {
		super(message);
		projectionGuid = null;
		actingUserGuid = null;
	}

	public ProjectionServiceException(String message, Throwable e) {
		super(message, e);
		projectionGuid = null;
		actingUserGuid = null;
	}

	public ProjectionServiceException(String message, UUID projectionGuid) {
		super(message);
		this.projectionGuid = projectionGuid;
		actingUserGuid = null;
	}

	public ProjectionServiceException(String message, UUID projectionGuid, UUID actingUserGuid) {
		super(message);
		this.projectionGuid = projectionGuid;
		this.actingUserGuid = actingUserGuid;
	}

	public ProjectionServiceException(String message, Throwable e, UUID projectionGuid) {
		super(message, e);
		this.projectionGuid = projectionGuid;
		this.actingUserGuid = null;
	}

	public ProjectionServiceException(String message, Throwable e, UUID projectionGuid, UUID actingUserGuid) {
		super(message, e);
		this.projectionGuid = projectionGuid;
		this.actingUserGuid = actingUserGuid;
	}

	public UUID getProjectionGuid() {
		return projectionGuid;
	}

	public UUID getActingUserGuid() {
		return actingUserGuid;
	}
}
