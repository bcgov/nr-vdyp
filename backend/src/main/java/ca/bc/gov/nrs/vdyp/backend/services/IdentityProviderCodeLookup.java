package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

	public static final String AZUREIDIR_IDP = "AZUREIDIR";
	public static final String BCEIDBUSINESS_IDP = "BCEIDBUSINESS";
	public static final String BCSC_IDP = "VDYP-SERVICE-6216";

	private Map<String, String> mapClaimValueToIdentityProviderCode;

	private static Set<String> idpWhitelist = Set.of(AZUREIDIR_IDP, BCEIDBUSINESS_IDP, BCSC_IDP);

	public IdentityProviderCodeLookup(
			IdentityProviderCodeRepository repository, IdentityProviderCodeResourceAssembler assembler
	) {
		this.repository = repository;
		this.assembler = assembler;
		ensureClaimValueMap();
	}

	public static boolean isUserProvider(String idp) {
		if (idp == null)
			return false;

		return idpWhitelist.contains(idp.toUpperCase());
	}

	private void ensureClaimValueMap() {
		mapClaimValueToIdentityProviderCode = new HashMap<>();
		mapClaimValueToIdentityProviderCode.put(AZUREIDIR_IDP, IdentityProviderCodeModel.IDIR);
		mapClaimValueToIdentityProviderCode.put(BCEIDBUSINESS_IDP, IdentityProviderCodeModel.BCEID);
		mapClaimValueToIdentityProviderCode.put(BCSC_IDP, IdentityProviderCodeModel.BCSC);
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
