package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsStopProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopRequestMessage;
import io.nats.client.Connection;
import io.nats.client.Message;

/**
 * Broadcasts a "does anyone own this job?" request over NATS and returns whichever replica's reply comes back first.
 * Only the instance that actually owns the job replies (StopRequestListener); staying silent otherwise means a
 * timed-out request (empty result) reliably means no instance currently owns the job.
 */
@Service
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class StopRemoteGateway {

	private static final Logger logger = LoggerFactory.getLogger(StopRemoteGateway.class);

	private final Connection natsConnection;
	private final NatsStopProperties properties;
	private final ObjectMapper objectMapper;

	public StopRemoteGateway(Connection natsConnection, NatsStopProperties properties, ObjectMapper objectMapper) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public Optional<StopReplyMessage> requestStop(String jobGuid, String projectionGuid) {
		try {
			byte[] payload = objectMapper.writeValueAsBytes(new StopRequestMessage(jobGuid, projectionGuid));

			Message reply = natsConnection.request(properties.subject(), payload, properties.timeout());
			if (reply == null) {
				logger.info(
						"[GUID: {}] No replica responded to the stop broadcast within {}.", jobGuid,
						properties.timeout()
				);
				return Optional.empty();
			}

			String json = new String(reply.getData(), StandardCharsets.UTF_8);
			return Optional.of(objectMapper.readValue(json, StopReplyMessage.class));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error(
					"[GUID: {}] Interrupted while broadcasting stop request over NATS: {}", jobGuid, e.getMessage(), e
			);
			return Optional.empty();
		} catch (Exception e) {
			logger.error("[GUID: {}] Failed to broadcast stop request over NATS: {}", jobGuid, e.getMessage(), e);
			return Optional.empty();
		}
	}
}
