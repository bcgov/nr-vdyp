package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsBatchProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.BatchRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.service.BatchJobLaunchService;
import ca.bc.gov.nrs.vdyp.batch.service.StartupRecoveryService;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;

@Component
@ConditionalOnProperty(name = "vdyp.nats.enabled", havingValue = "true", matchIfMissing = true)
public class BatchRequestConsumer implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(BatchRequestConsumer.class);

	private final Connection natsConnection;
	private final NatsBatchProperties properties;
	private final ObjectMapper objectMapper;
	private final BatchJobLaunchService launchService;
	private final StartupRecoveryService recoveryService;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread workerThread;

	public BatchRequestConsumer(
			Connection natsConnection, NatsBatchProperties properties, ObjectMapper objectMapper,
			BatchJobLaunchService launchService, StartupRecoveryService recoveryService
	) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.launchService = launchService;
		this.recoveryService = recoveryService;
	}

	@Override
	public void start() {
		if (!properties.enabled()) {
			return;
		}

		if (!running.compareAndSet(false, true)) {
			return;
		}

		workerThread = new Thread(this::consumeLoop, "vdyp-nats-batch-consumer");
		workerThread.start();
	}

	private void consumeLoop() {
		try {
			JetStream jetStream = natsConnection.jetStream();

			PullSubscribeOptions options = PullSubscribeOptions.builder().stream(properties.stream())
					.durable(properties.consumer()).build();

			JetStreamSubscription subscription = jetStream.subscribe(properties.subject(), options);

			while (running.get()) {
				if (recoveryService.recoverNextExpiredExecution()) {
					continue;
				}

				if (!launchService.hasCapacity()) {
					pauseUntilCapacityAvailable();
					continue;
				}

				List<Message> messages = subscription.fetch(1, properties.pollTimeout());
				if (messages.isEmpty()) {
					continue;
				}

				for (Message message : messages) {
					handleMessage(message);
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			running.set(false);
		} catch (Exception ex) {
			// In production, log this and let the container restart if appropriate.
			throw new IllegalStateException("NATS batch consumer failed", ex);
		}
	}

	void handleMessage(Message message) {
		if (recoveryService.recoverNextExpiredExecution()) {
			logger.info("Recovered expired batch work before accepting a new NATS batch request.");
			message.nak();
			return;
		}

		if (!launchService.hasCapacity()) {
			logger.info("No local batch thread capacity available; deferring NATS batch request.");
			message.nak();
			return;
		}
		try {
			String json = new String(message.getData(), StandardCharsets.UTF_8);
			BatchRequestMessage request = objectMapper.readValue(json, BatchRequestMessage.class);

			launchService.launch(request.projectionID(), request.parameterJSON());

			message.ack();
		} catch (Exception ex) {
			/*
			 * Do not ack if the job could not be launched. JetStream will redeliver after ack_wait.
			 */
			message.nak();
		}
	}

	private void pauseUntilCapacityAvailable() throws InterruptedException {
		Duration pollTimeout = properties.pollTimeout();
		long sleepMillis = 1000;

		if (pollTimeout != null && !pollTimeout.isNegative() && !pollTimeout.isZero()) {
			sleepMillis = Math.max(1, pollTimeout.toMillis());
		}

		logger.debug("Waiting for local batch job capacity before fetching another NATS request.");
		Thread.sleep(sleepMillis);
	}

	@Override
	public void stop() {
		running.set(false);

		if (workerThread != null) {
			workerThread.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public int getPhase() {
		return 1000;
	}
}
