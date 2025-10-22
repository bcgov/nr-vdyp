package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;

public class ProjectionResourceAssembler {

	FileMappingResourceAssembler fmra;
	CalculationEngineResourceAssembler cera;
	VDYPUserResourceAssembler vura;
	ProjectionStatusCodeResourceAssembler pscra;

	public ProjectionResourceAssembler() {
		fmra = new FileMappingResourceAssembler();
		cera = new CalculationEngineResourceAssembler();
		vura = new VDYPUserResourceAssembler();
		pscra = new ProjectionStatusCodeResourceAssembler();
	}

	public ProjectionEntity toEntity(ProjectionModel model) {
		if (model == null) {
			return null;
		}

		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(UUID.fromString(model.getProjectionGUID()));
		entity.setProjectionParameters(model.getProjectionParameters());
		entity.setPolygonFileMapping(fmra.toEntity(model.getPolygonFileMapping()));
		entity.setLayerFileMapping(fmra.toEntity(model.getLayerFileMapping()));
		entity.setResultFileMapping(fmra.toEntity(model.getResultFileMapping()));
		entity.setCalculationEngineCode(cera.toEntity(model.getCalculationEngineCode()));
		entity.setStartDate(model.getStartDate());
		entity.setEndDate(model.getEndDate());
		entity.setOwnerUser(vura.toEntity(model.getOwnerUser()));
		return entity;
	}

	public ProjectionModel toModel(ProjectionEntity entity) {
		if (entity == null) {
			return null;
		}

		ProjectionModel model = new ProjectionModel();
		model.setProjectionGUID(entity.getProjectionGUID().toString());
		model.setProjectionParameters(entity.getProjectionParameters());
		model.setPolygonFileMapping(fmra.toModel(entity.getPolygonFileMapping()));
		model.setLayerFileMapping(fmra.toModel(entity.getLayerFileMapping()));
		model.setResultFileMapping(fmra.toModel(entity.getResultFileMapping()));
		model.setCalculationEngineCode(cera.toModel(entity.getCalculationEngineCode()));
		model.setStartDate(entity.getStartDate());
		model.setEndDate(entity.getEndDate());
		model.setOwnerUser(vura.toModel(entity.getOwnerUser()));
		model.setProjectionStatusCode(pscra.toModel(entity.getProjectionStatusCode()));
		return model;
	}

}
