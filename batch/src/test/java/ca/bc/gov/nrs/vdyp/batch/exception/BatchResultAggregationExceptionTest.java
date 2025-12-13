package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

class BatchResultAggregationExceptionTest {

	@Test
	void testHandleResultAggregationFailure_WithValidParameters() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 12345L;
		String jobGuid = "test-guid-001";
		IOException cause = new IOException("Failed to write aggregated results");
		String errorDescription = "Failed to aggregate partition results";

		BatchResultAggregationException exception = BatchResultAggregationException
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
		RuntimeException cause = new RuntimeException((String) null);
		String errorDescription = "Aggregation failed without message";

		BatchResultAggregationException exception = BatchResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(BatchConstants.ErrorMessage.NO_ERROR_MESSAGE));
		assertTrue(exception.getMessage().contains("RuntimeException"));
		assertTrue(exception.getMessage().contains(errorDescription));

		verify(mockLogger).error(exception.getMessage(), cause);
	}

	@Test
	void testHandleResultAggregationFailure_WithEmptyErrorDescription() {
		Logger mockLogger = mock(Logger.class);
		Long jobExecutionId = 77777L;
		String jobGuid = "guid-77777";
		IOException cause = new IOException("I/O failure");
		String errorDescription = "";

		BatchResultAggregationException exception = BatchResultAggregationException
				.handleResultAggregationFailure(cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(jobExecutionId.toString()));
		verify(mockLogger).error(exception.getMessage(), cause);
	}
}
