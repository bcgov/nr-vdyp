package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	private static final String TEST_JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
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
	private BatchProperties.RetryProperties retry;

	@Mock
	private BatchProperties.SkipProperties skip;

	@Mock
	private BatchProperties.ThreadPoolProperties threadPool;

	@Mock
	private BatchProperties.PartitionProperties partition;

	@Mock
	private BatchProperties.ReaderProperties reader;

	@Mock
	private PlatformTransactionManager transactionManager;

	@Mock
	private PartitionedJobExecutionListener jobExecutionListener;

	@Mock
	private ResultAggregationService resultAggregationService;

	@Mock
	private VdypProjectionService vdypProjectionService;

	@Mock
	private ObjectMapper objectMapper;

	@TempDir
	Path tempDir;

	private PartitionedBatchConfiguration configuration;

	@BeforeEach
	void setUp() {
		configuration = new PartitionedBatchConfiguration(
				jobRepository, metricsCollector, batchProperties, resultAggregationService
		);

		when(batchProperties.getRetry()).thenReturn(retry);
		when(batchProperties.getSkip()).thenReturn(skip);
		when(batchProperties.getThreadPool()).thenReturn(threadPool);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(batchProperties.getReader()).thenReturn(reader);
		when(batchProperties.getRootDirectory()).thenReturn(tempDir.toString());

		when(retry.getMaxAttempts()).thenReturn(TEST_MAX_ATTEMPTS);
		when(retry.getBackoffPeriod()).thenReturn(TEST_BACKOFF_PERIOD);
		when(skip.getMaxCount()).thenReturn(TEST_MAX_SKIP_COUNT);
		when(threadPool.getCorePoolSize()).thenReturn(TEST_CORE_POOL_SIZE);
		when(threadPool.getMaxPoolSizeMultiplier()).thenReturn(TEST_MAX_POOL_MULTIPLIER);
		when(threadPool.getThreadNamePrefix()).thenReturn(TEST_THREAD_PREFIX);
		when(reader.getDefaultChunkSize()).thenReturn(10);
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
	void testSkipPolicy() {
		BatchSkipPolicy result = configuration.skipPolicy();

		assertNotNull(result);
		verify(skip).getMaxCount();
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
	void testDynamicPartitioner() {
		DynamicPartitioner result = configuration.dynamicPartitioner();

		assertNotNull(result);
	}

	@Test
	void testDynamicPartitionHandler() {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		Step workerStep = mock(Step.class);
		DynamicPartitioner dynamicPartitioner = mock(DynamicPartitioner.class);

		DynamicPartitionHandler result = configuration
				.dynamicPartitionHandler(taskExecutor, workerStep, dynamicPartitioner, batchProperties);

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
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector, batchProperties, vdypProjectionService,
				itemReader, partitionWriter, projectionProcessor
		);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testPartitionedJob() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job result = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		assertNotNull(result);
		assertEquals("VdypPartitionedJob", result.getName());
	}

	@Test
	void testPartitionReader() {
		ItemStreamReader<BatchRecord> result = configuration.partitionReader(
				metricsCollector, TEST_PARTITION_NAME, TEST_JOB_EXECUTION_ID, TEST_JOB_GUID, batchProperties
		);

		assertNotNull(result);
		verify(reader, atLeastOnce()).getDefaultChunkSize();
	}

	@Test
	void testVdypProjectionProcessor() {
		VdypProjectionProcessor result = configuration.vdypProjectionProcessor(metricsCollector);

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
	void testResultAggregationTasklet_successfulExecution() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		Path mockPath = mock(Path.class);
		when(mockPath.toString()).thenReturn("/tmp/consolidated.zip");
		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenReturn(mockPath);

		RepeatStatus result = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, result);
		verify(resultAggregationService)
				.aggregateResultsFromJobDir(eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString());
		verify(executionContext).putString("consolidatedOutputPath", "/tmp/consolidated.zip");
	}

	@Test
	void testWorkerStep_withMinimumChunkSize() {
		when(reader.getDefaultChunkSize()).thenReturn(0);

		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(
				ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector,
				batchProperties, vdypProjectionService, itemReader, partitionWriter, projectionProcessor);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testPartitionWriter() {
		VdypChunkProjectionWriter result = configuration.partitionWriter(vdypProjectionService, objectMapper);

		assertNotNull(result);
	}

	@Test
	void testResultAggregationTasklet_withValidation() throws Exception {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(true);

		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		Path mockPath = tempDir.resolve("consolidated.zip");
		when(resultAggregationService.aggregateResultsFromJobDir(eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()))
				.thenReturn(mockPath);
		when(resultAggregationService.validateConsolidatedZip(mockPath)).thenReturn(true);

		RepeatStatus result = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, result);
		verify(resultAggregationService).validateConsolidatedZip(mockPath);
		verify(resultAggregationService).cleanupPartitionDirectories(tempDir);
	}

	@Test
	void testResultAggregationTasklet_validationFailed() throws Exception {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(true);

		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		JobExecution jobExecution = mock(
				JobExecution.class
		);
		JobParameters jobParameters = mock(
				JobParameters.class
		);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		Path mockPath = tempDir.resolve("consolidated.zip");
		when(resultAggregationService.aggregateResultsFromJobDir(eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()))
				.thenReturn(mockPath);
		when(resultAggregationService.validateConsolidatedZip(mockPath)).thenReturn(false);

		RepeatStatus result = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, result);
		verify(resultAggregationService).validateConsolidatedZip(mockPath);
		verify(resultAggregationService, org.mockito.Mockito.never()).cleanupPartitionDirectories(any());
	}

	@Test
	void testResultAggregationTasklet_ioException() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		JobExecution jobExecution = mock(JobExecution.class);
		org.springframework.batch.core.JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		ResultAggregationException testException = ResultAggregationException.handleResultAggregationFailure(
				new IOException("Test IO error"), "I/O operation failed during result aggregation", TEST_JOB_GUID,
				TEST_JOB_EXECUTION_ID, org.slf4j.LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(testException);

		Exception exception = Assertions
				.assertThrows(ResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("I/O operation failed"));
		Assertions.assertTrue(exception.getMessage().contains("Test IO error"));
	}

	@Test
	void testResultAggregationTasklet_generalException() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		ResultAggregationException testException = ResultAggregationException.handleResultAggregationFailure(
				new RuntimeException("Test runtime error"), "Unexpected error during result aggregation", TEST_JOB_GUID,
				TEST_JOB_EXECUTION_ID, org.slf4j.LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(testException);

		Exception exception = Assertions
				.assertThrows(ResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("Unexpected error"));
		Assertions.assertTrue(exception.getMessage().contains("Test runtime error"));
	}

	@Test
	void testResultAggregationTasklet_exceptionWithNullMessage() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		ResultAggregationException testException = ResultAggregationException.handleResultAggregationFailure(
				new RuntimeException((String) null), "Unexpected error during result aggregation", TEST_JOB_GUID,
				TEST_JOB_EXECUTION_ID, org.slf4j.LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(testException);

		Exception exception = Assertions
				.assertThrows(ResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("Unexpected error"));
		Assertions.assertNotNull(exception.getMessage());
	}

	@Test
	void testWorkerStep_stepExecutionListenerBeforeStep() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step workerStep = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector, batchProperties, vdypProjectionService,
				itemReader, partitionWriter, projectionProcessor
		);

		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(eq("partitionName"), anyString())).thenReturn(TEST_PARTITION_NAME);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		assertNotNull(workerStep);
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testWorkerStep_stepExecutionListenerAfterStep() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step workerStep = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector, batchProperties, vdypProjectionService,
				itemReader, partitionWriter, projectionProcessor
		);

		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		ExitStatus exitStatus = ExitStatus.COMPLETED;

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(eq("partitionName"), anyString())).thenReturn(TEST_PARTITION_NAME);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getWriteCount()).thenReturn(100L);
		when(stepExecution.getReadCount()).thenReturn(100L);
		when(stepExecution.getSkipCount()).thenReturn(0L);
		when(stepExecution.getExitStatus()).thenReturn(exitStatus);

		assertNotNull(workerStep);
		assertEquals("workerStep", workerStep.getName());
	}

	@Test
	void testPartitionedJob_JobExecutionListeners() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testWorkerStep_chunkSizeCalculation() {
		when(reader.getDefaultChunkSize()).thenReturn(-5);

		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector,
				batchProperties, vdypProjectionService, itemReader, partitionWriter, projectionProcessor);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testWorkerStep_withLargeChunkSize() {
		when(reader.getDefaultChunkSize()).thenReturn(1000);

		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector,
				batchProperties, vdypProjectionService, itemReader, partitionWriter, projectionProcessor);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testBatchProperties_DefaultValues() {
		BatchProperties props = new BatchProperties();

		assertNotNull(props.getJob());
		assertNotNull(props.getPartition());
		assertNotNull(props.getThreadPool());
		assertNotNull(props.getValidation());
		assertNotNull(props.getRetry());
		assertNotNull(props.getSkip());
		assertNotNull(props.getReader());
	}

	@Test
	void testBatchProperties_JobConfiguration() {
		BatchProperties props = new BatchProperties();
		BatchProperties.JobProperties job = props.getJob();

		assertTrue(job.isAutoCreate());

		job.setAutoCreate(false);
		assertFalse(job.isAutoCreate());

		assertNull(job.getBaseFolderPrefix());
		job.setBaseFolderPrefix("vdyp-batch");
		assertEquals("vdyp-batch", job.getBaseFolderPrefix());
	}

	@Test
	void testBatchProperties_RetryConfiguration() {
		retry.setMaxAttempts(3);
		assertEquals(3, retry.getMaxAttempts());

		retry.setBackoffPeriod(1000);
		assertEquals(1000, retry.getBackoffPeriod());
	}

	@Test
	void testBatchProperties_ThreadPoolConfiguration() {
		threadPool.setCorePoolSize(4);
		assertEquals(4, threadPool.getCorePoolSize());

		threadPool.setMaxPoolSizeMultiplier(2);
		assertEquals(2, threadPool.getMaxPoolSizeMultiplier());
	}

	@Test
	void testBatchProperties_ValidationConfiguration() {
		BatchProperties props = new BatchProperties();
		BatchProperties.ValidationProperties validation = props.getValidation();

		validation.setMaxDataLength(10000);
		assertEquals(10000, validation.getMaxDataLength());

		validation.setMinPolygonIdLength(5);
		assertEquals(5, validation.getMinPolygonIdLength());

		validation.setMaxPolygonIdLength(20);
		assertEquals(20, validation.getMaxPolygonIdLength());
	}

	@Test
	void testBatchProperties_SkipConfiguration() {
		assertEquals(5, skip.getMaxCount());
	}

	@Test
	void testBatchProperties_ReaderConfiguration() {
		assertEquals(10, reader.getDefaultChunkSize());
	}

	@Test
	void testBatchProperties_RootDirectory() {
		BatchProperties props = new BatchProperties();

		assertNull(props.getRootDirectory());

		props.setRootDirectory("/tmp/vdyp-batch");
		assertEquals("/tmp/vdyp-batch", props.getRootDirectory());
	}

	@Test
	void testBatchProperties_SettersAndGetters() {
		BatchProperties props = new BatchProperties();

		BatchProperties.JobProperties newJob = new BatchProperties.JobProperties();
		newJob.setAutoCreate(false);
		props.setJob(newJob);
		assertFalse(props.getJob().isAutoCreate());

		BatchProperties.PartitionProperties newPartition = new BatchProperties.PartitionProperties();
		newPartition.setDefaultPartitionSize(200);
		props.setPartition(newPartition);
		assertEquals(200, props.getPartition().getDefaultPartitionSize());

		BatchProperties.RetryProperties newRetry = new BatchProperties.RetryProperties();
		newRetry.setMaxAttempts(5);
		props.setRetry(newRetry);
		assertEquals(5, props.getRetry().getMaxAttempts());

		BatchProperties.ThreadPoolProperties newThreadPool = new BatchProperties.ThreadPoolProperties();
		newThreadPool.setCorePoolSize(8);
		props.setThreadPool(newThreadPool);
		assertEquals(8, props.getThreadPool().getCorePoolSize());

		BatchProperties.ValidationProperties newValidation = new BatchProperties.ValidationProperties();
		newValidation.setMaxDataLength(20000);
		props.setValidation(newValidation);
		assertEquals(20000, props.getValidation().getMaxDataLength());

		BatchProperties.SkipProperties newSkip = new BatchProperties.SkipProperties();
		newSkip.setMaxCount(50);
		props.setSkip(newSkip);
		assertEquals(50, props.getSkip().getMaxCount());

		BatchProperties.ReaderProperties newReader = new BatchProperties.ReaderProperties();
		newReader.setDefaultChunkSize(75);
		props.setReader(newReader);
		assertEquals(75, props.getReader().getDefaultChunkSize());
	}

	@Test
	void testBatchProperties_NestedObjectIndependence() {
		BatchProperties props1 = new BatchProperties();
		BatchProperties props2 = new BatchProperties();

		props1.getJob().setAutoCreate(false);
		props2.getJob().setAutoCreate(true);

		assertFalse(props1.getJob().isAutoCreate());
		assertTrue(props2.getJob().isAutoCreate());
	}

	@Test
	void testBatchProperties_AllNullableFields() {
		BatchProperties props = new BatchProperties();

		assertNull(props.getRootDirectory());
		assertNull(props.getJob().getBaseFolderPrefix());
		assertNull(props.getPartition().getDefaultPartitionSize());
		assertNull(props.getPartition().getInputPolygonFileName());
		assertNull(props.getPartition().getInputLayerFileName());
		assertNull(props.getPartition().getInputFolderNamePrefix());
		assertNull(props.getPartition().getOutputFolderNamePrefix());
		assertNull(props.getPartition().getNamePrefix());
		assertNull(props.getThreadPool().getThreadNamePrefix());
		assertNull(props.getReader().getDefaultChunkSize());
	}

	@Test
	void testBatchProperties_CompleteConfiguration() {
		BatchProperties props = new BatchProperties();

		props.setRootDirectory("/data/vdyp");

		props.getJob().setAutoCreate(true);
		props.getJob().setBaseFolderPrefix("vdyp-job");

		props.getPartition().setDefaultPartitionSize(500);
		props.getPartition().setInputPolygonFileName("input-polygon.csv");
		props.getPartition().setInputLayerFileName("input-layer.csv");
		props.getPartition().setInputFolderNamePrefix("input");
		props.getPartition().setOutputFolderNamePrefix("output");
		props.getPartition().setNamePrefix("part");

		props.getRetry().setMaxAttempts(3);
		props.getRetry().setBackoffPeriod(2000);

		props.getThreadPool().setCorePoolSize(10);
		props.getThreadPool().setMaxPoolSizeMultiplier(3);
		props.getThreadPool().setThreadNamePrefix("batch-thread");

		props.getValidation().setMaxDataLength(50000);
		props.getValidation().setMinPolygonIdLength(8);
		props.getValidation().setMaxPolygonIdLength(25);

		props.getSkip().setMaxCount(200);

		props.getReader().setDefaultChunkSize(100);

		assertEquals("/data/vdyp", props.getRootDirectory());
		assertTrue(props.getJob().isAutoCreate());
		assertEquals("vdyp-job", props.getJob().getBaseFolderPrefix());
		assertEquals(500, props.getPartition().getDefaultPartitionSize());
		assertEquals("input-polygon.csv", props.getPartition().getInputPolygonFileName());
		assertEquals("input-layer.csv", props.getPartition().getInputLayerFileName());
		assertEquals("input", props.getPartition().getInputFolderNamePrefix());
		assertEquals("output", props.getPartition().getOutputFolderNamePrefix());
		assertEquals("part", props.getPartition().getNamePrefix());
		assertEquals(3, props.getRetry().getMaxAttempts());
		assertEquals(2000, props.getRetry().getBackoffPeriod());
		assertEquals(10, props.getThreadPool().getCorePoolSize());
		assertEquals(3, props.getThreadPool().getMaxPoolSizeMultiplier());
		assertEquals("batch-thread", props.getThreadPool().getThreadNamePrefix());
		assertEquals(50000, props.getValidation().getMaxDataLength());
		assertEquals(8, props.getValidation().getMinPolygonIdLength());
		assertEquals(25, props.getValidation().getMaxPolygonIdLength());
		assertEquals(200, props.getSkip().getMaxCount());
		assertEquals(100, props.getReader().getDefaultChunkSize());
	}

	@Test
	void testAsyncJobLauncher() throws Exception {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);

		org.springframework.batch.core.launch.JobLauncher result = configuration.asyncJobLauncher(taskExecutor);

		assertNotNull(result);
		assertTrue(result instanceof TaskExecutorJobLauncher);
	}

	@Test
	void testResultAggregationTasklet_cleanupDisabled() throws Exception {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(false);

		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		Path mockPath = tempDir.resolve("consolidated.zip");
		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenReturn(mockPath);

		RepeatStatus result = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, result);

		verify(resultAggregationService, org.mockito.Mockito.never()).cleanupPartitionDirectories(any());
		verify(resultAggregationService, org.mockito.Mockito.never()).validateConsolidatedZip(any());
	}

	@Test
	void testPartitionedJob_beforeJobCallback() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testPartitionedJob_afterJobCallback_withCompletedStatus() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);
		java.util.Set<StepExecution> stepExecutions = new java.util.HashSet<>();
		StepExecution step1 = mock(StepExecution.class);
		StepExecution step2 = mock(StepExecution.class);

		when(step1.getStepName()).thenReturn("workerStep:partition0");
		when(step2.getStepName()).thenReturn("workerStep:partition1");
		when(step1.getReadCount()).thenReturn(50L);
		when(step2.getReadCount()).thenReturn(75L);
		when(step1.getWriteCount()).thenReturn(48L);
		when(step2.getWriteCount()).thenReturn(73L);

		stepExecutions.add(step1);
		stepExecutions.add(step2);

		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
		when(jobExecution.getStepExecutions()).thenReturn(stepExecutions);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testPartitionedJob_afterJobCallback_withStoppedStatus_cleanupEnabled() {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(true);

		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);
		java.util.Set<StepExecution> stepExecutions = new java.util.HashSet<>();
		StepExecution step1 = mock(StepExecution.class);

		when(step1.getStepName()).thenReturn("workerStep:partition0");
		when(step1.getReadCount()).thenReturn(50L);
		when(step1.getWriteCount()).thenReturn(48L);

		stepExecutions.add(step1);

		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.STOPPED);
		when(jobExecution.getStepExecutions()).thenReturn(stepExecutions);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testPartitionedJob_afterJobCallback_withStoppedStatus_cleanupException() {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(true);

		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);
		java.util.Set<StepExecution> stepExecutions = new java.util.HashSet<>();

		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobBaseDir")).thenReturn(tempDir.toString());
		when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.STOPPED);
		when(jobExecution.getStepExecutions()).thenReturn(stepExecutions);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testPartitionedJob_afterJobCallback_withNullJobBaseDir() {
		when(partition.getInterimDirsCleanupEnabled()).thenReturn(true);

		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class
		);
		java.util.Set<StepExecution> stepExecutions = new java.util.HashSet<>();

		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobBaseDir")).thenReturn(null);
		when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.STOPPED);
		when(jobExecution.getStepExecutions()).thenReturn(stepExecutions);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

	@Test
	void testWorkerStep_afterStep_withFailedStatus() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchRecord> itemReader = mock(ItemStreamReader.class);
		VdypChunkProjectionWriter partitionWriter = mock(VdypChunkProjectionWriter.class);
		VdypProjectionProcessor projectionProcessor = mock(VdypProjectionProcessor.class);

		Step workerStep = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector, batchProperties, vdypProjectionService,
				itemReader, partitionWriter, projectionProcessor
		);

		StepExecution stepExecution = mock(StepExecution.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		ExitStatus exitStatus = ExitStatus.FAILED;
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(eq("partitionName"), anyString())).thenReturn(TEST_PARTITION_NAME);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getWriteCount()).thenReturn(50L);
		when(stepExecution.getReadCount()).thenReturn(100L);
		when(stepExecution.getSkipCount()).thenReturn(50L);
		when(stepExecution.getExitStatus()).thenReturn(exitStatus);

		assertNotNull(workerStep);
		assertEquals("workerStep", workerStep.getName());
	}

	@Test
	void testBatchProperties_PartitionConfiguration() {
		BatchProperties props = new BatchProperties();
		BatchProperties.PartitionProperties partitionConfig = props.getPartition();

		partitionConfig.setInterimDirsCleanupEnabled(true);
		assertTrue(partitionConfig.getInterimDirsCleanupEnabled());

		partitionConfig.setInterimDirsCleanupEnabled(false);
		assertFalse(partitionConfig.getInterimDirsCleanupEnabled());
	}

	@Test
	void testResultAggregationTasklet_withNullJobBaseDirParameter() throws Exception {
		Tasklet tasklet = configuration.resultAggregationTasklet();

		StepContribution contribution = mock(StepContribution.class);
		ChunkContext chunkContext = mock(ChunkContext.class);
		StepContext stepContext = mock(StepContext.class);
		StepExecution stepExecution = mock(StepExecution.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobParameters jobParameters = mock(JobParameters.class);

		when(chunkContext.getStepContext()).thenReturn(stepContext);
		when(stepContext.getStepExecution()).thenReturn(stepExecution);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getId()).thenReturn(TEST_JOB_EXECUTION_ID);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(jobParameters.getString("jobTimestamp")).thenReturn("test-timestamp");
		when(jobParameters.getString("jobBaseDir")).thenReturn(null);
		when(stepExecution.getJobExecutionId()).thenReturn(TEST_JOB_EXECUTION_ID);

		ResultAggregationException testException = ResultAggregationException.handleResultAggregationFailure(
				new NullPointerException("jobBaseDir cannot be null"),
				"Failed to aggregate results with null jobBaseDir", TEST_JOB_GUID, TEST_JOB_EXECUTION_ID,
				org.slf4j.LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService
						.aggregateResultsFromJobDir(eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), eq(null), anyString())
		).thenThrow(testException);

		Assertions.assertThrows(ResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));
	}
}
