package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;

public class CalculationEngineResourceAssembler {
	public CalculationEngineResourceAssembler() {
	}

	public CalculationEngineCodeEntity toEntity(CalculationEngineCodeModel model) {
		if (model == null) {
			return null;
		}

		CalculationEngineCodeEntity entity = new CalculationEngineCodeEntity();
		entity.setCalculationEngineCode(model.getCalculationEngineCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public CalculationEngineCodeModel toModel(CalculationEngineCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		CalculationEngineCodeModel model = new CalculationEngineCodeModel();
		model.setCalculationEngineCode(entity.getCalculationEngineCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
