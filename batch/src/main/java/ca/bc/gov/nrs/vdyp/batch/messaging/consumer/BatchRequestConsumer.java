package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.NatsBatchProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.BatchRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
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
	private final JobLauncher jobLauncher;
	private final Job vdypBatchJob;
	private final BatchProperties batchProperties;
	private final ThreadPoolTaskExecutor taskExecutor;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread workerThread;

	public BatchRequestConsumer(
			Connection natsConnection, NatsBatchProperties properties, ObjectMapper objectMapper,
			@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, @Qualifier("fetchAndPartitionJob") Job vdypBatchJob,
			BatchProperties batchProperties, @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor
	) {
		this.natsConnection = natsConnection;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.jobLauncher = jobLauncher;
		this.vdypBatchJob = vdypBatchJob;
		this.batchProperties = batchProperties;
		this.taskExecutor = taskExecutor;
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
				if (!hasJobLaunchCapacity()) {
					pauseUntilCapacityAvailable();
					continue;
				}

				List<Message> messages = subscription.fetch(properties.batchSize(), properties.pollTimeout());

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
		try {
			if (!hasJobLaunchCapacity()) {
				logger.info(
						"No batch thread pool capacity available; deferring NATS batch request. active={}, max={}",
						taskExecutor.getActiveCount(), taskExecutor.getMaxPoolSize()
				);
				message.nak();
				return;
			}

			String json = new String(message.getData(), StandardCharsets.UTF_8);
			BatchRequestMessage request = objectMapper.readValue(json, BatchRequestMessage.class);

			launchSpringBatchJob(request);

			message.ack();
		} catch (Exception ex) {
			/*
			 * Do not ack if the job could not be launched. JetStream will redeliver after ack_wait.
			 */
			message.nak();
		}
	}

	boolean hasJobLaunchCapacity() {
		return taskExecutor.getActiveCount() < taskExecutor.getMaxPoolSize();
	}

	private void pauseUntilCapacityAvailable() throws InterruptedException {
		Duration pollTimeout = properties.pollTimeout();
		long sleepMillis = 1000;

		if (pollTimeout != null && !pollTimeout.isNegative() && !pollTimeout.isZero()) {
			sleepMillis = Math.max(1, pollTimeout.toMillis());
		}

		logger.debug(
				"Waiting for batch thread pool capacity before fetching another NATS request. active={}, max={}",
				taskExecutor.getActiveCount(), taskExecutor.getMaxPoolSize()
		);
		Thread.sleep(sleepMillis);
	}

	private void launchSpringBatchJob(BatchRequestMessage request) throws Exception {
		String jobGuid = BatchUtils.createJobGuid();
		String jobTimestamp = BatchUtils.createJobTimestamp();
		Integer numPartitions = batchProperties.getPartition().getDefaultNumberOfPartitions();
		Path jobBaseDir = createJobBaseDirectory(jobGuid);

		JobParameters parameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.addString(BatchConstants.Projection.PARAMETERS_JSON, request.parameterJSON())
				.addString(BatchConstants.Job.TIMESTAMP, jobTimestamp)
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir.toString())
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, request.projectionID().toString())
				.addLong(BatchConstants.Partition.NUMBER, numPartitions.longValue()).toJobParameters();

		logger.info("[GUID: {}] Launching NATS batch request for projection {}", jobGuid, request.projectionID());
		jobLauncher.run(vdypBatchJob, parameters);
	}

	private Path createJobBaseDirectory(String jobGuid) throws IOException {
		Path batchRootDir = Paths.get(batchProperties.getRootDirectory());
		String jobBaseFolderName = BatchUtils.createJobFolderName(BatchConstants.Job.BASE_FOLDER_PREFIX, jobGuid);
		Path jobBaseDir = batchRootDir.resolve(jobBaseFolderName);
		Files.createDirectories(jobBaseDir);
		return jobBaseDir;
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
}
