package ca.bc.gov.nrs.vdyp.backend.filters;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import ca.bc.gov.nrs.vdyp.backend.config.ApiGatewayPathConfig;
import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.services.VDYPUserService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION + 1) // run right after auth
public class OidcUserSyncFilter {

	SecurityIdentity identity;

	VDYPUserService userService;

	CurrentVDYPUser currentUser;

	ApiGatewayPathConfig apiGatewayPathConfig;

	public OidcUserSyncFilter(
			SecurityIdentity identity, VDYPUserService userService, CurrentVDYPUser currentUser,
			ApiGatewayPathConfig apiGatewayPathConfig
	) {
		this.identity = identity;
		this.userService = userService;
		this.currentUser = currentUser;
		this.apiGatewayPathConfig = apiGatewayPathConfig;
	}

	@ServerRequestFilter
	public void filter(ContainerRequestContext context) {
		if (identity == null || identity.isAnonymous()) {
			return; // skip unauthenticated
		}

		String path = context.getUriInfo().getPath();
		// Users Authorized as API Gateway users do not have VDYP User Information and can only access limited endpoints
		if (identity.hasRole("KONG_API_GATEWAY") && apiGatewayPathConfig.isApiGatewayPath(path)) {
			return;
		}

		// This will get or create the VDYPUser for this token
		var vdypUser = userService.ensureVDYPUserFromSecurityIdentity(identity);
		// set it for the request context
		currentUser.setUser(vdypUser);
	}
}
