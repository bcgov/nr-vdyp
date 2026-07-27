package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import ca.bc.gov.nrs.vdyp.backend.data.entities.IdentityProviderCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.IdentityProviderCodeModel;
import jakarta.enterprise.context.Dependent;

@Dependent
public class IdentityProviderCodeResourceAssembler
		extends CodeTableResourceAssembler<IdentityProviderCodeEntity, IdentityProviderCodeModel> {

	public IdentityProviderCodeResourceAssembler() {
		super(IdentityProviderCodeEntity::new, IdentityProviderCodeModel::new);
	}

}
