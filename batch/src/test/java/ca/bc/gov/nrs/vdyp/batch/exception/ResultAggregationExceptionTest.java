package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

class ResultAggregationExceptionTest {

	@Test
	void testHandleResultAggregationFailure_WithValidParameters() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 12345L;
		String jobGuid = "test-guid-001";
		Exception cause = new IOException("Failed to write aggregated results");
		String errorDescription = "Failed to aggregate partition results";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(jobExecutionId.toString()));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertTrue(exception.getMessage().contains("IOException"));
		assertTrue(exception.getMessage().contains("Failed to write aggregated results"));

		verify(mockLogger).error(exception.getMessage(), cause);
	}

	@Test
	void testHandleResultAggregationFailure_WithNullCauseMessage() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 67890L;
		String jobGuid = "test-guid-002";
		Exception cause = new RuntimeException((String) null);
		String errorDescription = "Aggregation failed without message";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(BatchConstants.ErrorMessage.NO_ERROR_MESSAGE));
		assertTrue(exception.getMessage().contains("RuntimeException"));
		assertTrue(exception.getMessage().contains(errorDescription));

		verify(mockLogger).error(exception.getMessage(), cause);
	}

	@Test
	void testHandleResultAggregationFailure_MessageFormat() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 11111L;
		String jobGuid = "guid-12345";
		Exception cause = new IllegalStateException("Invalid state");
		String errorDescription = "Result merge failed";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		String expectedPattern = String.format(
				"[GUID: %s, EXEID: %d] %s. Exception type: IllegalStateException, Root cause: Invalid state", jobGuid,
				jobExecutionId, errorDescription
		);

		assertEquals(expectedPattern, exception.getMessage());
		verify(mockLogger).error(any(String.class), eq(cause));
	}

	@Test
	void testExceptionProperties_NotSkippable() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 33333L;
		String jobGuid = "guid-33333";
		Exception cause = new IOException("I/O error");
		String errorDescription = "Failed to merge results";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertFalse(exception.isSkippable(), "ResultAggregationException should not be skippable");
	}

	@Test
	void testExceptionProperties_RecordIdIsNull() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 44444L;
		String jobGuid = "guid-44444";
		Exception cause = new IOException("I/O error");
		String errorDescription = "Aggregation error";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNull(
				exception.getFeatureId(),
				"ResultAggregationException should have null recordId (affects entire job, not specific record)"
		);
	}

	@Test
	void testIsBatchException() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 55555L;
		String jobGuid = "guid-55555";
		Exception cause = new RuntimeException("Test");
		String errorDescription = "Test aggregation error";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertTrue(exception instanceof BatchException);
		assertTrue(exception instanceof Exception);
	}

	@Test
	void testExceptionChaining() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 66666L;
		String jobGuid = "guid-66666";
		Throwable rootCause = new IllegalArgumentException("Root cause");
		Exception intermediateCause = new RuntimeException("Intermediate cause", rootCause);
		String errorDescription = "Aggregation failed with chained exceptions";

		ResultAggregationException exception = ResultAggregationException.handleResultAggregationFailure(
				intermediateCause, errorDescription, jobGuid, jobExecutionId, mockLogger
		);

		assertNotNull(exception);
		assertSame(intermediateCause, exception.getCause());
		assertSame(rootCause, exception.getCause().getCause());
	}

	@Test
	void testHandleResultAggregationFailure_WithEmptyErrorDescription() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 77777L;
		String jobGuid = "guid-77777";
		Exception cause = new IOException("I/O failure");
		String errorDescription = "";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(jobExecutionId.toString()));
		verify(mockLogger).error(any(String.class), eq(cause));
	}

	@Test
	void testHandleResultAggregationFailure_DifferentExceptionTypes() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 88888L;
		String jobGuid = "guid-88888";
		String errorDescription = "Test different exception types";

		Exception ioException = new IOException("IO error");
		ResultAggregationException result1 = ResultAggregationException
				.handleResultAggregationFailure(ioException, errorDescription, jobGuid, jobExecutionId, mockLogger);
		assertTrue(result1.getMessage().contains("IOException"));

		Exception npeException = new NullPointerException("NPE error");
		ResultAggregationException result2 = ResultAggregationException
				.handleResultAggregationFailure(npeException, errorDescription, jobGuid, jobExecutionId, mockLogger);
		assertTrue(result2.getMessage().contains("NullPointerException"));

		Exception iseException = new IllegalStateException("State error");
		ResultAggregationException result3 = ResultAggregationException
				.handleResultAggregationFailure(iseException, errorDescription, jobGuid, jobExecutionId, mockLogger);
		assertTrue(result3.getMessage().contains("IllegalStateException"));
	}

	@Test
	void testSerialVersionUID() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 99999L;
		String jobGuid = "guid-99999";
		Exception cause = new RuntimeException("Test");
		String errorDescription = "Test serialization";

		ResultAggregationException exception = ResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertTrue(exception instanceof java.io.Serializable);
	}
}
