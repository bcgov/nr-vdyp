package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchPartitionExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchPartitionExceptionTest.class);

	private static final String TEST_JOB_GUID = "test-job-guid-12345";

	@Test
	void testConstructorWithIoException() {
		IOException cause = new IOException();
		String message = "Failed to write partition file";

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, message, TEST_JOB_GUID, logger);

		assertThat(exception.getCause(), is(sameInstance(cause)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getJobExecutionId(), is(nullValue()));
		assertThat(exception.getFeatureId(), is(nullValue()));
	}

	@Test
	void testHandlePartitionIoFailureWithContext() {
		IOException cause = new IOException("Permission denied");
		String jobGuid = "test-job-guid-123";

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, "Failed to create partition file", jobGuid, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-123]"));
		assertThat(exception.getMessage(), containsString("Failed to create partition file"));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("Permission denied"));
		assertThat(exception.getCause(), is(sameInstance(cause)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testHandlePartitionIoFailureWithoutContext() {
		IOException cause = new IOException("Network timeout");
		String jobGuid = "test-job-guid-456";

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, "Failed to read input file", jobGuid, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-456]"));
		assertThat(exception.getMessage(), containsString("Failed to read input file"));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("Network timeout"));
		assertThat(exception.getCause(), is(sameInstance(cause)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testHandlePartitionIoFailureWithNullCauseMessage() {
		IOException cause = new IOException((String) null);
		String jobGuid = "test-job-guid-789";

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, "Failed to write partition", jobGuid, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-789]"));
		assertThat(exception.getMessage(), containsString("Failed to write partition"));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("No error message available"));
		assertThat(exception.getCause(), is(sameInstance(cause)));
	}
}
