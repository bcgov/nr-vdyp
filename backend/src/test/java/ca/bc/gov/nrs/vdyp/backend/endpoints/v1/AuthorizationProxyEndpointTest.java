package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.config.OidcConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@ExtendWith(MockitoExtension.class)
public class AuthorizationProxyEndpointTest {
	private OidcConfig cfg;
	private WebClient webClient;

	private AuthorizationProxyEndpoint endpoint;

	@BeforeEach
	void setUp() throws Exception {
		cfg = mock(OidcConfig.class);
		webClient = mock(WebClient.class);

		endpoint = new AuthorizationProxyEndpoint(cfg);

		Field f = AuthorizationProxyEndpoint.class.getDeclaredField("client");
		f.setAccessible(true);
		f.set(endpoint, webClient);
	}

	@Test
	void token_injectsClientSecret_postsFormBody_andReturnsUpstreamResponse() {
		when(cfg.clientSecret()).thenReturn("s3cr3t");
		when(cfg.authServerUrl()).thenReturn("https://dev.loginproxy.gov.bc.ca/auth/realms/standard");

		@SuppressWarnings("unchecked")
		HttpRequest<Buffer> req = mock(HttpRequest.class);
		@SuppressWarnings("unchecked")
		HttpResponse<Buffer> upstream = mock(HttpResponse.class);

		when(webClient.postAbs("https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/token"))
				.thenReturn(req);

		when(req.putHeader(eq("Content-Type"), eq("application/x-www-form-urlencoded"))).thenReturn(req);

		ArgumentCaptor<Buffer> bodyCaptor = ArgumentCaptor.forClass(Buffer.class);
		when(req.sendBuffer(bodyCaptor.capture())).thenReturn(Future.succeededFuture(upstream));

		when(upstream.statusCode()).thenReturn(200);
		when(upstream.getHeader("Content-Type")).thenReturn("application/json");
		when(upstream.bodyAsString()).thenReturn("{\"access_token\":\"abc\"}");

		MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
		form.putSingle("grant_type", "authorization_code");
		form.putSingle("code", "a b"); // ensure url-encoding coverage
		form.putSingle("redirect_uri", "http://localhost:5173/");

		UriInfo uriInfo = mock(UriInfo.class); // not used by token(), but required by signature

		Response r = endpoint.token(uriInfo, form);

		// Verify client_secret was injected into form
		assertEquals("s3cr3t", form.getFirst("client_secret"));

		// Verify request setup
		verify(req).putHeader("Content-Type", "application/x-www-form-urlencoded");

		// Verify body contains the expected x-www-form-urlencoded pairs (order-independent)
		String sentBody = bodyCaptor.getValue().toString();
		assertFormBodyContainsAll(
				sentBody, "grant_type=authorization_code", "client_secret=s3cr3t", "code=a+b", // space -> '+'
				"redirect_uri=http%3A%2F%2Flocalhost%3A5173%2F"
		);

		// Verify response mapping
		assertEquals(200, r.getStatus());
		assertEquals("application/json", r.getMediaType().toString());
		assertEquals("{\"access_token\":\"abc\"}", r.getEntity());
	}

	@Test
	void authorize_forwardsWithQueryString_usingSeeOther() {
		when(cfg.authServerUrl()).thenReturn("https://dev.loginproxy.gov.bc.ca/auth/realms/standard");

		UriInfo uriInfo = mock(UriInfo.class);
		when(uriInfo.getRequestUri()).thenReturn(
				URI.create("http://localhost/auth/realms/standard/protocol/openid-connect/auth?foo=bar&x=1")
		);

		Response r = endpoint.authorize(uriInfo);

		assertEquals(303, r.getStatus()); // Response.seeOther => 303
		assertEquals(
				URI.create(
						"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/auth?foo=bar&x=1"
				), r.getLocation()
		);
	}

	@Test
	void forwardEndpoints_buildExpectedRedirectTargets_withAndWithoutQuery() {
		when(cfg.authServerUrl()).thenReturn("https://dev.loginproxy.gov.bc.ca/auth/realms/standard");

		UriInfo withQuery = mock(UriInfo.class);
		when(withQuery.getRequestUri()).thenReturn(URI.create("http://localhost/anything?state=abc"));

		UriInfo noQuery = mock(UriInfo.class);
		when(noQuery.getRequestUri()).thenReturn(URI.create("http://localhost/anything"));

		assertLocation(
				endpoint.forwardConfig(withQuery),
				"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/.well-known/openid-configuration?state=abc"
		);
		assertLocation(
				endpoint.forwardIntrospect(noQuery),
				"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/token/introspect"
		);
		assertLocation(
				endpoint.forwardUserInfo(withQuery),
				"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/userinfo?state=abc"
		);
		assertLocation(
				endpoint.forwardCerts(noQuery),
				"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/certs"
		);
		assertLocation(
				endpoint.forwardLogout(withQuery),
				"https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/logout?state=abc"
		);
	}

	private static void assertLocation(Response r, String expected) {
		assertEquals(303, r.getStatus());
		assertEquals(URI.create(expected), r.getLocation());
	}

	private static void assertFormBodyContainsAll(String body, String... expectedPairs) {
		Set<String> parts = new HashSet<>(Arrays.asList(body.split("&")));
		for (String pair : expectedPairs) {
			assertTrue(parts.contains(pair), "Expected form body to contain: " + pair + " but was: " + body);
		}
	}
}
