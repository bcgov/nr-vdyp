package ca.bc.gov.nrs.vdyp.backend.filters;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import ca.bc.gov.nrs.vdyp.backend.services.VDYPUserService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION + 1) // run right after auth
public class OidcUserSyncFilter {

	@Inject
	SecurityIdentity identity;

	@Inject
	VDYPUserService userService;

	@ServerRequestFilter
	public void filter(ContainerRequestContext context) {
		if (identity == null || identity.isAnonymous()) {
			return; // skip unauthenticated
		}

		// This will get or create the VDYPUser for this token
		userService.ensureVDYPUserFromSecurityIdentity(identity);
	}
}
