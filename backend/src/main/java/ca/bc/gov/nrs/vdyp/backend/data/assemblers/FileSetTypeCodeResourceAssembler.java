package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileSetTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;

public class FileSetTypeCodeResourceAssembler {
	public FileSetTypeCodeResourceAssembler() {
	}

	public FileSetTypeCodeEntity toEntity(FileSetTypeCodeModel model) {
		if (model == null) {
			return null;
		}

		FileSetTypeCodeEntity entity = new FileSetTypeCodeEntity();
		entity.setFileSetTypeCode(model.getFileSetTypeCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public FileSetTypeCodeModel toModel(FileSetTypeCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		FileSetTypeCodeModel model = new FileSetTypeCodeModel();
		model.setFileSetTypeCode(entity.getFileSetTypeCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
