package ca.bc.gov.nrs.vdyp.backend.messaging.publisher;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.messaging.message.BatchRequestMessage;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BatchJobPublisher {

	private final ObjectMapper objectMapper;
	private final String url;
	private final String username;
	private final String password;
	private final String subject;

	@Inject
	public BatchJobPublisher(
			ObjectMapper objectMapper, @ConfigProperty(name = "vdyp.nats.url") String url,
			@ConfigProperty(name = "vdyp.nats.username") Optional<String> username,
			@ConfigProperty(name = "vdyp.nats.password") Optional<String> password,
			@ConfigProperty(name = "vdyp.nats.subject") String subject
	) {
		this.objectMapper = objectMapper;
		this.url = url;
		this.username = username.filter(value -> !value.isBlank()).orElse("");
		this.password = password.orElse("");
		this.subject = subject;
	}

	public void publish(BatchRequestMessage message) {
		Options.Builder builder = new Options.Builder().server(url).connectionName("vdyp-backend-batch-publisher");

		if (username != null && !username.isBlank()) {
			builder.userInfo(username, password == null ? "" : password);
		}

		try (Connection connection = Nats.connect(builder.build())) {
			connection.jetStream().publish(subject, objectMapper.writeValueAsBytes(message));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while publishing NATS batch request", e);
		} catch (IOException | JetStreamApiException e) {
			throw new IllegalStateException("Unable to publish NATS batch request", e);
		}
	}
}
