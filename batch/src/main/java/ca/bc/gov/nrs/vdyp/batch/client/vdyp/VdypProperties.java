package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vdyp")
public record VdypProperties(
		String baseUrl, //
		String clientRegistrationId, //
		String principal
) {
}
