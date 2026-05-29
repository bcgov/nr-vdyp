package ca.bc.gov.nrs.vdyp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.backend.config.ApiGatewayPathConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

class ApiGatewayAuthenticationMechanismTest {

	private static final String GATEWAY_CONSUMER_HEADER = "X-Consumer-Username";

	private static final String GATEWAY_JWT_HEADER = "GW-JWT";

	private static final String GATEWAY_JWT = "gateway.jwt";

	private final ApiGatewayJwtVerifier apiGatewayJwtVerifier = mock(ApiGatewayJwtVerifier.class);

	private final ApiGatewayAuthenticationMechanism mechanism = new ApiGatewayAuthenticationMechanism(
			new ApiGatewayPathConfig("/api/token"), apiGatewayJwtVerifier
	);

	@Test
	void testAuthenticateReturnsNullForNonGatewayPath() {
		RoutingContext context = mock(RoutingContext.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(context.request()).thenReturn(request);
		when(request.path()).thenReturn("/api/other");

		SecurityIdentity identity = mechanism.authenticate(context, null).await().indefinitely();

		assertNull(identity);
		verify(request).path();
		verify(request, never()).getHeader(GATEWAY_CONSUMER_HEADER);
		verify(request, never()).getHeader(GATEWAY_JWT_HEADER);
		verifyNoInteractions(apiGatewayJwtVerifier);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void testAuthenticateReturnsNullWhenGatewayConsumerHeaderIsMissingOrBlank(String consumer) {
		RoutingContext context = gatewayContext("/api/token", consumer, null);

		SecurityIdentity identity = mechanism.authenticate(context, null).await().indefinitely();

		assertNull(identity);
		verifyNoInteractions(apiGatewayJwtVerifier);
	}

	@Test
	void testAuthenticateFailsWhenOnlyGatewayJwtHeaderIsPresent() {
		RoutingContext context = gatewayContext("/api/token", null, GATEWAY_JWT);

		assertThrows(
				AuthenticationFailedException.class, () -> mechanism.authenticate(context, null).await().indefinitely()
		);
		verifyNoInteractions(apiGatewayJwtVerifier);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void testAuthenticateFailsWhenGatewayJwtHeaderIsMissingOrBlank(String gatewayJwt) {
		RoutingContext context = gatewayContext("/api/token", "test-consumer", gatewayJwt);

		assertThrows(
				AuthenticationFailedException.class, () -> mechanism.authenticate(context, null).await().indefinitely()
		);
		verifyNoInteractions(apiGatewayJwtVerifier);
	}

	@Test
	void testAuthenticateFailsWhenGatewayJwtDoesNotVerify() throws Exception {
		RoutingContext context = gatewayContext("/api/token", "test-consumer", GATEWAY_JWT);
		when(apiGatewayJwtVerifier.verify(GATEWAY_JWT)).thenThrow(new ParseException("invalid token"));

		assertThrows(
				AuthenticationFailedException.class, () -> mechanism.authenticate(context, null).await().indefinitely()
		);
		verify(apiGatewayJwtVerifier).verify(GATEWAY_JWT);
	}

	@Test
	void testAuthenticateCreatesGatewaySecurityIdentity() throws Exception {
		RoutingContext context = gatewayContext("/api/token/", "test-consumer", GATEWAY_JWT);
		JsonWebToken gatewayClaims = gatewayClaims();
		when(apiGatewayJwtVerifier.verify(GATEWAY_JWT)).thenReturn(gatewayClaims);

		SecurityIdentity identity = mechanism.authenticate(context, null).await().indefinitely();

		assertNotNull(identity);
		assertEquals("test-consumer", identity.getPrincipal().getName());
		assertTrue(identity.hasRole("KONG_API_GATEWAY"));
		assertEquals("gwa", identity.getAttribute("auth_source"));
		assertEquals("test-consumer", identity.getAttribute("gateway_consumer"));
		assertEquals("https://issuer.example", identity.getAttribute("gateway_jwt_issuer"));
		verify(apiGatewayJwtVerifier).verify(GATEWAY_JWT);
	}

	@Test
	void testChallengeAdvertisesBearerAndApiKeyAuthentication() {
		ChallengeData challenge = mechanism.getChallenge(null).await().indefinitely();

		assertEquals(401, challenge.status);
		assertEquals("WWW-Authenticate", challenge.headerName.toString());
		assertEquals("Bearer, ApiKey", challenge.headerContent);
	}

	@Test
	void testPriorityAndCredentialTypes() {
		assertEquals(2000, mechanism.getPriority());
		assertTrue(mechanism.getCredentialTypes().isEmpty());
	}

	private static RoutingContext gatewayContext(String path, String consumer, String gatewayJwt) {
		RoutingContext context = mock(RoutingContext.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(context.request()).thenReturn(request);
		when(request.path()).thenReturn(path);
		when(request.getHeader(GATEWAY_CONSUMER_HEADER)).thenReturn(consumer);
		when(request.getHeader(GATEWAY_JWT_HEADER)).thenReturn(gatewayJwt);
		return context;
	}

	private static JsonWebToken gatewayClaims() {
		JsonWebToken claims = mock(JsonWebToken.class);
		when(claims.getIssuer()).thenReturn("https://issuer.example");
		return claims;
	}
}
