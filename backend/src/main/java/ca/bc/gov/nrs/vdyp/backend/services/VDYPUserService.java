package ca.bc.gov.nrs.vdyp.backend.services;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.codehaus.plexus.util.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.VDYPUserResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VDYPUserService {

	private static final Logger logger = LoggerFactory.getLogger(VDYPUserService.class);
	private final VDYPUserRepository userRepository;
	private final VDYPUserResourceAssembler assembler;
	private final UserTypeCodeLookup userTypeLookup;

	public VDYPUserService(
			VDYPUserRepository userRepository, VDYPUserResourceAssembler assembler, UserTypeCodeLookup userTypeLookup
	) {
		this.userRepository = userRepository;
		this.assembler = assembler;
		this.userTypeLookup = userTypeLookup;

	}

	public List<VDYPUserModel> getAllUsers() {
		return userRepository.listAll().stream().map(assembler::toModel).toList();
	}

	@Transactional
	public VDYPUserModel createUser(VDYPUserModel requestedUser) {
		// Confirm the requested User can be safely created
		if (StringUtils.isBlank(requestedUser.getOidcGUID())) {
			throw new IllegalArgumentException("Invalid User Business Identifier");
		}

		logger.debug("Checking for user with oidcId {}", requestedUser.getOidcGUID());
		userRepository.findByOIDC(requestedUser.getOidcGUID()).ifPresent(existing -> {
			throw new IllegalArgumentException("User already exists");
		});

		VDYPUserEntity entity = assembler.toEntity(requestedUser);
		userRepository.persist(entity);
		return assembler.toModel(entity);
	}

	public VDYPUserModel getUserById(UUID userId) {
		VDYPUserEntity entity = userRepository.findById(userId);
		return assembler.toModel(entity);
	}

	/**
	 * Ensure that a Security Claim has been provided
	 * <p>
	 *
	 * @param identity the SecurityIdentity containing claims for the api
	 *
	 * @return A VDYPUserModel reprenting the user if they exist or null if there was an issue or the user could not be
	 *         created
	 */
	@Transactional
	public VDYPUserModel ensureVDYPUserFromSecurityIdentity(SecurityIdentity identity) {
		if (identity == null || identity.isAnonymous()) {
			return null;
		}
		Principal identityToken = identity.getPrincipal();
		if (identityToken instanceof JsonWebToken jwt) {
			String oidcId = jwt.getName();
			logger.debug("Checking for user with oidcId {}", oidcId);
			Optional<VDYPUserEntity> userOption = userRepository.findByOIDC(oidcId);
			if (userOption.isEmpty()) {
				String firstName = jwt.getClaim("given_name");
				String lastName = jwt.getClaim("family_name");
				// FIXME VDYP-847 Record email and Display Name to our system

				VDYPUserModel newUser = new VDYPUserModel();
				newUser.setOidcGUID(oidcId);
				newUser.setFirstName(firstName);
				newUser.setLastName(lastName);

				Set<String> roles = identity.getRoles();
				newUser.setUserTypeCode(userTypeLookup.getUserTypeCodeFromExternalRoles(roles));
				logger.debug(
						"Creating new user with oidcId {}, firstName {}, lastName {}, usertypeCode {}", oidcId,
						firstName, lastName, newUser.getUserTypeCode().getCode()
				);

				return createUser(newUser);
			}
			return assembler.toModel(userOption.get());
		} else {
			return null;
		}
	}
}
