package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.BatchFailureTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchFailureTypeCodeModel;
import jakarta.enterprise.context.Dependent;

@Dependent
public class BatchFailureTypeCodeResourceAssembler {
	public BatchFailureTypeCodeResourceAssembler() {
	}

	public BatchFailureTypeCodeEntity toEntity(BatchFailureTypeCodeModel model) {
		if (model == null) {
			return null;
		}

		BatchFailureTypeCodeEntity entity = new BatchFailureTypeCodeEntity();
		entity.setCode(model.getCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public BatchFailureTypeCodeModel toModel(BatchFailureTypeCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		BatchFailureTypeCodeModel model = new BatchFailureTypeCodeModel();
		model.setCode(entity.getCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
