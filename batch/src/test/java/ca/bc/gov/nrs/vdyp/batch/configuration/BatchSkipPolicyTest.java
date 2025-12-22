package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileParseException;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
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
		BatchChunkMetadata chunkMetadata = createChunkMetadata("test-partition");
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
				new FlatFileParseException("Parse error", "data", 1), chunkMetadata, TEST_JOB_GUID, 1L,
				"test-partition", logger
		);

		assertThrows(SkipLimitExceededException.class, () -> batchSkipPolicy.shouldSkip(exception, 5));
	}

	@Test
	void testShouldSkip_WithSkippableBatchProjectionException_ReturnsTrue() throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");

		batchSkipPolicy.beforeStep(stepExecution);

		BatchChunkMetadata chunkMetadata = createChunkMetadata("test-partition");
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(new FlatFileParseException("Parse error", "invalid,data", 15), chunkMetadata, TEST_JOB_GUID, 100L, "test-partition", logger);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_WhenStepSynchronizationManagerThrows_StillReturnsTrue() throws SkipLimitExceededException {
		try (MockedStatic<StepSynchronizationManager> mockedManager = mockStatic(StepSynchronizationManager.class)) {
			mockedManager.when(StepSynchronizationManager::getContext).thenThrow(new RuntimeException("Context error"));

			BatchChunkMetadata chunkMetadata = createChunkMetadata("partition-1");
			BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
					new FlatFileParseException("Error", "bad data", 5), chunkMetadata, TEST_JOB_GUID, 1L, "partition-1",
					logger
			);

			boolean result = batchSkipPolicy.shouldSkip(exception, 1);

			assertTrue(result);
		}
	}

	@Test
	void testShouldSkip_WithNullMetricsCollector_ReturnsTrue() throws SkipLimitExceededException {
		BatchSkipPolicy policyWithNullMetrics = new BatchSkipPolicy(5L, null);
		when(stepExecution.getJobExecutionId()).thenReturn(300L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("null-metrics-partition");

		policyWithNullMetrics.beforeStep(stepExecution);

		BatchChunkMetadata chunkMetadata = createChunkMetadata("null-metrics-partition");
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
				new FlatFileParseException("Parse error", "data", 20), chunkMetadata, TEST_JOB_GUID, 300L,
				"null-metrics-partition", logger
		);

		boolean result = policyWithNullMetrics.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_WithNonParseException_ReturnsTrue() throws SkipLimitExceededException {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		batchSkipPolicy.beforeStep(stepExecution);

		BatchChunkMetadata chunkMetadata = createChunkMetadata("test-partition");
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
				new RuntimeException("malformed data"), chunkMetadata, TEST_JOB_GUID, 1L, "test-partition", logger
		);

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	@Test
	void testShouldSkip_MetricsCollectorThrowsBatchException_CatchesAndLogs() throws BatchMetricsException {
		when(stepExecution.getJobExecutionId()).thenReturn(500L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(TEST_JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString("partitionName")).thenReturn("metrics-error-partition");

		batchSkipPolicy.beforeStep(stepExecution);

		BatchChunkMetadata chunkMetadata = createChunkMetadata("metrics-error-partition");
		BatchProjectionException exception = BatchProjectionException.handleProjectionFailure(
				new FlatFileParseException("Parse error", "invalid,data", 10), chunkMetadata, TEST_JOB_GUID,
				500L, "metrics-error-partition", logger
		);

		var metricsException = BatchMetricsException
				.handleMetricsFailure("Failed to record metrics", TEST_JOB_GUID, 500L, logger);

		org.mockito.Mockito.doThrow(metricsException).when(metricsCollector)
				.recordSkip(500L, TEST_JOB_GUID, null, exception, "metrics-error-partition");

		boolean result = batchSkipPolicy.shouldSkip(exception, 1);

		assertTrue(result);
	}

	private BatchChunkMetadata createChunkMetadata(String partitionName) {
		return new BatchChunkMetadata(partitionName, "/tmp/test-job", 0L, 1, 0L, 0);
	}
}
