package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultAggregationException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.BatchResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchProjectionService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchConfigurationTest {

	// Test constants
	private static final String TEST_JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
	private static final Long TEST_JOB_EXECUTION_ID = 12345L;

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
	private BatchJobExecutionListener jobExecutionListener;

	@Mock
	private BatchResultAggregationService resultAggregationService;

	@Mock
	private BatchProjectionService batchProjectionService;

	@Mock
	private ObjectMapper objectMapper;

	@TempDir
	Path tempDir;

	private BatchConfiguration configuration;

	@BeforeEach
	void setUp() {
		configuration = new BatchConfiguration(
				jobRepository, metricsCollector, batchProperties, resultAggregationService
		);

		when(batchProperties.getRetry()).thenReturn(retry);
		when(batchProperties.getSkip()).thenReturn(skip);
		when(batchProperties.getThreadPool()).thenReturn(threadPool);
		when(batchProperties.getPartition()).thenReturn(partition);
		when(batchProperties.getReader()).thenReturn(reader);
		when(batchProperties.getRootDirectory()).thenReturn(tempDir.toString());

		when(retry.getMaxAttempts()).thenReturn(3);
		when(retry.getBackoffPeriod()).thenReturn(1000);
		when(skip.getMaxCount()).thenReturn(5);
		when(threadPool.getCorePoolSize()).thenReturn(4);
		when(threadPool.getMaxPoolSizeMultiplier()).thenReturn(2);
		when(threadPool.getThreadNamePrefix()).thenReturn("VDYP-Worker-");
		when(reader.getDefaultChunkSize()).thenReturn(10);
	}

	@Test
	void testMasterStep_WithValidComponents() {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		Step workerStep = mock(Step.class);
		DynamicPartitioner dynamicPartitioner = mock(DynamicPartitioner.class);
		DynamicPartitionHandler dynamicPartitionHandler = mock(DynamicPartitionHandler.class);

		Step result = configuration.masterStep(taskExecutor, workerStep, dynamicPartitioner, dynamicPartitionHandler);

		assertNotNull(result);
		assertEquals("masterStep", result.getName());
	}

	@Test
	void testWorkerStep_WithValidConfiguration() {
		BatchRetryPolicy retryPolicy = mock(BatchRetryPolicy.class);
		BatchSkipPolicy skipPolicy = mock(BatchSkipPolicy.class);
		@SuppressWarnings("unchecked")
		ItemStreamReader<BatchChunkMetadata> itemReader = mock(ItemStreamReader.class);
		BatchItemWriter partitionWriter = mock(BatchItemWriter.class);
		BatchItemProcessor projectionProcessor = mock(BatchItemProcessor.class);

		Step result = configuration.workerStep(
				retryPolicy, skipPolicy, transactionManager, metricsCollector, batchProperties, batchProjectionService,
				itemReader, partitionWriter, projectionProcessor
		);

		assertNotNull(result);
		assertEquals("workerStep", result.getName());
		verify(reader).getDefaultChunkSize();
	}

	@Test
	void testPartitionedJob_WithValidSteps() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job result = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		assertNotNull(result);
		assertEquals("VdypPartitionedJob", result.getName());
	}

	@Test
	void testPostProcessingStep_WithValidTransactionManager() {
		Step result = configuration.postProcessingStep(transactionManager);

		assertNotNull(result);
		assertEquals("postProcessingStep", result.getName());
	}

	@Test
	void testResultAggregationTasklet_WithSuccessfulAggregation() throws Exception //
	{
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
	void testResultAggregationTasklet_WithCleanupEnabled()
	throws Exception //
	{
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
	void testResultAggregationTasklet_WithValidationFailed()
	throws Exception //
	{
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
	void testResultAggregationTasklet_WithIOException() throws BatchResultAggregationException {
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

		IOException ioException = new IOException("Test IO error");
		BatchResultAggregationException wrappedException = BatchResultAggregationException
				.handleResultAggregationFailure(
						ioException, "I/O operation failed during result aggregation", TEST_JOB_GUID,
						TEST_JOB_EXECUTION_ID, LoggerFactory.getLogger(getClass())
				);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(wrappedException);

		BatchResultAggregationException exception = Assertions
				.assertThrows(BatchResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("I/O operation failed"));
		Assertions.assertNotNull(exception.getCause());
		Assertions.assertTrue(exception.getCause() instanceof IOException);
		Assertions.assertTrue(exception.getCause().getMessage().contains("Test IO error"));
	}

	@Test
	void testResultAggregationTasklet_WithBatchException() throws BatchResultAggregationException {
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

		BatchResultAggregationException testException = BatchResultAggregationException.handleResultAggregationFailure(
				new RuntimeException("Test runtime error"), "Unexpected error during result aggregation", TEST_JOB_GUID,
				TEST_JOB_EXECUTION_ID, LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(testException);

		BatchResultAggregationException exception = Assertions
				.assertThrows(BatchResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("Unexpected error"));
		Assertions.assertTrue(exception.getMessage().contains("Test runtime error"));
	}

	@Test
	void testResultAggregationTasklet_WithExceptionNullMessage() throws BatchResultAggregationException {
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

		BatchResultAggregationException testException = BatchResultAggregationException.handleResultAggregationFailure(
				new RuntimeException((String) null), "Unexpected error during result aggregation", TEST_JOB_GUID,
				TEST_JOB_EXECUTION_ID, LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService.aggregateResultsFromJobDir(
						eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), anyString(), anyString()
				)
		).thenThrow(testException);

		BatchResultAggregationException exception = Assertions
				.assertThrows(BatchResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));

		Assertions.assertTrue(exception.getMessage().contains("Unexpected error"));
		Assertions.assertNotNull(exception.getMessage());
	}

	@Test
	void testResultAggregationTasklet_WithCleanupDisabled()
	throws Exception //
	{
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
	void testResultAggregationTasklet_WithNullJobBaseDir() throws BatchResultAggregationException {
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

		BatchResultAggregationException testException = BatchResultAggregationException.handleResultAggregationFailure(
				new NullPointerException("jobBaseDir cannot be null"),
				"Failed to aggregate results with null jobBaseDir", TEST_JOB_GUID, TEST_JOB_EXECUTION_ID,
				org.slf4j.LoggerFactory.getLogger(getClass())
		);

		when(
				resultAggregationService
						.aggregateResultsFromJobDir(eq(TEST_JOB_EXECUTION_ID), eq(TEST_JOB_GUID), eq(null), anyString())
		).thenThrow(testException);

		Assertions
				.assertThrows(BatchResultAggregationException.class, () -> tasklet.execute(contribution, chunkContext));
	}

	@Test
	void testAsyncJobLauncher() throws Exception //
	{
		TaskExecutor taskExecutor = mock(TaskExecutor.class);

		var jobLauncher = configuration.asyncJobLauncher(taskExecutor);

		assertNotNull(jobLauncher);
	}

	@Test
	void testRetryPolicy() {
		var retryPolicy = configuration.retryPolicy();

		assertNotNull(retryPolicy);
		verify(retry).getMaxAttempts();
		verify(retry).getBackoffPeriod();
	}

	@Test
	void testSkipPolicy() {
		var skipPolicy = configuration.skipPolicy();

		assertNotNull(skipPolicy);
		verify(skip).getMaxCount();
	}

	@Test
	void testTaskExecutor() {
		var taskExecutor = configuration.taskExecutor();

		assertNotNull(taskExecutor);
		verify(threadPool).getCorePoolSize();
		verify(threadPool).getMaxPoolSizeMultiplier();
		verify(threadPool).getThreadNamePrefix();
	}

	@Test
	void testDynamicPartitioner() {
		var partitioner = configuration.dynamicPartitioner();

		assertNotNull(partitioner);
	}

	@Test
	void testDynamicPartitionHandler() {
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		Step workerStep = mock(Step.class);
		DynamicPartitioner dynamicPartitioner = mock(DynamicPartitioner.class);

		var handler = configuration
				.dynamicPartitionHandler(taskExecutor, workerStep, dynamicPartitioner, batchProperties);

		assertNotNull(handler);
	}

	@Test
	void testPartitionReader() {
		var partitionReader = configuration
				.partitionReader("partition-1", TEST_JOB_EXECUTION_ID, TEST_JOB_GUID, batchProperties);

		assertNotNull(partitionReader);
		verify(reader, org.mockito.Mockito.times(2)).getDefaultChunkSize();
	}

	@Test
	void testBatchItemProcessor() {
		var processor = configuration.batchItemProcessor(metricsCollector);

		assertNotNull(processor);
	}

	@Test
	void testPartitionWriter() {
		var writer = configuration.partitionWriter(batchProjectionService, objectMapper);

		assertNotNull(writer);
	}

	@Test
	void testPartitionedJob_Configuration() {
		Step masterStep = mock(Step.class);
		Step postProcessingStep = mock(Step.class);

		Job job = configuration
				.partitionedJob(jobExecutionListener, masterStep, postProcessingStep, transactionManager);

		assertNotNull(job);
		assertEquals("VdypPartitionedJob", job.getName());
	}

}
