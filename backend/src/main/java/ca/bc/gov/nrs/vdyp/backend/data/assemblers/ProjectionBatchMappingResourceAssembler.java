package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;

public class ProjectionBatchMappingResourceAssembler {
	public ProjectionBatchMappingResourceAssembler() {

	}

	public ProjectionBatchMappingEntity tpEntity(ProjectionBatchMappingModel model) {
		if (model == null) {
			return null;
		}

		ProjectionBatchMappingEntity entity = new ProjectionBatchMappingEntity();
		entity.setProjectionBatchMappingGUID(UUID.fromString(model.getProjectionBatchMappingGUID()));
		entity.setBatchJobGUID(UUID.fromString(model.getBatchJobGUID()));
		return entity;
	}

	public ProjectionBatchMappingModel toModel(ProjectionBatchMappingEntity entity) {
		if (entity == null) {
			return null;
		}

		ProjectionBatchMappingModel model = new ProjectionBatchMappingModel();
		model.setProjectionBatchMappingGUID(entity.getProjectionBatchMappingGUID().toString());
		model.setBatchJobGUID(entity.getBatchJobGUID().toString());
		return model;
	}
}
