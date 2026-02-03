package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class BatchResultPersistenceExceptionTest {
	@Test
	void testHandleResultPersistenceFailure() {
		IOException cause = new IOException("Permission denied");
		String errorDescription = "Failed to create output directory";
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 456L;
		Logger logger = mock(Logger.class);

		BatchResultPersistenceException exception = BatchResultPersistenceException
				.handleResultPersistenceFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertTrue(exception.getMessage().contains("IOException"));
		assertTrue(exception.getMessage().contains("Permission denied"));
		assertSame(cause, exception.getCause());

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultPersistenceFailure_WithNullCauseMessage() {
		IOException cause = new IOException((String) null);
		String errorDescription = "Stream write failed";
		String jobGuid = "job-guid-abc";
		Long jobExecutionId = 111L;
		Logger logger = mock(Logger.class);

		BatchResultPersistenceException exception = BatchResultPersistenceException
				.handleResultPersistenceFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("No error message available"));
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));

		verify(logger).error(anyString(), any(IOException.class));
	}
}
