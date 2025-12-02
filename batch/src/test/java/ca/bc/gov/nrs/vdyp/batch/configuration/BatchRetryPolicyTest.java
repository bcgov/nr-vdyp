package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class BatchRetryPolicyTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private RetryContext retryContext;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobParameters jobParameters;

	private BatchRetryPolicy batchRetryPolicy;

	private static final String JOB_GUID = "test-job-guid-123";

	@BeforeEach
	void setUp() {
		batchRetryPolicy = new BatchRetryPolicy(3, 100, metricsCollector);
	}

	@Test
	void testConstructor() {
		BatchRetryPolicy policy = new BatchRetryPolicy(5, 200, metricsCollector);
		assertNotNull(policy);
	}

	@Test
	void testBeforeStep_Success() {
		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");

		assertDoesNotThrow(() -> batchRetryPolicy.beforeStep(stepExecution));

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(stepExecution).getExecutionContext();
	}

	@Test
	void testBeforeStep_WithNullExecutionContext() {
		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME)).thenReturn(null);

		assertDoesNotThrow(() -> batchRetryPolicy.beforeStep(stepExecution));
	}

	@Test
	void testCanRetry_WithRetryableException() throws Exception {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		BatchException retryableException = mock(BatchException.class);
		when(retryableException.isRetryable()).thenReturn(true);
		when(retryableException.getMessage()).thenReturn("Test retryable exception");

		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
		verify(metricsCollector).recordRetryAttempt(100L, JOB_GUID, 1, retryableException, false, "partition0");
	}

	@Test
	void testCanRetry_WithNonRetryableException() throws BatchException {
		RuntimeException nonRetryableException = new RuntimeException("Test runtime exception");
		when(retryContext.getLastThrowable()).thenReturn(nonRetryableException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
		verify(metricsCollector, never())
				.recordRetryAttempt(anyLong(), any(), anyInt(), any(), anyBoolean(), anyString());
	}

	@Test
	void testCanRetry_WithNullException() {
		when(retryContext.getLastThrowable()).thenReturn(null);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_MaxAttemptsReached() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		BatchException retryableException = mock(BatchException.class);
		when(retryableException.isRetryable()).thenReturn(true);
		when(retryableException.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(3);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_WithTransientDataAccessException() throws BatchException {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		var transientException = new TransientDataAccessException("Database temporarily unavailable") {
		};
		when(retryContext.getLastThrowable()).thenReturn(transientException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
		verify(metricsCollector, never())
				.recordRetryAttempt(anyLong(), any(), anyInt(), any(), anyBoolean(), anyString());
	}

	@Test
	void testCanRetry_WithNonTransientDataAccessException() {
		var dataAccessException = new DataAccessResourceFailureException("Database connection failed permanently");
		when(retryContext.getLastThrowable()).thenReturn(dataAccessException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testBackoffDelay_Applied() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 50, metricsCollector);
		policyWithBackoff.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		long startTime = System.currentTimeMillis();
		boolean result = policyWithBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertTrue(result);
		assertTrue(endTime - startTime >= 40);
	}

	@Test
	void testBackoffDelay_NotAppliedWhenCannotRetry() {
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 1000, null);
		RuntimeException exception = new RuntimeException("Non-retryable");
		when(retryContext.getLastThrowable()).thenReturn(exception);

		long startTime = System.currentTimeMillis();
		boolean result = policyWithBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertFalse(result);
		assertTrue(endTime - startTime < 100); // Should not apply backoff
	}

	@Test
	void testBackoffDelay_InterruptedHandling() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 100, metricsCollector);
		policyWithBackoff.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		Thread.currentThread().interrupt();

		assertDoesNotThrow(() -> {
			boolean result = policyWithBackoff.canRetry(retryContext);
			assertTrue(result);
		});

		Thread.interrupted(); // Clear interrupted status
	}

	@Test
	void testRecordRetryAttempt_WithMetricsCollector() throws Exception {
		when(stepExecution.getJobExecutionId()).thenReturn(999L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition5");
		batchRetryPolicy.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		batchRetryPolicy.canRetry(retryContext);

		verify(metricsCollector).recordRetryAttempt(999L, JOB_GUID, 1, exception, false, "partition5");
	}

	@Test
	void testGetCurrentExecutionContext_WithValidStepContext() throws Exception {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			StepContext stepContext = mock(StepContext.class);
			StepExecution currentStepExecution = mock(StepExecution.class);
			ExecutionContext currentExecutionContext = mock(ExecutionContext.class);

			when(stepExecution.getJobExecutionId()).thenReturn(777L);
			when(stepExecution.getJobExecution()).thenReturn(jobExecution);
			when(jobExecution.getJobParameters()).thenReturn(jobParameters);
			when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
			when(stepExecution.getExecutionContext()).thenReturn(executionContext);
			when(executionContext.getString(BatchConstants.Partition.NAME)).thenReturn("partition99");
			batchRetryPolicy.beforeStep(stepExecution);

			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(stepContext);
			when(stepContext.getStepExecution()).thenReturn(currentStepExecution);
			when(currentStepExecution.getExecutionContext()).thenReturn(currentExecutionContext);
			when(currentExecutionContext.getString(BatchConstants.Partition.NAME, "partition99"))
					.thenReturn("partition99-current");

			BatchException exception = mock(BatchException.class);
			when(exception.isRetryable()).thenReturn(true);
			when(exception.getMessage()).thenReturn("Test exception");

			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);

			assertTrue(result);
			verify(metricsCollector).recordRetryAttempt(777L, JOB_GUID, 1, exception, false, "partition99-current");
		}
	}

	@Test
	void testGetCurrentExecutionContext_WithNullStepContext() throws Exception {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			when(stepExecution.getJobExecutionId()).thenReturn(100L);
			when(stepExecution.getJobExecution()).thenReturn(jobExecution);
			when(jobExecution.getJobParameters()).thenReturn(jobParameters);
			when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
			when(stepExecution.getExecutionContext()).thenReturn(executionContext);
			when(executionContext.getString(BatchConstants.Partition.NAME)).thenReturn("partition0");
			batchRetryPolicy.beforeStep(stepExecution);

			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(null);

			BatchException exception = mock(BatchException.class);
			when(exception.isRetryable()).thenReturn(true);
			when(exception.getMessage()).thenReturn("Test exception");

			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);

			assertTrue(result);
			verify(metricsCollector).recordRetryAttempt(100L, JOB_GUID, 1, exception, false, "partition0");
		}
	}

	@Test
	void testConstructorWithNullMetricsCollector() {
		BatchMetricsCollector newCollector = null;
		assertDoesNotThrow(() -> new BatchRetryPolicy(3, 100, newCollector));
	}

	@Test
	void testConstructorWithMetricsCollector() {
		BatchMetricsCollector newCollector = mock(BatchMetricsCollector.class);
		assertDoesNotThrow(() -> new BatchRetryPolicy(3, 100, newCollector));
	}

	@Test
	void testMultipleRetryAttempts() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		// First retry attempt
		BatchException exception1 = mock(BatchException.class);
		when(exception1.isRetryable()).thenReturn(true);
		when(exception1.getMessage()).thenReturn("First error");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Second retry attempt
		BatchException exception2 = mock(BatchException.class);
		when(exception2.isRetryable()).thenReturn(true);
		when(exception2.getMessage()).thenReturn("Second error");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Third retry attempt (max reached)
		BatchException exception3 = mock(BatchException.class);
		when(exception3.isRetryable()).thenReturn(true);
		when(exception3.getMessage()).thenReturn("Third error");
		when(retryContext.getLastThrowable()).thenReturn(exception3);
		when(retryContext.getRetryCount()).thenReturn(3);
		assertFalse(batchRetryPolicy.canRetry(retryContext));
	}

	@Test
	void testCreateRetryableExceptions() {
		BatchRetryPolicy policy = new BatchRetryPolicy(3, 0, null);

		when(retryContext.getLastThrowable()).thenReturn(new IOException("test"));

		assertFalse(policy.canRetry(retryContext));

		// RuntimeException should not be retryable
		when(retryContext.getLastThrowable()).thenReturn(new RuntimeException("test"));
		assertFalse(policy.canRetry(retryContext));
	}

	@Test
	void testMaxAttemptsReached_LogsError() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(2, 0, metricsCollector);
		policyWithJobId.beforeStep(stepExecution);

		// First attempt
		BatchException exception1 = mock(BatchException.class);
		when(exception1.isRetryable()).thenReturn(true);
		when(exception1.getMessage()).thenReturn("Error attempt 1");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policyWithJobId.canRetry(retryContext));

		// Second attempt - max reached
		BatchException exception2 = mock(BatchException.class);
		when(exception2.isRetryable()).thenReturn(true);
		when(exception2.getMessage()).thenReturn("Error attempt 2");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertFalse(policyWithJobId.canRetry(retryContext));
	}

	@Test
	void testRecordRetryAttempt_WithNullMetricsCollector() {
		BatchRetryPolicy policyWithoutMetrics = new BatchRetryPolicy(3, 0, null);

		when(stepExecution.getJobExecutionId()).thenReturn(400L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME)).thenReturn("partition-no-metrics");
		policyWithoutMetrics.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);

		boolean result = policyWithoutMetrics.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testRecordRetryAttempt_WithNullJobExecutionId() throws BatchException {
		BatchRetryPolicy policyWithMetrics = new BatchRetryPolicy(3, 0, metricsCollector);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);

		boolean result = policyWithMetrics.canRetry(retryContext);

		assertFalse(result);

		verify(metricsCollector, never())
				.recordRetryAttempt(anyLong(), any(), anyInt(), any(), anyBoolean(), anyString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Generic error message", "Another error" })
	@NullSource
	void testCanRetry_WithVariousErrorMessages(String errorMessage) {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn(errorMessage);

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testBackoffDelay_ZeroPeriod() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition0");

		BatchRetryPolicy policyNoBackoff = new BatchRetryPolicy(3, 0, metricsCollector);
		policyNoBackoff.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		long startTime = System.currentTimeMillis();
		boolean result = policyNoBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertTrue(result);
		assertTrue(endTime - startTime < 50);
	}

	@Test
	void testGetCurrentPartitionName_WithNoStepContext() throws Exception {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString(BatchConstants.Job.GUID)).thenReturn(JOB_GUID);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME))
				.thenReturn("partition-initial");
		batchRetryPolicy.beforeStep(stepExecution);

		BatchException exception = mock(BatchException.class);
		when(exception.isRetryable()).thenReturn(true);
		when(exception.getMessage()).thenReturn("Test exception");

		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(metricsCollector).recordRetryAttempt(100L, JOB_GUID, 1, exception, false, "partition-initial");
	}
}
