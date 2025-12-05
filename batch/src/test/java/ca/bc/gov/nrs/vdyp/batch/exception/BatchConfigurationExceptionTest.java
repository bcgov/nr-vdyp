package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class BatchConfigurationExceptionTest {

	@Test
	void testHandleConfigurationFailure_WithAllParameters() {
		IllegalArgumentException cause = new IllegalArgumentException("Invalid configuration value");
		String errorDescription = "Failed to initialize batch configuration";
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 456L;
		Logger logger = mock(Logger.class);

		BatchConfigurationException exception = BatchConfigurationException
				.handleConfigurationFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));
		assertTrue(exception.getMessage().contains("IllegalArgumentException"));
		assertTrue(exception.getMessage().contains("Invalid configuration value"));
		assertSame(cause, exception.getCause());
		assertFalse(exception.isRetryable());
		assertFalse(exception.isSkippable());

		verify(logger).error(anyString(), any(Throwable.class));
	}

	@Test
	void testHandleConfigurationFailure_WithNullCauseMessage() {
		RuntimeException cause = new RuntimeException((String) null);
		String errorDescription = "Configuration parameter missing";
		String jobGuid = "job-guid-abc";
		Long jobExecutionId = 111L;
		Logger logger = mock(Logger.class);

		BatchConfigurationException exception = BatchConfigurationException
				.handleConfigurationFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("No error message available"));
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(errorDescription));

		verify(logger).error(anyString(), any(Throwable.class));
	}

	@Test
	void testHandleConfigurationFailure_FormatsMessageCorrectly() {
		NullPointerException cause = new NullPointerException("Required property is null");
		String errorDescription = "Failed to read application properties";
		String jobGuid = "test-guid";
		Long jobExecutionId = 12345L;
		Logger logger = mock(Logger.class);

		BatchConfigurationException exception = BatchConfigurationException
				.handleConfigurationFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		String message = exception.getMessage();

		assertTrue(message.contains("[GUID: " + jobGuid));
		assertTrue(message.contains("EXEID: " + jobExecutionId));
		assertTrue(message.contains(errorDescription));
		assertTrue(message.contains("Exception type: NullPointerException"));
		assertTrue(message.contains("Root cause: Required property is null"));

		verify(logger).error(anyString(), any(Throwable.class));
	}
}
