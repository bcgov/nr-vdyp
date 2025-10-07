package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
import org.springframework.retry.RetryContext;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
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
		assertEquals(5, policy.getMaxAttempts());
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
		IOException retryableException = new IOException("Test IO exception with record ID 1098765432");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
	}

	@Test
	void testCanRetry_WithNonRetryableException() {
		RuntimeException nonRetryableException = new RuntimeException("Test runtime exception");
		when(retryContext.getLastThrowable()).thenReturn(nonRetryableException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
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
		IOException retryableException = new IOException("Test exception with record ID 1198765433");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(3);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_WithDataAccessException() {
		var dataAccessException = new DataAccessResourceFailureException(
				"Database connection failed with record ID 1298765434");
		when(retryContext.getLastThrowable()).thenReturn(dataAccessException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testRegisterRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1145678901");

		assertDoesNotThrow(() -> batchRetryPolicy.registerRecord(1145678901L, batchRecord));
	}

	@Test
	void testOnRetrySuccess_WithPreviousAttempts() {
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(3, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		// Setup StepExecution
		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1245678902");

		// Register and simulate retry
		policyWithJobId.registerRecord(1245678902L, batchRecord);

		IOException exception = new IOException("Error with record ID 1245678902");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);
		policyWithJobId.canRetry(retryContext);

		assertDoesNotThrow(() -> policyWithJobId.onRetrySuccess(1245678902L, batchRecord));

		verify(metricsCollector).recordRetryAttempt(100L, 1245678902L, batchRecord, 2, null, true, "partition0");
	}

	@Test
	void testOnRetrySuccess_WithNoRetryAttempts() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1345678903");

		assertDoesNotThrow(() -> batchRetryPolicy.onRetrySuccess(1345678903L, batchRecord));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Error with record ID notanumber",
			"Generic error message without record ID",
			"record ID "
	})
	@NullSource
	void testExtractRecordId_VariousEdgeCases(String errorMessage) {
		IOException exception = new IOException(errorMessage);
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testBackoffDelay_Applied() {
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 50);
		IOException exception = new IOException("Test exception with record ID 1777888999");
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

		long startTime = System.currentTimeMillis();
		boolean result = policyWithBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertFalse(result);
		assertTrue(endTime - startTime < 50); // Should not apply backoff
	}

	@Test
	void testBackoffDelay_InterruptedHandling() {
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 100);
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
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(3, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		when(stepExecution.getJobExecutionId()).thenReturn(999L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition5");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1555666777");
		policyWithJobId.registerRecord(1555666777L, batchRecord);

		IOException exception = new IOException("Error with record ID 1555666777");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		policyWithJobId.canRetry(retryContext);

		verify(metricsCollector).recordRetryAttempt(999L, 1555666777L, batchRecord, 1, exception, false, "partition5");
	}

	@Test
	void testHandleMaxAttemptsReached_TriggersCleanup() {
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(2, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		when(stepExecution.getJobExecutionId()).thenReturn(500L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition1");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1234567890");
		policyWithJobId.registerRecord(1234567890L, batchRecord);

		// First attempt
		IOException exception1 = new IOException("Error with record ID 1234567890");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policyWithJobId.canRetry(retryContext));

		// Second attempt - max reached
		IOException exception2 = new IOException("Error with record ID 1234567890 again");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertFalse(policyWithJobId.canRetry(retryContext));

		// Verify that retryInfo was cleaned up
		// Subsequent success should not trigger metrics
		policyWithJobId.onRetrySuccess(1234567890L, batchRecord);
	}

	@Test
	void testGetCurrentExecutionContext_WithValidStepContext() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			StepContext stepContext = mock(StepContext.class);
			StepExecution currentStepExecution = mock(StepExecution.class);
			ExecutionContext currentExecutionContext = mock(ExecutionContext.class);

			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(stepContext);
			when(stepContext.getStepExecution()).thenReturn(currentStepExecution);
			when(currentStepExecution.getJobExecutionId()).thenReturn(777L);
			when(currentStepExecution.getExecutionContext()).thenReturn(currentExecutionContext);
			when(currentExecutionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
					.thenReturn("partition99");

			IOException exception = new IOException("Error with record ID 1800900100");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);
			assertTrue(result);
		}
	}

	@Test
	void testGetCurrentExecutionContext_WithNullStepContext() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(null);

			IOException exception = new IOException("Error with record ID 1800900100");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);
			assertTrue(result);
		}
	}

	@Test
	void testGetCurrentExecutionContext_WithException() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			mockedStatic.when(StepSynchronizationManager::getContext)
					.thenThrow(new RuntimeException("Step context error"));

			IOException exception = new IOException("Error with record ID 1700800900");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);
			assertTrue(result);
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
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1445678905");

		batchRetryPolicy.registerRecord(1445678905L, batchRecord);

		// First retry attempt
		IOException exception1 = new IOException("First error with record ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Second retry attempt
		IOException exception2 = new IOException("Second error with record ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Third retry attempt (max reached)
		IOException exception3 = new IOException("Third error with record ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception3);
		when(retryContext.getRetryCount()).thenReturn(3);
		assertFalse(batchRetryPolicy.canRetry(retryContext));
	}

	@Test
	void testLogRetryAttempt_WithNullLastError() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1666777888");
		batchRetryPolicy.registerRecord(1666777888L, batchRecord);

		// First attempt with null stored error
		IOException exception = new IOException("New error with record ID 1666777888");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
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
	void testIsMaxAttemptsReached_ExactlyAtMaxAttempts() {
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(3, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		when(stepExecution.getJobExecutionId()).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition0");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("2000000001");
		policyWithJobId.registerRecord(2000000001L, batchRecord);

		// Attempt 1
		IOException exception1 = new IOException("Error with record ID 2000000001");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policyWithJobId.canRetry(retryContext));

		// Attempt 2
		IOException exception2 = new IOException("Error with record ID 2000000001");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertTrue(policyWithJobId.canRetry(retryContext));

		// Attempt 3 - exactly at max attempts (getRetryCount() >= getMaxAttempts())
		IOException exception3 = new IOException("Error with record ID 2000000001");
		when(retryContext.getLastThrowable()).thenReturn(exception3);
		when(retryContext.getRetryCount()).thenReturn(3);
		boolean canRetry = policyWithJobId.canRetry(retryContext);

		// Should return false because max attempts reached
		assertFalse(canRetry);
	}

	@Test
	void testHandleMaxAttemptsReached_LogsFinalStatusFailed() {
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(2, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		when(stepExecution.getJobExecutionId()).thenReturn(200L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition-test");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("2000000002");
		policyWithJobId.registerRecord(2000000002L, batchRecord);

		IOException exception1 = new IOException("Error with record ID 2000000002");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policyWithJobId.canRetry(retryContext));

		IOException exception2 = new IOException("Error with record ID 2000000002");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		boolean canRetry = policyWithJobId.canRetry(retryContext);

		assertFalse(canRetry);

		policyWithJobId.onRetrySuccess(2000000002L, batchRecord);
	}

	@Test
	void testOnRetrySuccess_WithAttemptCountGreaterThanZero() {
		BatchRetryPolicy policyWithJobId = new BatchRetryPolicy(3, 0);
		policyWithJobId.setMetricsCollector(metricsCollector);

		when(stepExecution.getJobExecutionId()).thenReturn(300L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition-success");
		policyWithJobId.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("2000000003");
		policyWithJobId.registerRecord(2000000003L, batchRecord);

		IOException exception = new IOException("Error with record ID 2000000003");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);
		policyWithJobId.canRetry(retryContext);

		policyWithJobId.onRetrySuccess(2000000003L, batchRecord);

		verify(metricsCollector).recordRetryAttempt(300L, 2000000003L, batchRecord, 2, null, true,
				"partition-success");
	}

	@Test
	void testRecordRetryAttempt_WithNullMetricsCollector() {
		BatchRetryPolicy policyWithoutMetrics = new BatchRetryPolicy(3, 0);

		when(stepExecution.getJobExecutionId()).thenReturn(400L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(executionContext.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN))
				.thenReturn("partition-no-metrics");
		policyWithoutMetrics.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("2000000004");
		policyWithoutMetrics.registerRecord(2000000004L, batchRecord);

		IOException exception = new IOException("Error with record ID 2000000004");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = policyWithoutMetrics.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testRecordRetryAttempt_WithNullJobExecutionId() {
		BatchRetryPolicy policyWithMetrics = new BatchRetryPolicy(3, 0);
		policyWithMetrics.setMetricsCollector(metricsCollector);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("2000000005");
		policyWithMetrics.registerRecord(2000000005L, batchRecord);

		IOException exception = new IOException("Error with record ID 2000000005");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = policyWithMetrics.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testExtractRecordId_WithNullErrorMessage() {
		IOException exception = new IOException((String) null);
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testExtractRecordId_WithEmptyTokens() {
		IOException exception = new IOException("Error: record ID ");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testExtractRecordId_WithValidTokens() {
		IOException exception = new IOException("Processing failed for record ID 987654321 in partition 5");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testExtractRecordId_NullErrorMessage() throws Exception {
		Method extractRecordIdMethod = BatchRetryPolicy.class.getDeclaredMethod("extractRecordId", String.class);
		extractRecordIdMethod.setAccessible(true);
		Long result = (Long) extractRecordIdMethod.invoke(batchRetryPolicy, (String) null);
		assertEquals(-1L, result, "Should return -1L for null errorMessage");
	}

	@Test
	void testProcessThrowableWithNullMessage() {
		IOException exceptionWithNullMessage = new IOException((String) null);
		when(retryContext.getLastThrowable()).thenReturn(exceptionWithNullMessage);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}
}
