package ca.bc.gov.nrs.vdyp.batch.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vdyp.nats.stop")
public record NatsStopProperties(String subject, Duration timeout) {
}
