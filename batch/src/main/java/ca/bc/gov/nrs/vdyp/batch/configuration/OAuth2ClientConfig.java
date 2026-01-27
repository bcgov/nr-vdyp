package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration
public class OAuth2ClientConfig {
	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository registrations, OAuth2AuthorizedClientRepository authorizedClients
	) {

		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials()
				.build();

		DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(
				registrations, authorizedClients
		);

		manager.setAuthorizedClientProvider(provider);
		return manager;
	}
}
