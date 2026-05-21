package ca.bc.gov.nrs.vdyp.backend.security;

import java.security.Principal;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.backend.config.ApiGatewayPathConfig;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiGatewayAuthenticationMechanism implements HttpAuthenticationMechanism {

	private static final String GATEWAY_CONSUMER_HEADER = "X-Consumer-Username";

	private final ApiGatewayPathConfig apiGatewayPathConfig;

	public ApiGatewayAuthenticationMechanism(ApiGatewayPathConfig apiGatewayPathConfig) {
		this.apiGatewayPathConfig = apiGatewayPathConfig;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
		var request = context.request();
		if (!apiGatewayPathConfig.isApiGatewayPath(request.path())) {
			return Uni.createFrom().nullItem();
		}

		String consumer = request.getHeader(GATEWAY_CONSUMER_HEADER);

		if (consumer == null || consumer.isBlank()) {
			// Let OIDC bearer-token authentication try next.
			return Uni.createFrom().nullItem();
		}

		Principal principal = () -> consumer;

		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(principal)
				.addRole("KONG_API_GATEWAY").addAttribute("auth_source", "gwa")
				.addAttribute("gateway_consumer", consumer).build();

		return Uni.createFrom().item(identity);
	}

	@Override
	public Uni<ChallengeData> getChallenge(RoutingContext context) {
		return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer, ApiKey"));
	}

	@Override
	public int getPriority() {
		// Let this run before the default OIDC mechanism when gateway headers are present.
		return 2000;
	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
		return Set.of();
	}
}
