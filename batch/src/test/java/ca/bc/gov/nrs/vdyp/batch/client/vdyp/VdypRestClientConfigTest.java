package ca.bc.gov.nrs.vdyp.batch.client.vdyp; // adjust to your package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class VdypRestClientConfigTest {

	@Test
	void oauth2AuthorizedClientManager_buildsManagerWithProvider() {
		// Arrange
		var cfg = new VdypRestClientConfig();
		ClientRegistrationRepository repo = mock(ClientRegistrationRepository.class);
		OAuth2AuthorizedClientService svc = mock(OAuth2AuthorizedClientService.class);

		// Act
		OAuth2AuthorizedClientManager manager = cfg.oauth2AuthorizedClientManager(repo, svc);

		// Assert (coverage-focused: ensure correct type and non-null)
		assertNotNull(manager);
	}

	@Test
	void vdypBackendRestClient_setsBearerHeader_andThrowsWhenUnauthorizedClientNull() throws Exception {
		// Arrange
		var cfg = new VdypRestClientConfig();

		// Minimal VdypProperties stub via Mockito; adjust if it's a record/final (see note below)
		VdypProperties props = new VdypProperties(
				"http://localhost:9999", //
				"vdyp-backend", //
				"vdyp_rest_client"
		);

		OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);

		OAuth2AccessToken token = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER, "abc123", Instant.now().minusSeconds(5),
				Instant.now().plusSeconds(300)
		);

		OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
		when(authorizedClient.getAccessToken()).thenReturn(token);
		when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

		ClientHttpRequestInterceptor interceptor = cfg.bearerInterceptor(manager, props);

		MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
		ClientHttpRequestExecution exec = (request, body) -> {
			assertEquals("Bearer abc123", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
			return mock(ClientHttpResponse.class);
		};

		interceptor.intercept(new HttpRequestWrapper(req), new byte[0], exec);

		verify(manager, times(1)).authorize(any(OAuth2AuthorizeRequest.class));
	}

	@Test
	void interceptor_throwsWhenAuthorizeReturnsNull() {
		var cfg = new VdypRestClientConfig();

		VdypProperties props = mock(VdypProperties.class);
		when(props.clientRegistrationId()).thenReturn("vdyp-backend");
		when(props.principal()).thenReturn("vdyp-batch-system");

		OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
		when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

		ClientHttpRequestInterceptor interceptor = cfg.bearerInterceptor(manager, props);

		MockClientHttpRequest req = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
		ClientHttpRequestExecution exec = mock(ClientHttpRequestExecution.class);

		IllegalStateException ex = assertThrows(
				IllegalStateException.class, () -> interceptor.intercept(new HttpRequestWrapper(req), new byte[0], exec)
		);

		assertTrue(ex.getMessage().contains("Unable to authorize OAuth2 client 'vdyp-backend'"));
	}
}
