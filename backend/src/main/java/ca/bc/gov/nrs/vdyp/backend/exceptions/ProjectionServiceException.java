package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionServiceException extends Exception {

	private static final Logger log = LoggerFactory.getLogger(ProjectionServiceException.class);

	private final UUID projectionGuid;
	private UUID actingUserGuid;

	public ProjectionServiceException(String message, UUID projectionGuid) {
		super(message);
		this.projectionGuid = projectionGuid;
	}

	public ProjectionServiceException(String message, UUID projectionGuid, UUID actingUserGuid) {
		this(message, projectionGuid);
		this.actingUserGuid = actingUserGuid;
	}

	public ProjectionServiceException(String message, Throwable e, UUID projectionGuid) {
		super(message, e);
		this.projectionGuid = projectionGuid;
	}

	public ProjectionServiceException(String message, Throwable e, UUID projectionGuid, UUID actingUserGuid) {
		this(message, e, projectionGuid);
		this.actingUserGuid = actingUserGuid;
	}

	public UUID getProjectionGuid() {
		return projectionGuid;
	}

	public UUID getActingUserGuid() {
		return actingUserGuid;
	}
}
