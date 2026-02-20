package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VDYPUserResourceAssembler {

	UserTypeCodeResourceAssembler utra;

	public VDYPUserResourceAssembler() {
		utra = new UserTypeCodeResourceAssembler();
	}

	public VDYPUserEntity toEntity(VDYPUserModel model) {
		if (model == null) {
			return null;
		}

		VDYPUserEntity entity = new VDYPUserEntity();
		entity.setVdypUserGUID(model.getVdypUserGUID() == null ? null : UUID.fromString(model.getVdypUserGUID()));
		entity.setOidcGUID(model.getOidcGUID());
		entity.setUserTypeCode(utra.toEntity(model.getUserTypeCode()));
		entity.setFirstName(model.getFirstName());
		entity.setLastName(model.getLastName());
		entity.setDisplayName(model.getDisplayName());
		entity.setEmail(model.getEmail());

		return entity;
	}

	public VDYPUserModel toModel(VDYPUserEntity entity) {
		if (entity == null) {
			return null;
		}
		VDYPUserModel model = new VDYPUserModel();
		model.setVdypUserGUID(entity.getVdypUserGUID() == null ? null : entity.getVdypUserGUID().toString());
		model.setOidcGUID(entity.getOidcGUID());
		model.setUserTypeCode(utra.toModel(entity.getUserTypeCode()));
		model.setFirstName(entity.getFirstName());
		model.setLastName(entity.getLastName());
		model.setDisplayName(entity.getDisplayName());
		model.setEmail(entity.getEmail());

		return model;
	}
}
