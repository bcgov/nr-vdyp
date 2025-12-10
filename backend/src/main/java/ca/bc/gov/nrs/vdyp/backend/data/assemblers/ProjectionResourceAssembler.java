package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionResourceAssembler {

	ProjectionFileSetResourceAssembler pfsra;
	CalculationEngineResourceAssembler cera;
	VDYPUserResourceAssembler vura;
	ProjectionStatusCodeResourceAssembler pscra;

	public ProjectionResourceAssembler() {
		pfsra = new ProjectionFileSetResourceAssembler();
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
		entity.setPolygonFileSet(pfsra.toEntity(model.getPolygonFileSet()));
		entity.setLayerFileSet(pfsra.toEntity(model.getLayerFileSet()));
		entity.setResultFileSet(pfsra.toEntity(model.getResultFileSet()));
		entity.setCalculationEngineCode(cera.toEntity(model.getCalculationEngineCode()));
		entity.setProjectionStatusCode(pscra.toEntity(model.getProjectionStatusCode()));
		entity.setStartDate(model.getStartDate());
		entity.setEndDate(model.getEndDate());
		entity.setOwnerUser(vura.toEntity(model.getOwnerUser()));
		entity.setReportDescription(model.getReportDescription());
		entity.setReportTitle(model.getReportTitle());
		return entity;
	}

	public ProjectionModel toModel(ProjectionEntity entity) {
		if (entity == null) {
			return null;
		}

		ProjectionModel model = new ProjectionModel();
		model.setProjectionGUID(entity.getProjectionGUID().toString());
		model.setProjectionParameters(entity.getProjectionParameters());
		model.setPolygonFileSet(pfsra.toModel(entity.getPolygonFileSet()));
		model.setLayerFileSet(pfsra.toModel(entity.getLayerFileSet()));
		model.setResultFileSet(pfsra.toModel(entity.getResultFileSet()));
		model.setCalculationEngineCode(cera.toModel(entity.getCalculationEngineCode()));
		model.setProjectionStatusCode(pscra.toModel(entity.getProjectionStatusCode()));
		model.setStartDate(entity.getStartDate());
		model.setEndDate(entity.getEndDate());
		model.setOwnerUser(vura.toModel(entity.getOwnerUser()));
		model.setProjectionStatusCode(pscra.toModel(entity.getProjectionStatusCode()));
		model.setLastUpdatedDate(entity.getUpdateDate());
		model.setReportTitle(entity.getReportTitle());
		model.setReportDescription(entity.getReportDescription());
		return model;
	}

}
