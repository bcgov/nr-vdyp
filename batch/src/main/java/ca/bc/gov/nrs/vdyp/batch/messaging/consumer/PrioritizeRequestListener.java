package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsPrioritizeProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService.PrioritizeOutcome;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

/**
 * Answers "does this instance own job X?" broadcasts from PrioritizeRemoteGateway. Only replies when this instance
 * actually owns the job and it's running - staying silent otherwise is what lets the requester's timeout mean "no
 * instance owns this job" without any extra bookkeeping.
 */
@Component
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class PrioritizeRequestListener implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(PrioritizeRequestListener.class);

	private final Connection natsConnection;
	private final NatsPrioritizeProperties properties;
	private final ObjectMapper objectMapper;
	private final JobExecutionLookupService lookupService;
	private final JobOwnershipService ownershipService;
	private final BatchPrioritizationService prioritizationService;

	private volatile Dispatcher dispatcher;

	public PrioritizeRequestListener(
			Connection natsConnection, NatsPrioritizeProperties properties, ObjectMapper objectMapper,
			JobExecutionLookupService lookupService, JobOwnershipService ownershipService,
			BatchPrioritizationService prioritizationService
	) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.lookupService = lookupService;
		this.ownershipService = ownershipService;
		this.prioritizationService = prioritizationService;
	}

	@Override
	public void start() {
		dispatcher = natsConnection.createDispatcher(this::handleMessage);
		dispatcher.subscribe(properties.subject());
	}

	void handleMessage(Message message) {
		String replyTo = message.getReplyTo();
		if (replyTo == null) {
			return;
		}

		try {
			String json = new String(message.getData(), StandardCharsets.UTF_8);
			PrioritizeRequestMessage request = objectMapper.readValue(json, PrioritizeRequestMessage.class);

			if (!ownershipService.isOwnedLocally(request.projectionGuid())) {
				return;
			}

			JobExecution targetExecution;
			try {
				targetExecution = lookupService
						.findJobExecutionByJobParameter(BatchConstants.Job.GUID, request.jobGuid(), true);
			} catch (NoSuchJobExecutionException e) {
				return;
			}

			if (!targetExecution.getStatus().isRunning()) {
				return;
			}

			PrioritizeOutcome outcome = prioritizationService.prioritizeLocally(request.jobGuid(), targetExecution);
			reply(
					replyTo,
					new PrioritizeReplyMessage(
							true, outcome.status(), outcome.message(), outcome.othersPausedCount(),
							outcome.targetExecutionId()
					)
			);
		} catch (Exception e) {
			logger.warn("Failed to handle prioritize broadcast request: {}", e.getMessage(), e);
		}
	}

	private void reply(String replyTo, PrioritizeReplyMessage reply) {
		try {
			byte[] payload = objectMapper.writeValueAsBytes(reply);
			natsConnection.publish(replyTo, payload);
		} catch (Exception e) {
			logger.warn("Failed to publish prioritize reply: {}", e.getMessage(), e);
		}
	}

	@Override
	public void stop() {
		if (dispatcher != null) {
			natsConnection.closeDispatcher(dispatcher);
			dispatcher = null;
		}
	}

	@Override
	public boolean isRunning() {
		return dispatcher != null;
	}
}
