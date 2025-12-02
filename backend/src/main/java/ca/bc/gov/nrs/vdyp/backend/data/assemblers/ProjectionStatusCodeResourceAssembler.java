package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;

public class ProjectionStatusCodeResourceAssembler {
	public ProjectionStatusCodeEntity toEntity(ProjectionStatusCodeModel model) {
		if (model == null) {
			return null;
		}

		ProjectionStatusCodeEntity entity = new ProjectionStatusCodeEntity();
		entity.setProjectionStatusCode(model.getProjectionStatusCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public ProjectionStatusCodeModel toModel(ProjectionStatusCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		ProjectionStatusCodeModel model = new ProjectionStatusCodeModel();
		model.setProjectionStatusCode(entity.getProjectionStatusCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
