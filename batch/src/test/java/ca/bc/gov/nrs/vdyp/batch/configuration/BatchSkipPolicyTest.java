package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileParseException;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

@ExtendWith(MockitoExtension.class)
class BatchSkipPolicyTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchSkipPolicyTest.class);

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobParameters jobParameters;

	private BatchSkipPolicy batchSkipPolicy;

	private static final String TEST_JOB_GUID = "test-job-guid-123";

	@BeforeEach
	void setUp() {
		batchSkipPolicy = new BatchSkipPolicy(5L, metricsCollector);
	}

	@Test
	void testShouldSkip_SkipLimitExceeded_ThrowsException() {
		RuntimeException exception = new RuntimeException("Error");

		assertThrows(SkipLimitExceededException.class, () -> batchSkipPolicy.shouldSkip(exception, 5));
	}

	@Test
	void testBeforeStep_SetsJobExecutionId() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("partition1");

		batchSkipPolicy.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(executionContext).getString("partitionName");
	}

	@Test
	void testConstructor_WithValidParameters() {
		BatchSkipPolicy policy = new BatchSkipPolicy(10L, metricsCollector);

		assertNotNull(policy);
	}

	@Test
	void testShouldSkip_ProcessesSkippableExceptionPath_WithMetrics() throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");

		batchSkipPolicy.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(new FlatFileParseException("Parse error", "invalid,data", 15), List.of(batchRecord), TEST_JOB_GUID, 100L, "test-partition", logger);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_WithStepSynchronizationManager_UpdatesContext() throws SkipLimitExceededException {
		StepContext stepContext = mock(StepContext.class);
		StepExecution currentStepExecution = mock(StepExecution.class);
		ExecutionContext currentExecutionContext = mock(ExecutionContext.class);
		JobExecution currentJobExecution = mock(JobExecution.class);
		JobParameters currentJobParameters = mock(JobParameters.class);

		try (MockedStatic<StepSynchronizationManager> mockedManager = mockStatic(StepSynchronizationManager.class)) {
			mockedManager.when(StepSynchronizationManager::getContext).thenReturn(stepContext);
			when(stepContext.getStepExecution()).thenReturn(currentStepExecution);
			when(currentStepExecution.getJobExecutionId()).thenReturn(200L);
			when(currentStepExecution.getJobExecution()).thenReturn(currentJobExecution);
			when(currentJobExecution.getJobParameters()).thenReturn(currentJobParameters);
			when(currentJobParameters.getString("jobGuid")).thenReturn("dynamic-job-guid");
			when(currentStepExecution.getExecutionContext()).thenReturn(currentExecutionContext);
			when(currentExecutionContext.getString("partitionName")).thenReturn("dynamic-partition");

			BatchRecord batchRecord = createValidBatchRecord();
			BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
					new FlatFileParseException("Error", "bad data", 5), List.of(batchRecord), TEST_JOB_GUID, 200L,
					"dynamic-partition", logger
			);

			boolean result = batchSkipPolicy.shouldSkip(exception, 1);

			assertTrue(result);
			verify(stepContext).getStepExecution();
			verify(currentStepExecution).getJobExecutionId();
			verify(currentStepExecution).getExecutionContext();
		}
	}

	@Test
	void testShouldSkip_WithStepSynchronizationManagerException_Handles() throws SkipLimitExceededException {
		try (MockedStatic<StepSynchronizationManager> mockedManager = mockStatic(StepSynchronizationManager.class)) {
			mockedManager.when(StepSynchronizationManager::getContext).thenThrow(new RuntimeException("Context error"));

			BatchRecord batchRecord = createValidBatchRecord();
			BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
					new FlatFileParseException("Error", "bad data", 5), List.of(batchRecord), TEST_JOB_GUID, 1L,
					"partition-1", logger
			);

			boolean result = batchSkipPolicy.shouldSkip(exception, 1);

			assertTrue(result);
		}
	}

	@Test
	void testShouldSkip_WithNullMetricsCollector_DoesNotFail() throws SkipLimitExceededException {
		BatchSkipPolicy policyWithNullMetrics = new BatchSkipPolicy(5L, null);
		when(stepExecution.getJobExecutionId()).thenReturn(300L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("null-metrics-partition");

		policyWithNullMetrics.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
				new FlatFileParseException("Parse error", "data", 20), List.of(batchRecord), TEST_JOB_GUID, 300L,
				"null-metrics-partition", logger
		);

		boolean result = policyWithNullMetrics.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testExtractRecord_WithoutCachedRecord_CreatesBasicRecord() throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		batchSkipPolicy.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(new RuntimeException("malformed data"), List.of(batchRecord), TEST_JOB_GUID, 1L, "test-partition", logger);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	private BatchRecord createValidBatchRecord() {
		return new BatchRecord("12345678901", "12345678901,MAP1", Collections.emptyList(), "test-partition");
	}
}
