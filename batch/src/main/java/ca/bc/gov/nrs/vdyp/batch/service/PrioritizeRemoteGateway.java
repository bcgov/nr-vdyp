package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsPrioritizeProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeRequestMessage;
import io.nats.client.Connection;
import io.nats.client.Message;

/**
 * Broadcasts a "does anyone own this job?" request over NATS and returns whichever replica's reply comes back first.
 * Only the instance that actually owns the job replies (PrioritizeRequestListener); staying silent otherwise means a
 * timed-out request (empty result) reliably means no instance currently owns the job.
 */
@Service
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class PrioritizeRemoteGateway {

	private static final Logger logger = LoggerFactory.getLogger(PrioritizeRemoteGateway.class);

	private final Connection natsConnection;
	private final NatsPrioritizeProperties properties;
	private final ObjectMapper objectMapper;

	public PrioritizeRemoteGateway(
			Connection natsConnection, NatsPrioritizeProperties properties, ObjectMapper objectMapper
	) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public Optional<PrioritizeReplyMessage> requestPrioritize(String jobGuid, String projectionGuid) {
		try {
			byte[] payload = objectMapper.writeValueAsBytes(new PrioritizeRequestMessage(jobGuid, projectionGuid));

			Message reply = natsConnection.request(properties.subject(), payload, properties.timeout());
			if (reply == null) {
				logger.info(
						"[GUID: {}] No replica responded to the prioritize broadcast within {}.", jobGuid,
						properties.timeout()
				);
				return Optional.empty();
			}

			String json = new String(reply.getData(), StandardCharsets.UTF_8);
			return Optional.of(objectMapper.readValue(json, PrioritizeReplyMessage.class));
		} catch (Exception e) {
			logger.error("[GUID: {}] Failed to broadcast prioritize request over NATS: {}", jobGuid, e.getMessage(), e);
			return Optional.empty();
		}
	}
}
