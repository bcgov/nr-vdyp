package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class BatchConfigurationExceptionTest {

	@Test
	void testHandleConfigurationFailure_WithValidCause() {
		IllegalArgumentException cause = new IllegalArgumentException("Invalid configuration value");
		String errorDescription = "Failed to initialize batch configuration";
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 456L;
		Logger logger = mock(Logger.class);

		BatchConfigurationException exception = BatchConfigurationException
				.handleConfigurationFailure(cause, errorDescription, jobGuid, jobExecutionId, logger);

		assertNotNull(exception);
		assertThat(exception.getMessage(), containsString("[GUID: " + jobGuid));
		assertThat(exception.getMessage(), containsString("EXEID: " + jobExecutionId));
		assertThat(exception.getMessage(), containsString(errorDescription));
		assertThat(exception.getMessage(), containsString("Exception type: IllegalArgumentException"));
		assertThat(exception.getMessage(), containsString("Root cause: Invalid configuration value"));
		assertThat(exception.getCause(), sameInstance(cause));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));

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
		assertThat(exception.getMessage(), containsString("No error message available"));
		assertThat(exception.getMessage(), containsString(jobGuid));
		assertThat(exception.getMessage(), containsString(String.valueOf(jobExecutionId)));
		assertThat(exception.getMessage(), containsString(errorDescription));

		verify(logger).error(anyString(), any(Throwable.class));
	}
}
