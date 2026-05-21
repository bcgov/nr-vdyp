package ca.bc.gov.nrs.vdyp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.backend.config.ApiGatewayPathConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

class ApiGatewayAuthenticationMechanismTest {

	private static final String GATEWAY_CONSUMER_HEADER = "X-Consumer-Username";

	private final ApiGatewayAuthenticationMechanism mechanism = new ApiGatewayAuthenticationMechanism(
			new ApiGatewayPathConfig("/api/token")
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
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void testAuthenticateReturnsNullWhenGatewayConsumerHeaderIsMissingOrBlank(String consumer) {
		RoutingContext context = gatewayContext("/api/token", consumer);

		SecurityIdentity identity = mechanism.authenticate(context, null).await().indefinitely();

		assertNull(identity);
	}

	@Test
	void testAuthenticateCreatesGatewaySecurityIdentity() {
		RoutingContext context = gatewayContext("/api/token/", "test-consumer");

		SecurityIdentity identity = mechanism.authenticate(context, null).await().indefinitely();

		assertNotNull(identity);
		assertEquals("test-consumer", identity.getPrincipal().getName());
		assertTrue(identity.hasRole("KONG_API_GATEWAY"));
		assertEquals("gwa", identity.getAttribute("auth_source"));
		assertEquals("test-consumer", identity.getAttribute("gateway_consumer"));
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

	private static RoutingContext gatewayContext(String path, String consumer) {
		RoutingContext context = mock(RoutingContext.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(context.request()).thenReturn(request);
		when(request.path()).thenReturn(path);
		when(request.getHeader(GATEWAY_CONSUMER_HEADER)).thenReturn(consumer);
		return context;
	}
}
