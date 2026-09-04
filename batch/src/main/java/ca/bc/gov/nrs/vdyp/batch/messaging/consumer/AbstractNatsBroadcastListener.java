package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.context.SmartLifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

/**
 * Shared NATS core pub/sub lifecycle for "does this instance own X?" broadcast listeners: subscribes to a subject on
 * start, dispatches each message to handleMessage(Message), and cleans up its dispatcher on stop.
 */
public abstract class AbstractNatsBroadcastListener implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNatsBroadcastListener.class);

	protected final Connection natsConnection;
	protected final ObjectMapper objectMapper;
	private final String subject;
	private final JobOwnershipService ownershipService;
	private final JobExecutionLookupService lookupService;

	private final AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();

	protected AbstractNatsBroadcastListener(
			Connection natsConnection, ObjectMapper objectMapper, String subject, JobOwnershipService ownershipService,
			JobExecutionLookupService lookupService
	) {
		this.natsConnection = natsConnection;
		this.objectMapper = objectMapper;
		this.subject = subject;
		this.ownershipService = ownershipService;
		this.lookupService = lookupService;
	}

	/**
	 * Returns the running execution for jobGuid only if this instance owns projectionGuid - empty otherwise (not owned
	 * locally, or no such execution), which is what tells the caller to stay silent on this broadcast.
	 */
	protected Optional<JobExecution> getJobExecutionForProjectionGuid(String projectionGuid, String jobGuid) {
		if (!ownershipService.isOwnedLocally(projectionGuid)) {
			return Optional.empty();
		}
		try {
			return Optional.of(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, jobGuid, true));
		} catch (NoSuchJobExecutionException e) {
			return Optional.empty();
		}
	}

	@Override
	public void start() {
		Dispatcher newDispatcher = natsConnection.createDispatcher(this::handleMessage);
		newDispatcher.subscribe(subject);
		dispatcher.set(newDispatcher);
	}

	abstract void handleMessage(Message message);

	protected void reply(String replyTo, Object payload) {
		try {
			natsConnection.publish(replyTo, objectMapper.writeValueAsBytes(payload));
		} catch (Exception e) {
			logger.warn("Failed to publish reply: {}", e.getMessage(), e);
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
