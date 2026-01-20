package ca.bc.gov.nrs.vdyp.backend.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.oidc")
public interface OidcConfig {
	String authServerUrl();

	String clientSecret();
}
