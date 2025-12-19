package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileSetTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import jakarta.enterprise.context.Dependent;

@Dependent
public class FileSetTypeCodeResourceAssembler {
	public FileSetTypeCodeResourceAssembler() {
	}

	public FileSetTypeCodeEntity toEntity(FileSetTypeCodeModel model) {
		if (model == null) {
			return null;
		}

		FileSetTypeCodeEntity entity = new FileSetTypeCodeEntity();
		entity.setCode(model.getCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public FileSetTypeCodeModel toModel(FileSetTypeCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		FileSetTypeCodeModel model = new FileSetTypeCodeModel();
		model.setCode(entity.getCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
