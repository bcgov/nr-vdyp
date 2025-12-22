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

class BatchResultStorageExceptionTest {
	@Test
	void testHandleResultStorageFailure_WithoutFeatureId() {
		IOException cause = new IOException("Permission denied");
		String errorDescription = "Failed to create output directory";
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 456L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

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
	void testHandleResultStorageFailure_WithFeatureId() {
		IOException cause = new IOException("File not found");
		String errorDescription = "Output stream creation failed";
		String jobGuid = "job-guid-789";
		Long jobExecutionId = 999L;
		String featureId = "POLY-123";
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(cause, errorDescription, jobGuid, jobExecutionId, featureId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(featureId));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertSame(cause, exception.getCause());

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_WithNullCauseMessage() {
		IOException cause = new IOException((String) null);
		String errorDescription = "Stream write failed";
		String jobGuid = "job-guid-abc";
		Long jobExecutionId = 111L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("No error message available"));
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));

		verify(logger).error(anyString(), any(IOException.class));
	}
}
