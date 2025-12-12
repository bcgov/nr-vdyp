package ca.bc.gov.nrs.vdyp.backend.services;

import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Handles business rules for file sets, including creation updating and deletion
 */
@ApplicationScoped
public class ProjectionFileSetService {
	public ProjectionFileSetModel createEmptyFileSet(FileSetTypeCodeModel typeCodeModel, VDYPUserModel actingUser) {

	}
}
