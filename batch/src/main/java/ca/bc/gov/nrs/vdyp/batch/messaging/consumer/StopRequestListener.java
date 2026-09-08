package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsStopProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchStopService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchStopService.StopOutcome;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import io.nats.client.Connection;
import io.nats.client.Message;

/**
 * Answers "does this instance own job X?" broadcasts from StopRemoteGateway. Only replies when this instance actually
 * owns the job - staying silent otherwise is what lets the requester's timeout mean "no instance owns this job" without
 * any extra bookkeeping.
 */
@Component
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class StopRequestListener extends AbstractNatsBroadcastListener {

	private static final Logger logger = LoggerFactory.getLogger(StopRequestListener.class);

	private final BatchStopService stopService;

	public StopRequestListener(
			Connection natsConnection, NatsStopProperties properties, ObjectMapper objectMapper,
			JobExecutionLookupService lookupService, JobOwnershipService ownershipService, BatchStopService stopService
	) {
		super(natsConnection, objectMapper, properties.subject(), ownershipService, lookupService);
		this.stopService = stopService;
	}

	@Override
	void handleMessage(Message message) {
		String replyTo = message.getReplyTo();
		if (replyTo == null) {
			return;
		}

		try {
			String json = new String(message.getData(), StandardCharsets.UTF_8);
			StopRequestMessage request = objectMapper.readValue(json, StopRequestMessage.class);

			Optional<JobExecution> targetExecution = getJobExecutionForProjectionGuid(
					request.projectionGuid(), request.jobGuid()
			);
			if (targetExecution.isEmpty()) {
				return;
			}

			StopOutcome outcome = stopService.stopLocally(request.jobGuid(), targetExecution.get());
			reply(replyTo, new StopReplyMessage(outcome.status(), outcome.message(), outcome.executionId()));
		} catch (Exception e) {
			logger.warn("Failed to handle stop broadcast request: {}", e.getMessage(), e);
		}
	}
}
