package ca.bc.gov.nrs.vdyp.backend.security;

import java.security.Principal;
import java.util.function.Supplier;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.services.IdentityProviderCodeLookup;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRolesAugmentor implements SecurityIdentityAugmentor {

	private static final Logger logger = LoggerFactory.getLogger(UserRolesAugmentor.class);

	public UserRolesAugmentor() {
	}

	@Override
	public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
		return Uni.createFrom().item(build(identity));
	}

	private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
		Principal identityToken = identity.getPrincipal();

		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

		if (identityToken instanceof JsonWebToken jwt) {
			String idp = jwt.getClaim("identity_provider");
			if (IdentityProviderCodeLookup.isUserProvider(idp)) {
				builder.addRole("USER");
			}
		}
		return builder::build;
	}
}
