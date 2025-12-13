package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionParameterPresetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionParameterPresetModel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionParameterPresetResourceAssembler {
	VDYPUserResourceAssembler vura;

	public ProjectionParameterPresetResourceAssembler() {
		vura = new VDYPUserResourceAssembler();
	}

	public ProjectionParameterPresetEntity toEntity(ProjectionParameterPresetModel model) {
		if (model == null) {
			return null;
		}

		ProjectionParameterPresetEntity entity = new ProjectionParameterPresetEntity();
		entity.setProjectionParameterPresetGUID(UUID.fromString(model.getProjectionParameterPresetGUID()));
		entity.setOwnerUser(vura.toEntity(model.getOwnerUser()));
		entity.setPresetName(model.getPresetName());
		entity.setPresetParameters(model.getPresetParameters());

		return entity;
	}

	public ProjectionParameterPresetModel toModel(ProjectionParameterPresetEntity entity) {
		if (entity == null) {
			return null;
		}

		ProjectionParameterPresetModel model = new ProjectionParameterPresetModel();
		model.setProjectionParameterPresetGUID(entity.getProjectionParameterPresetGUID().toString());
		model.setOwnerUser(vura.toModel(entity.getOwnerUser()));
		model.setPresetName(entity.getPresetName());
		model.setPresetParameters(entity.getPresetParameters());

		return model;
	}
}
