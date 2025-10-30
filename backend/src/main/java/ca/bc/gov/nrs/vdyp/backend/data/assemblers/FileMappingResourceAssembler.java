package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;

public class FileMappingResourceAssembler {

	ProjectionFileSetResourceAssembler pfsra;

	public FileMappingResourceAssembler() {
		pfsra = new ProjectionFileSetResourceAssembler();
	}

	public FileMappingModel toModel(FileMappingEntity entity) {
		if (entity == null) {
			return null;
		}

		FileMappingModel model = new FileMappingModel();
		model.setFileMappingGUID(entity.getFileMappingGUID() == null ? null : entity.getFileMappingGUID().toString());
		model.setComsObjectGUID(entity.getComsObjectGUID() == null ? null : entity.getComsObjectGUID().toString());
		model.setProjectionFileSet(
				entity.getProjectionFileSet() == null ? null : pfsra.toModel(entity.getProjectionFileSet())
		);

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
		entity.setProjectionFileSet(
				model.getProjectionFileSet() == null ? null : pfsra.toEntity(model.getProjectionFileSet())
		);
		return entity;
	}
}
