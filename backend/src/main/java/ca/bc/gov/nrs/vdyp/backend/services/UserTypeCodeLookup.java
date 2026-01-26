package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.UserTypeCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.UserTypeCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserTypeCodeLookup extends AbstractCodeTableLookup<UserTypeCodeModel, UserTypeCodeEntity> {

	private static final Logger logger = LoggerFactory.getLogger(UserTypeCodeLookup.class);
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
		mapExternalRolesToUserTypeCodes.put("ADMIN", UserTypeCodeModel.ADMIN);
		mapExternalRolesToUserTypeCodes.put("SYSTEM", UserTypeCodeModel.SYSTEM);
		mapExternalRolesToUserTypeCodes.put("USER", UserTypeCodeModel.USER);
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
		UserTypeCodeModel model = null;
		for (String role : roles) {
			Optional<UserTypeCodeModel> internalCode = findModel(
					mapExternalRolesToUserTypeCodes.getOrDefault(normalize(role), "")
			);
			if (internalCode.isPresent()
					&& (model == null || internalCode.get().getDisplayOrder().compareTo(model.getDisplayOrder()) < 0)) {
				model = internalCode.get();
			}
		}
		return model;
	}
}
