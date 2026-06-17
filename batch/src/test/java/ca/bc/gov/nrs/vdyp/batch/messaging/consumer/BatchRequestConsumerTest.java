package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.NatsBatchProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.BatchRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;

@ExtendWith(MockitoExtension.class)
class BatchRequestConsumerTest {

	private static final NatsBatchProperties PROPERTIES = new NatsBatchProperties(
			"nats://localhost:4222", "", "", "VDYP_BATCH_REQUESTS", "VDYP_BATCH_WORKER", "vdyp.batch.request.standard",
			"vdyp.batch.status.standard", true, Duration.ofSeconds(1), 1
	);

	@Mock
	private Connection natsConnection;

	@Mock
	private JetStream jetStream;

	@Mock
	private JetStreamSubscription subscription;

	@Mock
	private JobLauncher jobLauncher;

	@Mock
	private Job vdypBatchJob;

	@Mock
	private BatchProperties batchProperties;

	@Mock
	private BatchProperties.PartitionProperties partitionProperties;

	@Mock
	private ThreadPoolTaskExecutor taskExecutor;

	@Mock
	private Message message;

	@TempDir
	private Path tempDir;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private BatchRequestConsumer consumer;

	@BeforeEach
	void setUp() {
		consumer = new BatchRequestConsumer(
				natsConnection, PROPERTIES, objectMapper, jobLauncher, vdypBatchJob, batchProperties, taskExecutor
		);
	}

	@Test
	void handleMessage_WhenThreadPoolHasCapacity_LaunchesJobAndAcksMessage() throws Exception {
		UUID jobId = UUID.randomUUID();

		givenThreadPoolHasCapacity();
		givenBatchProperties(2);
		givenMessageData(jobId, "{}");

		consumer.handleMessage(message);

		ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
		verify(jobLauncher).run(eq(vdypBatchJob), parametersCaptor.capture());

		JobParameters parameters = parametersCaptor.getValue();
		assertNotNull(parameters.getString(BatchConstants.Job.GUID));
		assertEquals("{}", parameters.getString(BatchConstants.Projection.PARAMETERS_JSON));
		assertNotNull(parameters.getString(BatchConstants.Job.TIMESTAMP));
		assertTrue(Files.exists(Path.of(parameters.getString(BatchConstants.Job.BASE_DIR))));
		assertEquals(jobId.toString(), parameters.getString(BatchConstants.GuidInput.PROJECTION_GUID));
		assertEquals(2L, parameters.getLong(BatchConstants.Partition.NUMBER));
		verify(message).ack();
		verify(message, never()).nak();
	}

	@Test
	void handleMessage_WhenThreadPoolHasNoCapacity_NaksMessageWithoutLaunchingJob() throws Exception {
		when(taskExecutor.getActiveCount()).thenReturn(1);
		when(taskExecutor.getMaxPoolSize()).thenReturn(1);

		consumer.handleMessage(message);

		verify(jobLauncher, never()).run(eq(vdypBatchJob), any(JobParameters.class));
		verify(message).nak();
		verify(message, never()).ack();
	}

	@Test
	void handleMessage_WhenPayloadCannotBeParsed_NaksMessageWithoutLaunchingJob() throws Exception {
		givenThreadPoolHasCapacity();
		when(message.getData()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

		consumer.handleMessage(message);

		verify(jobLauncher, never()).run(eq(vdypBatchJob), any(JobParameters.class));
		verify(message).nak();
		verify(message, never()).ack();
	}

	@Test
	void handleMessage_WhenJobLauncherFails_NaksMessageWithoutAcking() throws Exception {
		UUID jobId = UUID.randomUUID();

		givenThreadPoolHasCapacity();
		givenBatchProperties(3);
		givenMessageData(jobId, "{\"mode\":\"test\"}");
		when(jobLauncher.run(eq(vdypBatchJob), any(JobParameters.class))).thenThrow(new RuntimeException("boom"));

		consumer.handleMessage(message);

		verify(jobLauncher).run(eq(vdypBatchJob), any(JobParameters.class));
		verify(message).nak();
		verify(message, never()).ack();
	}

	@Test
	void hasJobLaunchCapacity_WhenThreadPoolHasAvailableWorker_ReturnsTrue() {
		when(taskExecutor.getActiveCount()).thenReturn(1);
		when(taskExecutor.getMaxPoolSize()).thenReturn(2);

		assertTrue(consumer.hasJobLaunchCapacity());
	}

	@Test
	void hasJobLaunchCapacity_WhenThreadPoolIsFull_ReturnsFalse() {
		when(taskExecutor.getActiveCount()).thenReturn(2);
		when(taskExecutor.getMaxPoolSize()).thenReturn(2);

		assertFalse(consumer.hasJobLaunchCapacity());
	}

	@Test
	void start_WhenNatsIsDisabled_DoesNotStartWorker() throws Exception {
		BatchRequestConsumer disabledConsumer = new BatchRequestConsumer(
				natsConnection, natsProperties(false), objectMapper, jobLauncher, vdypBatchJob, batchProperties,
				taskExecutor
		);

		disabledConsumer.start();

		assertFalse(disabledConsumer.isRunning());
		verify(natsConnection, never()).jetStream();
	}

	@Test
	void start_WhenNatsIsEnabled_SubscribesAndFetchesBatchMessages() throws Exception {
		CountDownLatch fetchCalled = new CountDownLatch(1);

		when(taskExecutor.getActiveCount()).thenReturn(0);
		when(taskExecutor.getMaxPoolSize()).thenReturn(1);
		when(natsConnection.jetStream()).thenReturn(jetStream);
		when(jetStream.subscribe(eq(PROPERTIES.subject()), any(PullSubscribeOptions.class))).thenReturn(subscription);
		when(subscription.fetch(PROPERTIES.batchSize(), PROPERTIES.pollTimeout())).thenAnswer(invocation -> {
			consumer.stop();
			fetchCalled.countDown();
			return List.of();
		});

		try {
			consumer.start();

			assertTrue(fetchCalled.await(1, TimeUnit.SECONDS));
			assertFalse(consumer.isRunning());
			verify(jetStream).subscribe(eq(PROPERTIES.subject()), any(PullSubscribeOptions.class));
			verify(subscription).fetch(PROPERTIES.batchSize(), PROPERTIES.pollTimeout());
		} finally {
			consumer.stop();
		}
	}

	@Test
	void start_WhenNoJobLaunchCapacityInitially_PausesUntilCapacityIsAvailableBeforeFetching() throws Exception {
		NatsBatchProperties shortPollProperties = natsProperties(true, Duration.ofMillis(1));
		BatchRequestConsumer capacityLimitedConsumer = new BatchRequestConsumer(
				natsConnection, shortPollProperties, objectMapper, jobLauncher, vdypBatchJob, batchProperties,
				taskExecutor
		);
		CountDownLatch fetchCalled = new CountDownLatch(1);

		when(taskExecutor.getActiveCount()).thenReturn(1, 1, 0);
		when(taskExecutor.getMaxPoolSize()).thenReturn(1);
		when(natsConnection.jetStream()).thenReturn(jetStream);
		when(jetStream.subscribe(eq(shortPollProperties.subject()), any(PullSubscribeOptions.class)))
				.thenReturn(subscription);
		when(subscription.fetch(shortPollProperties.batchSize(), shortPollProperties.pollTimeout()))
				.thenAnswer(invocation -> {
					capacityLimitedConsumer.stop();
					fetchCalled.countDown();
					return List.of();
				});

		try {
			capacityLimitedConsumer.start();

			assertTrue(fetchCalled.await(1, TimeUnit.SECONDS));
			assertFalse(capacityLimitedConsumer.isRunning());
			verify(subscription).fetch(shortPollProperties.batchSize(), shortPollProperties.pollTimeout());
		} finally {
			capacityLimitedConsumer.stop();
		}
	}

	private void givenThreadPoolHasCapacity() {
		when(taskExecutor.getActiveCount()).thenReturn(0);
		when(taskExecutor.getMaxPoolSize()).thenReturn(1);
	}

	private void givenBatchProperties(int numberOfPartitions) {
		when(batchProperties.getPartition()).thenReturn(partitionProperties);
		when(partitionProperties.getDefaultNumberOfPartitions()).thenReturn(numberOfPartitions);
		when(batchProperties.getRootDirectory()).thenReturn(tempDir.toString());
	}

	private void givenMessageData(UUID projectionId, String parameterJson) throws Exception {
		when(message.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(projectionId, parameterJson)));
	}

	private static NatsBatchProperties natsProperties(boolean enabled) {
		return natsProperties(enabled, PROPERTIES.pollTimeout());
	}

	private static NatsBatchProperties natsProperties(boolean enabled, Duration pollTimeout) {
		return new NatsBatchProperties(
				PROPERTIES.url(), PROPERTIES.username(), PROPERTIES.password(), PROPERTIES.stream(),
				PROPERTIES.consumer(), PROPERTIES.subject(), PROPERTIES.statusSubject(), enabled, pollTimeout,
				PROPERTIES.batchSize()
		);
	}
}
