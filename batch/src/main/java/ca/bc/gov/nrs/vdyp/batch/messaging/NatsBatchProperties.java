package ca.bc.gov.nrs.vdyp.batch.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vdyp.nats")
public record NatsBatchProperties(
		String url, String username, String password, String stream, String consumer, String subject,
		String statusSubject, boolean enabled, Duration pollTimeout, int batchSize
) {
}
