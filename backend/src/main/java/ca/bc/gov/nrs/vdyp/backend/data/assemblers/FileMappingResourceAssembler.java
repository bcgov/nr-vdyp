package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;

public class FileMappingResourceAssembler {

	public FileMappingResourceAssembler() {

	}

	public FileMappingModel toModel(FileMappingEntity entity) {
		if (entity == null) {
			return null;
		}

		FileMappingModel model = new FileMappingModel();
		model.setFileMappingGUID(entity.getFileMappingGUID() == null ? null : entity.getFileMappingGUID().toString());
		model.setComsObjectGUID(entity.getComsObjectGUID() == null ? null : entity.getComsObjectGUID().toString());
		return model;
	}

	public FileMappingEntity toEntity(FileMappingModel model) {
		if (model == null) {
			return null;
		}

		FileMappingEntity entity = new FileMappingEntity();
		entity.setFileMappingGUID(
				model.getFileMappingGUID() == null ? null : UUID.fromString(model.getFileMappingGUID())
		);
		entity.setComsObjectGUID(model.getComsObjectGUID() == null ? null : UUID.fromString(model.getComsObjectGUID()));
		return entity;
	}
}
