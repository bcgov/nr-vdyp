package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsStopProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopRequestMessage;
import io.nats.client.Connection;

/**
 * Broadcasts a "does anyone own this job?" request over NATS to find the replica that should stop it.
 */
@Service
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class StopRemoteGateway extends AbstractNatsBroadcastGateway<StopReplyMessage> {

	public StopRemoteGateway(Connection natsConnection, NatsStopProperties properties, ObjectMapper objectMapper) {
		super(natsConnection, objectMapper, properties.subject(), properties.timeout(), StopReplyMessage.class);
	}

	public Optional<StopReplyMessage> requestStop(String jobGuid, String projectionGuid) {
		return broadcast(jobGuid, new StopRequestMessage(jobGuid, projectionGuid));
	}
}
