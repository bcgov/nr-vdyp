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
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
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

	private VDYPUserModel systemUserCache = null;

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
				Set<String> roles = identity.getRoles();
				UserTypeCodeModel userType = userTypeLookup.getUserTypeCodeFromExternalRoles(roles);
				if (!userType.isSystemUser()) {
					String firstName = jwt.getClaim("given_name");
					String lastName = jwt.getClaim("family_name");
					String displayName = jwt.getClaim("display_name");
					String email = jwt.getClaim("email");

					VDYPUserModel newUser = new VDYPUserModel();
					newUser.setUserTypeCode(userType);
					newUser.setOidcGUID(oidcId);
					newUser.setFirstName(firstName);
					newUser.setLastName(lastName);
					newUser.setDisplayName(displayName);
					newUser.setEmail(email);

					logger.debug(
							"Creating new user with oidcId {}, firstName {}, lastName {}, usertypeCode {}", oidcId,
							firstName, lastName, newUser.getUserTypeCode().getCode()
					);

					return createUser(newUser);
				} else {
					return getSystemUser();
				}
			}
			return assembler.toModel(userOption.get());
		} else {
			return null;
		}
	}

	public VDYPUserModel getSystemUser() {
		if (systemUserCache == null) {
			systemUserCache = new VDYPUserModel();
			systemUserCache.setOidcGUID("system");
			systemUserCache.setFirstName("System");
			systemUserCache.setLastName("User");
			systemUserCache.setUserTypeCode(userTypeLookup.requireModel(UserTypeCodeModel.SYSTEM));
		}
		return systemUserCache;
	}
}
