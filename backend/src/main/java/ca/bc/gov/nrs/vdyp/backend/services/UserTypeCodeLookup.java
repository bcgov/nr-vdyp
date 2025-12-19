package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.UserTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.UserTypeCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserTypeCodeLookup extends AbstractCodeTableLookup<UserTypeCodeModel, UserTypeCodeEntity> {

	UserTypeCodeRepository repository;
	UserTypeCodeResourceAssembler assembler;

	private Map<String, String> mapExternalRolesToUserTypeCodes;

	public UserTypeCodeLookup(UserTypeCodeRepository repository, UserTypeCodeResourceAssembler assembler) {
		this.repository = repository;
		this.assembler = assembler;
		ensureExternalRoleMap();
	}

	private void ensureExternalRoleMap() {
		mapExternalRolesToUserTypeCodes = new HashMap<>();
		mapExternalRolesToUserTypeCodes.put("Admin", "ADMIN");
		mapExternalRolesToUserTypeCodes.put("Super User", "SUPERUSER");
	}

	@Override
	protected Stream<UserTypeCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	@Override
	protected Stream<UserTypeCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}

	public UserTypeCodeModel getUserTypeCodeFromExternalRoles(Set<String> roles) {
		// default to user only get replace if user has higher level role
		UserTypeCodeModel model = requireModel("USER");
		for (String role : roles) {
			Optional<UserTypeCodeModel> internalCode = findModel(
					mapExternalRolesToUserTypeCodes.getOrDefault(role, "")
			);
			if (internalCode.isPresent()
					&& internalCode.get().getDisplayOrder().compareTo(model.getDisplayOrder()) < 0) {
				model = internalCode.get();
			}
		}
		return model;
	}
}
