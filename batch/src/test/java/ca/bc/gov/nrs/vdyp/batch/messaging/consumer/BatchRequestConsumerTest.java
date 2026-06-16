package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

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
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
class BatchRequestConsumerTest {

	private static final NatsBatchProperties PROPERTIES = new NatsBatchProperties(
			"nats://localhost:4222", "", "", "VDYP_BATCH_REQUESTS", "VDYP_BATCH_WORKER", "vdyp.batch.request.standard",
			"vdyp.batch.status.standard", true, Duration.ofSeconds(1), 1
	);

	@Mock
	private Connection natsConnection;

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

	private BatchRequestConsumer consumer;

	@BeforeEach
	void setUp() {
		consumer = new BatchRequestConsumer(
				natsConnection, PROPERTIES, new ObjectMapper(), jobLauncher, vdypBatchJob, batchProperties, taskExecutor
		);
	}

	@Test
	void handleMessage_WhenThreadPoolHasCapacity_LaunchesJobAndAcksMessage() throws Exception {
		UUID jobId = UUID.randomUUID();

		when(taskExecutor.getActiveCount()).thenReturn(0);
		when(taskExecutor.getMaxPoolSize()).thenReturn(1);
		when(batchProperties.getPartition()).thenReturn(partitionProperties);
		when(partitionProperties.getDefaultNumberOfPartitions()).thenReturn(2);
		when(batchProperties.getRootDirectory()).thenReturn(tempDir.toString());
		when(message.getData()).thenReturn(
				("{\"projectionID\":\"" + jobId + "\",\"parameterJSON\":\"{}\"}").getBytes(StandardCharsets.UTF_8)
		);

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

		verify(jobLauncher, never()).run(eq(vdypBatchJob), org.mockito.ArgumentMatchers.any(JobParameters.class));
		verify(message).nak();
		verify(message, never()).ack();
	}
}
