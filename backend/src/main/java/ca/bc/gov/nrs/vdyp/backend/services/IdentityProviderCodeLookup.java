package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.IdentityProviderCodeResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.IdentityProviderCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.IdentityProviderCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.IdentityProviderCodeRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdentityProviderCodeLookup
		extends AbstractCodeTableLookup<IdentityProviderCodeModel, IdentityProviderCodeEntity> {

	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderCodeLookup.class);
	IdentityProviderCodeRepository repository;
	IdentityProviderCodeResourceAssembler assembler;

	private Map<String, String> mapClaimValueToIdentityProviderCode;

	public IdentityProviderCodeLookup(
			IdentityProviderCodeRepository repository, IdentityProviderCodeResourceAssembler assembler
	) {
		this.repository = repository;
		this.assembler = assembler;
		ensureClaimValueMap();
	}

	private void ensureClaimValueMap() {
		mapClaimValueToIdentityProviderCode = new HashMap<>();
		mapClaimValueToIdentityProviderCode.put("AZUREIDIR", IdentityProviderCodeModel.IDIR);
		mapClaimValueToIdentityProviderCode.put("BCEIDBUSINESS", IdentityProviderCodeModel.BCEID);
	}

	@Override
	protected Stream<IdentityProviderCodeModel> loadAllModels() {
		return repository.listAll().stream().map(assembler::toModel);
	}

	@Override
	protected Stream<IdentityProviderCodeEntity> loadAllEntities() {
		return repository.listAll().stream();
	}

	/**
	 * Maps a JWT {@code identity_provider} claim value (e.g. {@code azureidir}, {@code bceidbusiness}) to the
	 * corresponding {@link IdentityProviderCodeModel}.
	 *
	 * @param identityProviderClaim the raw claim value from the JWT, may be null
	 *
	 * @return the matching model, or empty if the claim is null or unrecognized
	 */
	public Optional<IdentityProviderCodeModel> getIdentityProviderCodeFromClaim(String identityProviderClaim) {
		if (identityProviderClaim == null) {
			logger.debug("No identity_provider claim found");
			return Optional.empty();
		}
		return findModel(mapClaimValueToIdentityProviderCode.getOrDefault(normalize(identityProviderClaim), ""));
	}
}
