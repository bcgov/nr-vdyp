package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;
import io.nats.client.Message;

/**
 * Shared NATS core pub/sub request-reply behaviour for "does anyone own this job?" broadcasts: publishes a request and
 * returns whichever replica's reply comes back first. Only the owning replica's listener ever replies, so a timed-out
 * request (empty result) reliably means no replica currently owns the job.
 */
public abstract class AbstractNatsBroadcastGateway<R> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNatsBroadcastGateway.class);

	private final Connection natsConnection;
	private final ObjectMapper objectMapper;
	private final String subject;
	private final Duration timeout;
	private final Class<R> replyType;

	protected AbstractNatsBroadcastGateway(
			Connection natsConnection, ObjectMapper objectMapper, String subject, Duration timeout, Class<R> replyType
	) {
		this.natsConnection = natsConnection;
		this.objectMapper = objectMapper;
		this.subject = subject;
		this.timeout = timeout;
		this.replyType = replyType;
	}

	protected Optional<R> broadcast(String jobGuid, Object request) {
		try {
			byte[] payload = objectMapper.writeValueAsBytes(request);

			Message reply = natsConnection.request(subject, payload, timeout);
			if (reply == null) {
				logger.info(
						"[GUID: {}] No replica responded to the broadcast on {} within {}.", jobGuid, subject, timeout
				);
				return Optional.empty();
			}

			String json = new String(reply.getData(), StandardCharsets.UTF_8);
			return Optional.of(objectMapper.readValue(json, replyType));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error(
					"[GUID: {}] Interrupted while broadcasting request over NATS on {}: {}", jobGuid, subject,
					e.getMessage(), e
			);
			return Optional.empty();
		} catch (Exception e) {
			logger.error(
					"[GUID: {}] Failed to broadcast request over NATS on {}: {}", jobGuid, subject, e.getMessage(), e
			);
			return Optional.empty();
		}
	}
}
