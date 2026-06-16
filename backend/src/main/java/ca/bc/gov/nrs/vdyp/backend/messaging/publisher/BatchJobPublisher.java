package ca.bc.gov.nrs.vdyp.backend.messaging.publisher;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.messaging.message.BatchRequestMessage;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.MessageInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BatchJobPublisher {

	private static final Logger logger = LoggerFactory.getLogger(BatchJobPublisher.class);
	private static final int NOT_FOUND = 404;

	private final ObjectMapper objectMapper;
	private final String url;
	private final String username;
	private final String password;
	private final String subject;
	private final String stream;
	private final NatsConnectionFactory connectionFactory;

	@Inject
	public BatchJobPublisher(
			ObjectMapper objectMapper, @ConfigProperty(name = "vdyp.nats.url") String url,
			@ConfigProperty(name = "vdyp.nats.username") Optional<String> username,
			@ConfigProperty(name = "vdyp.nats.password") Optional<String> password,
			@ConfigProperty(name = "vdyp.nats.subject") String subject,
			@ConfigProperty(name = "vdyp.nats.stream", defaultValue = "VDYP_BATCH_REQUESTS") String stream
	) {
		this(objectMapper, url, username, password, subject, stream, Nats::connect);
	}

	BatchJobPublisher(
			ObjectMapper objectMapper, String url, Optional<String> username, Optional<String> password, String subject,
			String stream, NatsConnectionFactory connectionFactory
	) {
		this.objectMapper = objectMapper;
		this.url = url;
		this.username = username.filter(value -> !value.isBlank()).orElse("");
		this.password = password.orElse("");
		this.subject = subject;
		this.stream = stream;
		this.connectionFactory = connectionFactory;
	}

	public void publish(BatchRequestMessage message) {
		try (Connection connection = connect("vdyp-backend-batch-publisher")) {
			connection.jetStream().publish(subject, objectMapper.writeValueAsBytes(message));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while publishing NATS batch request", e);
		} catch (IOException | JetStreamApiException e) {
			throw new IllegalStateException("Unable to publish NATS batch request", e);
		}
	}

	public boolean deleteQueuedRequest(UUID projectionID) {
		try (Connection connection = connect("vdyp-backend-batch-canceller")) {
			JetStreamManagement management = connection.jetStreamManagement();
			MessageInfo messageInfo = firstQueuedMessage(management);

			while (messageInfo != null && messageInfo.isMessage()) {
				if (messageMatchesProjection(messageInfo, projectionID)) {
					boolean deleted = management.deleteMessage(stream, messageInfo.getSeq());
					if (deleted) {
						logger.info("Deleted queued NATS batch request for projection {}", projectionID);
					}
					return deleted;
				}

				messageInfo = nextQueuedMessage(management, messageInfo.getSeq());
			}

			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while deleting queued NATS batch request", e);
		} catch (IOException | JetStreamApiException e) {
			throw new IllegalStateException("Unable to delete queued NATS batch request", e);
		}
	}

	private Connection connect(String connectionName) throws IOException, InterruptedException {
		Options.Builder builder = new Options.Builder().server(url).connectionName(connectionName);

		if (username != null && !username.isBlank()) {
			builder.userInfo(username, password == null ? "" : password);
		}

		return connectionFactory.connect(builder.build());
	}

	private MessageInfo firstQueuedMessage(JetStreamManagement management) throws IOException, JetStreamApiException {
		try {
			return management.getFirstMessage(stream, subject);
		} catch (JetStreamApiException e) {
			if (isNotFound(e)) {
				return null;
			}
			throw e;
		}
	}

	private MessageInfo nextQueuedMessage(JetStreamManagement management, long currentSequence)
			throws IOException, JetStreamApiException {
		try {
			return management.getNextMessage(stream, currentSequence, subject);
		} catch (JetStreamApiException e) {
			if (isNotFound(e)) {
				return null;
			}
			throw e;
		}
	}

	private boolean messageMatchesProjection(MessageInfo messageInfo, UUID projectionID) {
		try {
			BatchRequestMessage message = objectMapper.readValue(messageInfo.getData(), BatchRequestMessage.class);
			return projectionID.equals(message.projectionID());
		} catch (IOException e) {
			logger.warn(
					"Skipping malformed NATS batch request message seq={} stream={} subject={}", messageInfo.getSeq(),
					stream, subject, e
			);
			return false;
		}
	}

	private boolean isNotFound(JetStreamApiException e) {
		return e.getErrorCode() == NOT_FOUND;
	}

	@FunctionalInterface
	interface NatsConnectionFactory {
		Connection connect(Options options) throws IOException, InterruptedException;
	}
}
