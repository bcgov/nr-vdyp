package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import jakarta.enterprise.context.Dependent;

@Dependent
public class UserTypeCodeResourceAssembler extends CodeTableResourceAssembler<UserTypeCodeEntity, UserTypeCodeModel> {

	public UserTypeCodeResourceAssembler() {
		super(UserTypeCodeEntity::new, UserTypeCodeModel::new);
	}

}
