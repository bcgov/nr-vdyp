package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.retry.RetryContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchRetryPolicyTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private RetryContext retryContext;

	@Mock
	private StepContext stepContext;

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
	void testBeforeStep_withNullExecutionContext() {
		// Test the behavior when ExecutionContext is null
		when(stepExecution.getJobExecutionId()).thenReturn(123L);
		when(stepExecution.getExecutionContext()).thenReturn(null);

		// This should throw NullPointerException due to null ExecutionContext
		assertThrows(NullPointerException.class, () -> batchRetryPolicy.beforeStep(stepExecution));

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getExecutionContext();
	}

	@Test
	void testCanRetry_withRetryableException() {
		IOException retryableException = new IOException("Test IO exception with Feature ID 1098765432");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
		verify(retryContext, atLeast(1)).getRetryCount();
	}

	@Test
	void testCanRetry_withNonRetryableException() {
		RuntimeException nonRetryableException = new RuntimeException("Test runtime exception");
		when(retryContext.getLastThrowable()).thenReturn(nonRetryableException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_withNullException() {
		when(retryContext.getLastThrowable()).thenReturn(null);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testCanRetry_withExceptionButNullMessage() {
		IOException exceptionWithNullMessage = new IOException((String) null);
		when(retryContext.getLastThrowable()).thenReturn(exceptionWithNullMessage);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
	}

	@Test
	void testCanRetry_maxAttemptsReached() {
		IOException retryableException = new IOException("Test exception with Feature ID 1198765433");
		when(retryContext.getLastThrowable()).thenReturn(retryableException);
		when(retryContext.getRetryCount()).thenReturn(3); // Max attempts reached

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
	}

	@Test
	void testCanRetry_withDataAccessException() {
		var dataAccessException = new org.springframework.dao.DataAccessResourceFailureException(
				"Database connection failed with Feature ID 1298765434"
		);
		when(retryContext.getLastThrowable()).thenReturn(dataAccessException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		// DataAccessResourceFailureException is not in retryable exceptions, so it
		// should return false
		assertFalse(result);
	}

	@Test
	void testRegisterRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1145678901");

		assertDoesNotThrow(() -> batchRetryPolicy.registerRecord(1145678901L, batchRecord));
	}

	@Test
	void testRegisterRecord_withNullRecord() {
		assertDoesNotThrow(() -> batchRetryPolicy.registerRecord(1145678901L, null));
	}

	@Test
	void testOnRetrySuccess() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1245678902");

		// First register a feature that had retry attempts
		batchRetryPolicy.registerRecord(1245678902L, batchRecord);

		// Simulate a retry attempt first
		IOException exception = new IOException("Error with Feature ID 1245678902");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);
		batchRetryPolicy.canRetry(retryContext);

		// Then test successful retry
		assertDoesNotThrow(() -> batchRetryPolicy.onRetrySuccess(1245678902L, batchRecord));
	}

	@Test
	void testOnRetrySuccess_withNoRetryAttempts() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1345678903");

		// Call onRetrySuccess without any previous retry attempts
		assertDoesNotThrow(() -> batchRetryPolicy.onRetrySuccess(1345678903L, batchRecord));
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "Processing failed for Feature ID 1045678904 in batch",
					"Generic error message without Feature ID", "Error with Feature ID notanumber",
					"Complex error message with Feature ID 1999888777 and other text",
					"Error with Feature ID 1555666777 and 1666777888 numbers" }
	)
	void testExtractFeatureId_withVariousMessages(String errorMessage) {
		IOException exception = new IOException(errorMessage);
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertTrue(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
	}

	@Test
	void testBackoffDelay() {
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 50);
		IOException exception = new IOException("Test exception with Feature ID 1777888999");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		long startTime = System.currentTimeMillis();
		boolean result = policyWithBackoff.canRetry(retryContext);
		long endTime = System.currentTimeMillis();

		assertTrue(result);
		// Verify that some delay occurred (allowing for timing variance)
		assertTrue(endTime - startTime >= 40); // Slightly less than 50ms to account for timing variance
	}

	@Test
	void testInterruptedBackoff() {
		BatchRetryPolicy policyWithBackoff = new BatchRetryPolicy(3, 100);
		IOException exception = new IOException("Test exception");
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		// Interrupt current thread
		Thread.currentThread().interrupt();

		assertDoesNotThrow(() -> {
			boolean result = policyWithBackoff.canRetry(retryContext);
			assertTrue(result);
		});

		// Clear interrupted status
		Thread.interrupted();
	}

	@Test
	void testSetMetricsCollector() {
		BatchMetricsCollector newCollector = mock(BatchMetricsCollector.class);
		assertDoesNotThrow(() -> batchRetryPolicy.setMetricsCollector(newCollector));
	}

	@Test
	void testSetMetricsCollector_withNull() {
		assertDoesNotThrow(() -> batchRetryPolicy.setMetricsCollector(null));
	}

	@Test
	void testStepSynchronizationManager_withException() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			mockedStatic.when(StepSynchronizationManager::getContext)
					.thenThrow(new RuntimeException("Step context error"));

			IOException exception = new IOException("Error with Feature ID 1700800900");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			// Should handle the exception gracefully
			boolean result = batchRetryPolicy.canRetry(retryContext);
			assertTrue(result);
		}
	}

	@Test
	void testStepSynchronizationManager_withNullContext() {
		try (MockedStatic<StepSynchronizationManager> mockedStatic = mockStatic(StepSynchronizationManager.class)) {
			mockedStatic.when(StepSynchronizationManager::getContext).thenReturn(null);

			IOException exception = new IOException("Error with Feature ID 1800900100");
			when(retryContext.getLastThrowable()).thenReturn(exception);
			when(retryContext.getRetryCount()).thenReturn(1);

			boolean result = batchRetryPolicy.canRetry(retryContext);
			assertTrue(result);
		}
	}

	@Test
	void testMultipleRetryAttempts() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("1445678905");

		batchRetryPolicy.registerRecord(1445678905L, batchRecord);

		// First retry attempt
		IOException exception1 = new IOException("First error with Feature ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception1);
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Second retry attempt
		IOException exception2 = new IOException("Second error with Feature ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception2);
		when(retryContext.getRetryCount()).thenReturn(2);
		assertTrue(batchRetryPolicy.canRetry(retryContext));

		// Third retry attempt (max reached)
		IOException exception3 = new IOException("Third error with Feature ID 1445678905");
		when(retryContext.getLastThrowable()).thenReturn(exception3);
		when(retryContext.getRetryCount()).thenReturn(3);
		assertFalse(batchRetryPolicy.canRetry(retryContext));
	}

	@Test
	void testCreateRetryKey_differentThreads() {
		BatchRecord batchRecord1 = new BatchRecord();
		batchRecord1.setFeatureId("1545678906");

		BatchRecord batchRecord2 = new BatchRecord();
		batchRecord2.setFeatureId("1545678906"); // Same featureId but should be tracked separately per thread

		batchRetryPolicy.registerRecord(1545678906L, batchRecord1);
		batchRetryPolicy.registerRecord(1545678906L, batchRecord2);

		// The internal retry key should include thread name, making them unique
		assertDoesNotThrow(() -> {
			batchRetryPolicy.onRetrySuccess(1545678906L, batchRecord1);
		});
	}

	@Test
	void testProcessNonRetryableException() {
		RuntimeException nonRetryableException = new RuntimeException("Non-retryable error");
		when(retryContext.getLastThrowable()).thenReturn(nonRetryableException);

		boolean result = batchRetryPolicy.canRetry(retryContext);

		assertFalse(result);
		verify(retryContext, atLeast(1)).getLastThrowable();
	}

	@Test
	void testExtractFeatureId_withNullMessage() {
		IOException exception = new IOException((String) null);
		when(retryContext.getLastThrowable()).thenReturn(exception);
		when(retryContext.getRetryCount()).thenReturn(1);

		// Should handle null message gracefully
		boolean result = batchRetryPolicy.canRetry(retryContext);
		assertTrue(result);
	}

	@Test
	void testCanRetry_withRetryableExceptionInMap() {
		// Test that IOException is properly recognized as retryable
		IOException ioException = new IOException("IO error");
		when(retryContext.getLastThrowable()).thenReturn(ioException);
		when(retryContext.getRetryCount()).thenReturn(1);

		boolean result = batchRetryPolicy.canRetry(retryContext);
		assertTrue(result);
	}

	@Test
	void testCreateRetryableExceptions() {
		// Test the static method indirectly by checking behavior
		BatchRetryPolicy policy = new BatchRetryPolicy(3, 0);

		// IOException should be retryable
		when(retryContext.getLastThrowable()).thenReturn(new IOException("test"));
		when(retryContext.getRetryCount()).thenReturn(1);
		assertTrue(policy.canRetry(retryContext));

		// RuntimeException should not be retryable
		when(retryContext.getLastThrowable()).thenReturn(new RuntimeException("test"));
		assertFalse(policy.canRetry(retryContext));
	}

}