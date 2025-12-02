package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void testHandleResultStorageFailure_WithAllParameters() {
		String context = "/path/to/output/file.csv";
		IOException cause = new IOException("Permission denied");
		String errorDescription = "Failed to create output directory";
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 456L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertTrue(exception.getMessage().contains(context));
		assertTrue(exception.getMessage().contains("IOException"));
		assertTrue(exception.getMessage().contains("Permission denied"));
		assertSame(cause, exception.getCause());

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_WithNullContext() {
		IOException cause = new IOException("File not found");
		String errorDescription = "Output stream creation failed";
		String jobGuid = "job-guid-789";
		Long jobExecutionId = 999L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(null, cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertFalse(exception.getMessage().contains("null")); // Should not include context
		assertTrue(exception.getMessage().contains("IOException"));
		assertTrue(exception.getMessage().contains("File not found"));

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_WithNullCauseMessage() {
		String context = "output-stream";
		IOException cause = new IOException((String) null);
		String errorDescription = "Stream write failed";
		String jobGuid = "job-guid-abc";
		Long jobExecutionId = 111L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("No error message available"));
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertTrue(exception.getMessage().contains(context));

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_FormatsMessageCorrectly() {
		String context = "/tmp/results/partition-1";
		IOException cause = new IOException("Disk quota exceeded");
		String errorDescription = "Failed to store partition results";
		String jobGuid = "test-guid";
		Long jobExecutionId = 12345L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, logger);

		String message = exception.getMessage();

		assertTrue(message.contains("[GUID: " + jobGuid));
		assertTrue(message.contains("EXEID: " + jobExecutionId));
		assertTrue(message.contains(errorDescription));
		assertTrue(message.contains(context));
		assertTrue(message.contains("Exception type: IOException"));
		assertTrue(message.contains("Root cause: Disk quota exceeded"));

		verify(logger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_LogsToProvidedLogger() {
		String context = "test-file.txt";
		IOException cause = new IOException("Test error");
		String errorDescription = "Test operation failed";
		String jobGuid = "guid";
		Long jobExecutionId = 1L;
		Logger mockLogger = mock(Logger.class);

		BatchResultStorageException
				.handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, mockLogger);

		verify(mockLogger).error(anyString(), any(IOException.class));
	}

	@Test
	void testHandleResultStorageFailure_WithDifferentIOExceptionTypes() {
		String context = "/output/directory";
		IOException cause = new java.io.FileNotFoundException("Directory not found");
		String errorDescription = "Cannot access output directory";
		String jobGuid = "guid-xyz";
		Long jobExecutionId = 777L;
		Logger logger = mock(Logger.class);

		BatchResultStorageException exception = BatchResultStorageException
				.handleResultStorageFailure(context, cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("FileNotFoundException"));
		assertTrue(exception.getMessage().contains("Directory not found"));

		verify(logger).error(anyString(), any(IOException.class));
	}
}
