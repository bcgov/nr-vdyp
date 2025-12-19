package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import jakarta.enterprise.context.Dependent;

@Dependent
public class ProjectionStatusCodeResourceAssembler {
	public ProjectionStatusCodeEntity toEntity(ProjectionStatusCodeModel model) {
		if (model == null) {
			return null;
		}

		ProjectionStatusCodeEntity entity = new ProjectionStatusCodeEntity();
		entity.setCode(model.getCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public ProjectionStatusCodeModel toModel(ProjectionStatusCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		ProjectionStatusCodeModel model = new ProjectionStatusCodeModel();
		model.setCode(entity.getCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
