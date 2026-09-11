package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsPrioritizeProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeRequestMessage;
import io.nats.client.Connection;

/**
 * Broadcasts a "does anyone own this job?" request over NATS to find the replica that should prioritize it.
 */
@Service
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class PrioritizeRemoteGateway extends AbstractNatsBroadcastGateway<PrioritizeReplyMessage> {

	public PrioritizeRemoteGateway(
			Connection natsConnection, NatsPrioritizeProperties properties, ObjectMapper objectMapper
	) {
		super(natsConnection, objectMapper, properties.subject(), properties.timeout(), PrioritizeReplyMessage.class);
	}

	public Optional<PrioritizeReplyMessage> requestPrioritize(String jobGuid, String projectionGuid) {
		return broadcast(jobGuid, new PrioritizeRequestMessage(jobGuid, projectionGuid));
	}
}
