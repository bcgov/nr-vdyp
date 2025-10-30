package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;

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

	private BatchRetryPolicy batchRetryPolicy;

	@BeforeEach
	void setUp() {
		batchRetryPolicy = new BatchRetryPolicy(3, 100);
		batchRetryPolicy.setMetricsCollector(metricsCollector);
	}

	@Test
	void testConstructor() {
		BatchRetryPolicy policy = new BatchRetryPolicy(5, 200);
		assertNotNull(policy);
	}

	@Test
	void testBeforeStep_Success() {
		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");

		assertDoesNotThrow(() -> batchRetryPolicy.beforeStep(stepExecution));

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getExecutionContext();
	}

	@Test
	void testBeforeStep_WithNullExecutionContext() {
		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getExecutionContext()).thenReturn(null);

		assertThrows(NullPointerException.class, () -> batchRetryPolicy.beforeStep(stepExecution));
	}

	@Test
	void testCanRetry_WithRetryableException() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		IOException retryableException = new IOException("Test IO exception");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
		verify(metricsCollector).recordRetryAttempt(100L, 1, retryableException, false, "partition0");
	}

	@Test
	void testCanRetry_WithNonRetryableException() {
		RuntimeException nonRetryableException = new RuntimeException("Test runtime exception");
		when(retryContext.getLastThrowable()).thenReturn(nonRetryableException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
		verify(metricsCollector, never()).recordRetryAttempt(anyLong(), anyInt(), any(), anyBoolean(), anyString());
	}

	@Test
	void testCanRetry_WithNullException() {
		when(retryContext.getLastThrowable()).thenReturn(null);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testCanRetry_MaxAttemptsReached() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		IOException retryableException = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(3);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_WithTransientDataAccessException() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		var transientException = new TransientDataAccessException("Database temporarily unavailable") {
		};
		when(retryContext.getLastThrowable()).thenReturn(transientException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(metricsCollector).recordRetryAttempt(100L, 1, transientException, false, "partition0");
	}

	@Test
	void testCanRetry_WithNonTransientDataAccessException() {
		var dataAccessException = new DataAccessResourceFailureException("Database connection failed permanently");
		when(retryContext.getLastThrowable()).thenReturn(dataAccessException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testBackoffDelay_Applied() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 50);
		policyWithBackoff.setMetricsCollector(metricsCollector);
		policyWithBackoff.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
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
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 100);
		RuntimeException exception = new RuntimeException("Non-retryable");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		long startTime = System.currentTimeMillis();
		boolean result = policyWithBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertFalse(result);
		assertTrue(endTime - startTime < 50); // Should not apply backoff
	}

	@Test
	void testBackoffDelay_InterruptedHandling() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 100);
		policyWithBackoff.setMetricsCollector(metricsCollector);
		policyWithBackoff.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
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
	void testRecordRetryAttempt_WithMetricsCollector() {
		when(stepExecution.getJobExecutionId()).thenReturn(999L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition5");
		batchRetryPolicy.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		batchRetryPolicy.canRetry(retryContext);

		verify(metricsCollector).recordRetryAttempt(999L, 1, exception, false, "partition5");
	}

	@Test
	void testGetCurrentExecutionContext_WithValidStepContext() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			StepContext stepContext = mock(StepContext.class);
			StepExecution currentStepExecution = mock(StepExecution.class);
			ExecutionContext currentExecutionContext = mock(ExecutionContext.class);

			when(stepExecution.getJobExecutionId()).thenReturn(777L);
			when(stepExecution.getExecutionContext()).thenReturn(executionContext);
			when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
					.thenReturn("partition99");
			batchRetryPolicy.beforeStep(stepExecution);

			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(stepContext);
			when(stepContext.getStepExecution()).thenReturn(currentStepExecution);
			when(currentStepExecution.getExecutionContext()).thenReturn(currentExecutionContext);
			when(currentExecutionContext.getString(BatchConstants.Partition.NAME, "partition99"))
					.thenReturn("partition99-current");

			IOException exception = new IOException("Test exception");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);

			assertTrue(result);
			verify(metricsCollector).recordRetryAttempt(777L, 1, exception, false, "partition99-current");
		}
	}

	@Test
	void testGetCurrentExecutionContext_WithNullStepContext() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			when(stepExecution.getJobExecutionId()).thenReturn(100L);
			when(stepExecution.getExecutionContext()).thenReturn(executionContext);
			when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
					.thenReturn("partition0");
			batchRetryPolicy.beforeStep(stepExecution);

			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(null);

			IOException exception = new IOException("Test exception");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);

			assertTrue(result);
			verify(metricsCollector).recordRetryAttempt(100L, 1, exception, false, "partition0");
		}
	}

	@Test
	void testGetCurrentExecutionContext_WithException() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			when(stepExecution.getJobExecutionId()).thenReturn(100L);
			when(stepExecution.getExecutionContext()).thenReturn(executionContext);
			when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
					.thenReturn("partition0");
			batchRetryPolicy.beforeStep(stepExecution);

			mockedStatic.when(StepSynchronizationManager::getContext)
					.thenThrow(new RuntimeException("Step context error"));

			IOException exception = new IOException("Test exception");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);

			assertTrue(result);
			verify(metricsCollector).recordRetryAttempt(100L, 1, exception, false, "partition0");
		}
	}

	@Test
	void testSetMetricsCollector() {
		BatchMetricsCollector newCollector = mock(BatchMetricsCollector.class);
		assertDoesNotThrow(() -> batchRetryPolicy.setMetricsCollector(newCollector));
	}

	@Test
	void testSetMetricsCollector_WithNull() {
		assertDoesNotThrow(() -> batchRetryPolicy.setMetricsCollector(null));
	}

	@Test
	void testMultipleRetryAttempts() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		// First retry attempt
		IOException exception1 = new IOException("First error");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Second retry attempt
		IOException exception2 = new IOException("Second error");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Third retry attempt (max reached)
		IOException exception3 = new IOException("Third error");
		when(retryContext.getLastThrowable()).thenReturn(exception3);
		when(retryContext.getRetryCount()).thenReturn(3);
		assertFalse(batchRetryPolicy.canRetry(retryContext));
	}

	@Test
	void testCreateRetryableExceptions() {
		BatchRetryPolicy policy = new BatchRetryPolicy(3, 0);

		// IOException should be retryable
		when(retryContext.getLastThrowable()).thenReturn(new IOException("test"));
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policy.canRetry(retryContext));

		// RuntimeException should not be retryable
		when(retryContext.getLastThrowable()).thenReturn(new RuntimeException("test"));
		assertFalse(policy.canRetry(retryContext));
	}

	@Test
	void testMaxAttemptsReached_LogsError() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");

		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(2, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);
		policyWithJobId.beforeStep(stepExecution);

		// First attempt
		IOException exception1 = new IOException("Error attempt 1");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policyWithJobId.canRetry(retryContext));

		// Second attempt - max reached
		IOException exception2 = new IOException("Error attempt 2");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertFalse(policyWithJobId.canRetry(retryContext));
	}

	@Test
	void testRecordRetryAttempt_WithNullMetricsCollector() {
		BatchRetryPolicy policyWithoutMetrics = new BatchRetryPolicy(3, 0);

		when(stepExecution.getJobExecutionId()).thenReturn(400L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition-no-metrics");
		policyWithoutMetrics.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = policyWithoutMetrics.canRetry(retryContext);

		assertTrue(result);
		// No metrics collector interaction expected
	}

	@Test
	void testRecordRetryAttempt_WithNullJobExecutionId() {
		BatchRetryPolicy policyWithMetrics = new BatchRetryPolicy(3, 0);
		policyWithMetrics.setMetricsCollector(metricsCollector);

		// Don't call beforeStep - jobExecutionId will be null

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = policyWithMetrics.canRetry(retryContext);

		assertTrue(result);
		// No metrics collector interaction expected when jobExecutionId is null
		verify(metricsCollector, never()).recordRetryAttempt(anyLong(), anyInt(), any(), anyBoolean(), anyString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Generic error message", "Another error" })
	@NullSource
	void testCanRetry_WithVariousErrorMessages(String errorMessage) {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		batchRetryPolicy.beforeStep(stepExecution);

		IOException exception = new IOException(errorMessage);
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testBackoffDelay_ZeroPeriod() {
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");

		BatchRetryPolicy policyNoBackoff = new BatchRetryPolicy(3, 0);
		policyNoBackoff.setMetricsCollector(metricsCollector);
		policyNoBackoff.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		long startTime = System.currentTimeMillis();
		boolean result = policyNoBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertTrue(result);
		assertTrue(endTime - startTime < 50); // Should be immediate with no backoff
	}

	@Test
	void testGetCurrentPartitionName_WithNoStepContext() {
		// Setup initial partition name
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition-initial");
		batchRetryPolicy.beforeStep(stepExecution);

		// No StepSynchronizationManager setup - should use stored partition name
		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(metricsCollector).recordRetryAttempt(100L, 1, exception, false, "partition-initial");
	}

	@Test
	void testGetCurrentPartitionName_WithUnknownDefault() {
		BatchRetryPolicy policyNoPartition = new BatchRetryPolicy(3, 0);
		policyNoPartition.setMetricsCollector(metricsCollector);

		// Setup without partition name (will use UNKNOWN)
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn(BatchConstants.Common.UNKNOWN);
		policyNoPartition.beforeStep(stepExecution);

		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = policyNoPartition.canRetry(retryContext);

		assertTrue(result);
		verify(metricsCollector).recordRetryAttempt(100L, 1, exception, false, BatchConstants.Common.UNKNOWN);
	}
}
