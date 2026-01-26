package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestClient;

@Configuration
public class VdypRestClientConfig {
	@Bean
	RestClient vdypBackendRestClient(
			OAuth2AuthorizedClientManager oauth2AuthorizedClientManager, VdypProperties vdypProperties
	) {
		return RestClient.builder().baseUrl(vdypProperties.baseUrl())
				.requestInterceptor(bearerInterceptor(oauth2AuthorizedClientManager, vdypProperties)).build();
	}

	@Bean
	OAuth2AuthorizedClientManager oauth2AuthorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientService authorizedClientService
	) {
		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials()
				.build();

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientService
		);
		manager.setAuthorizedClientProvider(provider);
		return manager;
	}

	ClientHttpRequestInterceptor bearerInterceptor(
			OAuth2AuthorizedClientManager oauth2AuthorizedClientManager, VdypProperties vdypProperties
	) {
		Authentication principal = new AnonymousAuthenticationToken(
				vdypProperties.principal(), vdypProperties.principal(),
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM"))
		);

		return (request, body, execution) -> {
			OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
					.withClientRegistrationId(vdypProperties.clientRegistrationId()).principal(principal).build();

			OAuth2AuthorizedClient client = oauth2AuthorizedClientManager.authorize(authorizeRequest);
			if (client == null) {
				throw new IllegalStateException(
						"Unable to authorize OAuth2 client '" + vdypProperties.clientRegistrationId() + "'"
				);
			}
			OAuth2AccessToken token = client.getAccessToken();
			request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue());
			return execution.execute(request, body);
		};
	}
}
