package ca.bc.gov.nrs.vdyp.backend.security;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.config.ApiGatewayPathConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity.Builder;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiGatewayAuthenticationMechanism implements HttpAuthenticationMechanism {

	private static final Logger logger = LoggerFactory.getLogger(ApiGatewayAuthenticationMechanism.class);

	private static final String GATEWAY_CONSUMER_HEADER = "X-Consumer-Username";

	private static final String GATEWAY_JWT_HEADER = "GW-JWT";

	private final ApiGatewayPathConfig apiGatewayPathConfig;

	private final ApiGatewayJwtVerifier apiGatewayJwtVerifier;

	public ApiGatewayAuthenticationMechanism(
			ApiGatewayPathConfig apiGatewayPathConfig, ApiGatewayJwtVerifier apiGatewayJwtVerifier
	) {
		this.apiGatewayPathConfig = apiGatewayPathConfig;
		this.apiGatewayJwtVerifier = apiGatewayJwtVerifier;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
		var request = context.request();
		if (!apiGatewayPathConfig.isApiGatewayPath(request.path())) {
			return Uni.createFrom().nullItem();
		}

		String consumer = request.getHeader(GATEWAY_CONSUMER_HEADER);
		String gatewayJwt = request.getHeader(GATEWAY_JWT_HEADER);

		String useConsumer = isBlank(consumer) ? "Unknown API Gateway Consumer" : consumer;

		if (isBlank(gatewayJwt)) {
			return Uni.createFrom().failure(new AuthenticationFailedException());
		}

		JsonWebToken gatewayClaims;
		try {
			gatewayClaims = apiGatewayJwtVerifier.verify(gatewayJwt);
		} catch (Exception e) {
			logger.warn("API gateway JWT verification failed: {}", e.getMessage());
			logger.debug("API gateway JWT verification failure details", e);
			return Uni.createFrom().failure(new AuthenticationFailedException(e));
		}

		Principal principal = () -> useConsumer;

		Builder identityBuilder = QuarkusSecurityIdentity.builder().setPrincipal(principal).addRole("KONG_API_GATEWAY")
				.addAttribute("auth_source", "gwa").addAttribute("gateway_consumer", useConsumer);
		if (gatewayClaims != null && gatewayClaims.getIssuer() != null) {
			identityBuilder.addAttribute("gateway_jwt_issuer", gatewayClaims.getIssuer());
		}
		logger.info("JWT is valid");
		SecurityIdentity identity = identityBuilder.build();

		logger.info("Created identity: {}", identity);

		logger.info(
				"Current SecurityIdentity principal={}, anonymous={}, roles={}",
				identity.getPrincipal() != null ? identity.getPrincipal().getName() : null, identity.isAnonymous(),
				identity.getRoles()
		);

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

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
