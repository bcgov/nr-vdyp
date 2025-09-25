package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import ca.bc.gov.nrs.vdyp.batch.exception.ResultAggregationException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.ResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartitionedBatchConfigurationTest {

	// Test constants
	private static final String TEST_THREAD_PREFIX = "VDYP-Worker-";
	private static final Long TEST_JOB_EXECUTION_ID = 12345L;
	private static final String TEST_PARTITION_NAME = "partition0";
	private static final int TEST_CORE_POOL_SIZE = 4;
	private static final int TEST_MAX_POOL_MULTIPLIER = 2;
	private static final int TEST_MAX_ATTEMPTS = 3;
	private static final int TEST_BACKOFF_PERIOD = 1000;
	private static final int TEST_MAX_SKIP_COUNT = 5;

	@Mock
	private JobRepository jobRepository;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private BatchProperties batchProperties;

	@Mock
	private BatchProperties.Retry retry;

	@Mock
	private BatchProperties.Skip skip;

	@Mock
	private BatchProperties.ThreadPool threadPool;

	@Mock
	private BatchProperties.Partitioning partitioning;

	@Mock
	private BatchProperties.Output output;

	@Mock
	private BatchProperties.Output.Directory directory;

	@Mock
	private BatchProperties.Reader reader;

	@Mock
	private PlatformTransactionManager transactionManager;

	@Mock
	private PartitionedJobExecutionListener jobExecutionListener;

	@Mock
	private ResultAggregationService resultAggregationService;

	@Mock
	private VdypProjectionService vdypProjectionService;

	@TempDir
	Path tempDir;

	private PartitionedBatchConfiguration configuration;

	@BeforeEach
	void setUp() {
		configuration = new PartitionedBatchConfiguration(
				jobRepository, metricsCollector, batchProperties, resultAggregationService);

		// Setup common mock behaviors
		when(batchProperties.getRetry()).thenReturn(retry);
		when(batchProperties.getSkip()).thenReturn(skip);
		when(batchProperties.getThreadPool()).thenReturn(threadPool);
		when(batchProperties.getPartitioning()).thenReturn(partitioning);
		when(batchProperties.getOutput()).thenReturn(output);
		when(batchProperties.getReader()).thenReturn(reader);
		when(output.getDirectory()).thenReturn(directory);

		// Setup default values to prevent IllegalStateException
		when(directory.getDefaultPath()).thenReturn(tempDir.toString());
		when(retry.getMaxAttempts()).thenReturn(TEST_MAX_ATTEMPTS);
		when(retry.getBackoffPeriod()).thenReturn(TEST_BACKOFF_PERIOD);
		when(skip.getMaxCount()).thenReturn(TEST_MAX_SKIP_COUNT);
		when(threadPool.getCorePoolSize()).thenReturn(TEST_CORE_POOL_SIZE);
		when(threadPool.getMaxPoolSizeMultiplier()).thenReturn(TEST_MAX_POOL_MULTIPLIER);
		when(threadPool.getThreadNamePrefix()).thenReturn(TEST_THREAD_PREFIX);
		when(reader.getChunkSize()).thenReturn(10);
	}

	@Test
	void testConstructor() {
		assertNotNull(configuration);
	}

	@Test
	void testRetryPolicy() {
		BatchRetryPolicy result = configuration.retryPolicy();

		assertNotNull(result);
		verify(retry).getMaxAttempts();
		verify(retry).getBackoffPeriod();
	}

	@Test
	void testRetryPolicy_invalidMaxAttempts_throwsException() {
		when(retry.getMaxAttempts()).thenReturn(0);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.retryPolicy();
		});

		assertTrue(exception.getMessage().contains("batch.retry.max-attempts must be configured"));
	}

	@Test
	void testRetryPolicy_invalidBackoffPeriod_throwsException() {
		when(retry.getMaxAttempts()).thenReturn(TEST_MAX_ATTEMPTS);
		when(retry.getBackoffPeriod()).thenReturn(0);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.retryPolicy();
		});

		assertTrue(exception.getMessage().contains("batch.retry.backoff-period must be configured"));
	}

	@Test
	void testSkipPolicy() {
		BatchSkipPolicy result = configuration.skipPolicy();

		assertNotNull(result);
		verify(skip).getMaxCount();
	}

	@Test
	void testSkipPolicy_invalidMaxCount_throwsException() {
		when(skip.getMaxCount()).thenReturn(0);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.skipPolicy();
		});

		assertTrue(exception.getMessage().contains("batch.skip.max-count must be configured"));
	}

	@Test
	void testTaskExecutor() {
		TaskExecutor result = configuration.taskExecutor();

		assertNotNull(result);
		verify(threadPool).getCorePoolSize();
		verify(threadPool).getMaxPoolSizeMultiplier();
		verify(threadPool).getThreadNamePrefix();
	}

	@Test
	void testTaskExecutor_invalidCorePoolSize_throwsException() {
		when(threadPool.getCorePoolSize()).thenReturn(0);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.taskExecutor();
		});

		assertTrue(exception.getMessage().contains("batch.thread-pool.core-pool-size must be configured"));
	}

	@Test
	void testTaskExecutor_invalidMaxPoolSizeMultiplier_throwsException() {
		when(threadPool.getCorePoolSize()).thenReturn(TEST_CORE_POOL_SIZE);
		when(threadPool.getMaxPoolSizeMultiplier()).thenReturn(0);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.taskExecutor();
		});

		assertTrue(exception.getMessage().contains("batch.thread-pool.max-pool-size-multiplier must be configured"));
	}

	@Test
	void testTaskExecutor_invalidThreadNamePrefix_throwsException() {
		when(threadPool.getCorePoolSize()).thenReturn(TEST_CORE_POOL_SIZE);
		when(threadPool.getMaxPoolSizeMultiplier()).thenReturn(TEST_MAX_POOL_MULTIPLIER);
		when(threadPool.getThreadNamePrefix()).thenReturn("");

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			configuration.taskExecutor();
		});

		assertTrue(exception.getMessage().contains("batch.thread-pool.thread-name-prefix must be configured"));
	}

	@Test
	void testDynamicPartitioner() {
		DynamicPartitioner result = configuration.dynamicPartitioner();

		assertNotNull(result);
	}

	@Test
	void testDynamicPartitionHandler() {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		Step workerStep = mock(Step.class);
		DynamicPartitioner dynamicPartitioner = mock(DynamicPartitioner.class);

		DynamicPartitionHandler result = configuration.dynamicPartitionHandler(
				taskExecutor, workerStep, dynamicPartitioner, batchProperties);

		assertNotNull(result);
	}

	@Test
	void testMasterStep() {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		Step workerStep = mock(Step.class);
		DynamicPartitioner dynamicPartitioner = mock(DynamicPartitioner.class);
		DynamicPartitionHandler dynamicPartitionHandler = mock(DynamicPartitionHandler.class);

		Step result = configuration.masterStep(taskExecutor, workerStep, dynamicPartitioner, dynamicPartitionHandler);

		assertNotNull(result);
		assertEquals("masterStep", result.getName());
	}

	@Test
	void testWorkerStep() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(
				ItemStreamReader.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector,
				batchProperties, vdypProjectionService, itemReader);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getChunkSize();
	}

	@Test
	void testPartitionedJob() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job result = configuration.partitionedJob(
				jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		assertNotNull(result);
		assertEquals("VdypPartitionedJob", result.getName());
	}

	@Test
	void testPartitionReader() {
		ItemStreamReader<BatchRecord> result = configuration
				.partitionReader(metricsCollector, TEST_PARTITION_NAME, TEST_JOB_EXECUTION_ID, batchProperties);

		assertNotNull(result);
		verify(reader, atLeastOnce()).getChunkSize();
	}

	@Test
	void testVdypProjectionProcessor() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);

		VdypProjectionProcessor result = configuration.vdypProjectionProcessor(retryPolicy, metricsCollector);

		assertNotNull(result);
	}

	@Test
	void testPostProcessingStep() {
		Step result = configuration.postProcessingStep(transactionManager);

		assertNotNull(result);
		assertEquals("postProcessingStep", result.getName());
	}

	@Test
	void testResultAggregationTasklet() {
		Tasklet result = configuration.resultAggregationTasklet();

		assertNotNull(result);
	}

	@Test
	void testResultAggregationTasklet_nullOutputPath_usesSystemTemp() throws Exception {
		when(directory.getDefaultPath()).thenReturn(null);

		Tasklet tasklet = configuration.resultAggregationTasklet();

		// Mock the tasklet execution context
		StepContribution contribution = mock(
				StepContribution.class);
		ChunkContext chunkContext = mock(
				ChunkContext.class);
		StepContext stepContext = mock(
				StepContext.class);
		StepExecution stepExecution = mock(
				StepExecution.class);
		ExecutionContext executionContext = mock(
				ExecutionContext.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		// Mock successful aggregation
		Path mockPath = mock(Path.class);
		when(mockPath.toString()).thenReturn("/tmp/consolidated.zip");
		when(resultAggregationService.aggregateResults(eq(TEST_JOB_EXECUTION_ID), anyString())).thenReturn(mockPath);

		RepeatStatus result = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, result);
		verify(resultAggregationService).aggregateResults(eq(TEST_JOB_EXECUTION_ID), anyString());
		verify(executionContext).putString("consolidatedOutputPath", "/tmp/consolidated.zip");
	}

	@Test
	void testResultAggregationTasklet_ioException_throwsResultAggregationException() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		// Mock execution context
		StepContribution contribution = mock(
				StepContribution.class);
		ChunkContext chunkContext = mock(
				ChunkContext.class);
		StepContext stepContext = mock(
				StepContext.class);
		StepExecution stepExecution = mock(
				StepExecution.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		// Mock IOException during aggregation
		when(resultAggregationService.aggregateResults(eq(TEST_JOB_EXECUTION_ID), anyString()))
				.thenThrow(new IOException("File write failed"));

		ResultAggregationException exception = assertThrows(
				ResultAggregationException.class, () -> {
					tasklet.execute(contribution, chunkContext);
				});

		assertTrue(exception.getMessage().contains("I/O operation failed during result aggregation"));
		assertTrue(exception.getCause() instanceof IOException);
	}

	@Test
	void testResultAggregationTasklet_generalException_throwsResultAggregationException() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		// Mock execution context
		StepContribution contribution = mock(
				StepContribution.class);
		ChunkContext chunkContext = mock(
				ChunkContext.class);
		StepContext stepContext = mock(
				StepContext.class);
		StepExecution stepExecution = mock(
				StepExecution.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		// Mock general exception during aggregation
		when(resultAggregationService.aggregateResults(eq(TEST_JOB_EXECUTION_ID), anyString()))
				.thenThrow(new RuntimeException("Unexpected processing error"));

		ResultAggregationException exception = assertThrows(
				ResultAggregationException.class, () -> {
					tasklet.execute(contribution, chunkContext);
				});

		assertTrue(exception.getMessage().contains("Unexpected error during result aggregation"));
		assertTrue(exception.getCause() instanceof RuntimeException);
	}

	@Test
	void testWorkerStep_withMinimumChunkSize() {
		when(reader.getChunkSize()).thenReturn(0); // Test minimum chunk size enforcement

		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(
				ItemStreamReader.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector,
				batchProperties, vdypProjectionService, itemReader);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getChunkSize(); // Verify chunk size was accessed for validation
	}
}
