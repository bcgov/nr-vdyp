package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsStopProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchStopService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchStopService.StopOutcome;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

/**
 * Answers "does this instance own job X?" broadcasts from StopRemoteGateway. Only replies when this instance actually
 * owns the job - staying silent otherwise is what lets the requester's timeout mean "no instance owns this job" without
 * any extra bookkeeping.
 */
@Component
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class StopRequestListener implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(StopRequestListener.class);

	private final Connection natsConnection;
	private final NatsStopProperties properties;
	private final ObjectMapper objectMapper;
	private final JobExecutionLookupService lookupService;
	private final JobOwnershipService ownershipService;
	private final BatchStopService stopService;

	private final AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();

	public StopRequestListener(
			Connection natsConnection, NatsStopProperties properties, ObjectMapper objectMapper,
			JobExecutionLookupService lookupService, JobOwnershipService ownershipService, BatchStopService stopService
	) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.lookupService = lookupService;
		this.ownershipService = ownershipService;
		this.stopService = stopService;
	}

	@Override
	public void start() {
		Dispatcher newDispatcher = natsConnection.createDispatcher(this::handleMessage);
		newDispatcher.subscribe(properties.subject());
		dispatcher.set(newDispatcher);
	}

	void handleMessage(Message message) {
		String replyTo = message.getReplyTo();
		if (replyTo == null) {
			return;
		}

		try {
			String json = new String(message.getData(), StandardCharsets.UTF_8);
			StopRequestMessage request = objectMapper.readValue(json, StopRequestMessage.class);

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

			StopOutcome outcome = stopService.stopLocally(request.jobGuid(), targetExecution);
			reply(replyTo, new StopReplyMessage(outcome.status(), outcome.message(), outcome.executionId()));
		} catch (Exception e) {
			logger.warn("Failed to handle stop broadcast request: {}", e.getMessage(), e);
		}
	}

	private void reply(String replyTo, StopReplyMessage reply) {
		try {
			byte[] payload = objectMapper.writeValueAsBytes(reply);
			natsConnection.publish(replyTo, payload);
		} catch (Exception e) {
			logger.warn("Failed to publish stop reply: {}", e.getMessage(), e);
		}
	}

	@Override
	public void stop() {
		Dispatcher currentDispatcher = dispatcher.getAndSet(null);
		if (currentDispatcher != null) {
			natsConnection.closeDispatcher(currentDispatcher);
		}
	}

	@Override
	public boolean isRunning() {
		return dispatcher.get() != null;
	}
}
