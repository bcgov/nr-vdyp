package ca.bc.gov.nrs.vdyp.batch.messaging;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

@Configuration
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class NatsConfiguration {

	@Bean(destroyMethod = "close")
	Connection natsConnection(NatsBatchProperties properties) throws IOException, InterruptedException {
		Options.Builder builder = new Options.Builder().server(properties.url());

		if (properties.username() != null && !properties.username().isBlank()) {
			builder.userInfo(properties.username(), properties.password());
		}

		return Nats.connect(builder.build());
	}
}
