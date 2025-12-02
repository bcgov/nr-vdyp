package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;

public class UserTypeCodeResourceAssembler {

	public UserTypeCodeEntity toEntity(UserTypeCodeModel model) {
		if (model == null) {
			return null;
		}

		UserTypeCodeEntity entity = new UserTypeCodeEntity();
		entity.setUserTypeCode(model.getUserTypeCode());
		entity.setDescription(model.getDescription());
		entity.setDisplayOrder(model.getDisplayOrder());
		return entity;
	}

	public UserTypeCodeModel toModel(UserTypeCodeEntity entity) {
		if (entity == null) {
			return null;
		}
		UserTypeCodeModel model = new UserTypeCodeModel();
		model.setUserTypeCode(entity.getUserTypeCode());
		model.setDescription(entity.getDescription());
		model.setDisplayOrder(entity.getDisplayOrder());
		return model;
	}
}
