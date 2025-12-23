package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

public class ProjectionFileSetNotFoundException extends ProjectionServiceException {
	private UUID fileSetGUID;

	public ProjectionFileSetNotFoundException(UUID fileSetGUID) {
		super(String.format("Could not find projection file set %s", fileSetGUID));
		this.fileSetGUID = fileSetGUID;
	}

}
