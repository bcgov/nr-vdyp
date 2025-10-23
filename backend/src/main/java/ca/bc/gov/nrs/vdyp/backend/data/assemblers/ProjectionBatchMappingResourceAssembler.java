package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;

public class ProjectionBatchMappingResourceAssembler {
	public ProjectionBatchMappingResourceAssembler() {

	}

	public ProjectionBatchMappingEntity toEntity(ProjectionBatchMappingModel model) {
		if (model == null) {
			return null;
		}

		ProjectionBatchMappingEntity entity = new ProjectionBatchMappingEntity();
		entity.setProjectionBatchMappingGUID(
				model.getProjectionBatchMappingGUID() == null ? null
						: UUID.fromString(model.getProjectionBatchMappingGUID())
		);
		entity.setBatchJobGUID(model.getBatchJobGUID() == null ? null : UUID.fromString(model.getBatchJobGUID()));
		return entity;
	}

	public ProjectionBatchMappingModel toModel(ProjectionBatchMappingEntity entity) {
		if (entity == null) {
			return null;
		}

		ProjectionBatchMappingModel model = new ProjectionBatchMappingModel();
		model.setProjectionBatchMappingGUID(
				entity.getProjectionBatchMappingGUID() == null ? null
						: entity.getProjectionBatchMappingGUID().toString()
		);
		model.setBatchJobGUID(entity.getBatchJobGUID() == null ? null : entity.getBatchJobGUID().toString());
		return model;
	}
}
