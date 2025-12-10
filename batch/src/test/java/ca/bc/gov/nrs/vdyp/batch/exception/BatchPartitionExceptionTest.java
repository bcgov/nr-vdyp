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
	void testHandlePartitionFailure_WithException() {
		IOException cause = new IOException("Permission denied");

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, "Failed to create partition file", TEST_JOB_GUID, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-12345]"));
		assertThat(exception.getMessage(), containsString("Failed to create partition file"));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("Permission denied"));
		assertThat(exception.getCause(), is(sameInstance(cause)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getFeatureId(), is(nullValue()));
	}

	@Test
	void testHandlePartitionFailure_WithNullExceptionMessage() {
		IOException cause = new IOException((String) null);

		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure(cause, "Failed to write partition", TEST_JOB_GUID, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-12345]"));
		assertThat(exception.getMessage(), containsString("Failed to write partition"));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("No error message available"));
		assertThat(exception.getCause(), is(sameInstance(cause)));
	}

	@Test
	void testHandlePartitionFailure_WithoutException() {
		BatchPartitionException exception = BatchPartitionException
				.handlePartitionFailure("Failed to initialize partitioner", TEST_JOB_GUID, logger);

		assertThat(exception.getMessage(), notNullValue());
		assertThat(exception.getMessage(), containsString("[GUID: test-job-guid-12345]"));
		assertThat(exception.getMessage(), containsString("Failed to initialize partitioner"));
		assertThat(exception.getCause(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getFeatureId(), is(nullValue()));
	}
}
