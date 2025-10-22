package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;

public class ProjectionFileSetResourceAssembler {

	private FileSetTypeCodeResourceAssembler fstcra;

	public ProjectionFileSetResourceAssembler() {
		fstcra = new FileSetTypeCodeResourceAssembler();
	}

	public ProjectionFileSetEntity toModel(ProjectionFileSetModel model) {
		if (model == null) {
			return null;
		}
		ProjectionFileSetEntity entity = new ProjectionFileSetEntity();
		entity.setProjectionFileSetGUID(UUID.fromString(model.getProjectionFileSetGUID()));
		entity.setFileSetTypeCode(fstcra.toEntity(model.getFileSetTypeCode()));
		entity.setFileSetName(model.getFileSetName());
		return entity;
	}

	public ProjectionFileSetModel toEntity(ProjectionFileSetEntity entity) {
		if (entity == null) {
			return null;
		}
		ProjectionFileSetModel model = new ProjectionFileSetModel();
		model.setProjectionFileSetGUID(entity.getProjectionFileSetGUID().toString());
		model.setFileSetTypeCode(fstcra.toModel(entity.getFileSetTypeCode()));
		model.setFileSetName(entity.getFileSetName());
		return model;
	}
}
