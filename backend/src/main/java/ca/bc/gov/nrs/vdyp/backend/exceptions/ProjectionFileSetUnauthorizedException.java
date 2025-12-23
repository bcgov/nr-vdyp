package ca.bc.gov.nrs.vdyp.backend.exceptions;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;

public class ProjectionFileSetUnauthorizedException extends ProjectionServiceException {
	UUID fileSetGUID;

	public ProjectionFileSetUnauthorizedException(UUID fileSetGUID, VDYPUserModel model) {
		super(
				String.format(
						"User %s does not have authorization to update file set %s", model.getOidcGUID(), fileSetGUID
				)
		);
		this.fileSetGUID = fileSetGUID;
	}
}
