package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchDataReadExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchDataReadExceptionTest.class);

	private static final String TEST_JOB_GUID = "test-job-guid-12345";
	private static final Long TEST_JOB_EXECUTION_ID = 100L;
	private static final String TEST_PARTITION_NAME = "partition-001";
	private static final String TEST_FEATURE_ID = "feature-123";

	@Test
	void testHandleDataReadFailure_WithFeatureId() {
		IOException cause = new IOException("Failed to read data");
		String errorDescription = "Failed to read polygon CSV";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_FEATURE_ID, TEST_PARTITION_NAME,
				logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("[GUID: " + TEST_JOB_GUID));
		assertThat(exception.getMessage(), containsString("EXEID: " + TEST_JOB_EXECUTION_ID));
		assertThat(exception.getMessage(), containsString("Partition: " + TEST_PARTITION_NAME));
		assertThat(exception.getMessage(), containsString("Record: " + TEST_FEATURE_ID));
		assertThat(exception.getMessage(), containsString(errorDescription));
		assertThat(exception.getMessage(), containsString("Exception type: IOException"));
		assertThat(exception.getMessage(), containsString("Root cause: Failed to read data"));
		assertThat(exception.getCause(), is(equalTo(cause)));
		assertThat(exception.getFeatureId(), is(equalTo(TEST_FEATURE_ID)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(true));
	}

	@Test
	void testHandleDataReadFailure_WithoutFeatureId() {
		IOException cause = new IOException("File not found");
		String errorDescription = "Failed to open layer file";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_PARTITION_NAME, logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("[GUID: " + TEST_JOB_GUID));
		assertThat(exception.getMessage(), containsString("EXEID: " + TEST_JOB_EXECUTION_ID));
		assertThat(exception.getMessage(), containsString("Partition: " + TEST_PARTITION_NAME));
		assertThat(exception.getMessage(), containsString(errorDescription));
		assertThat(exception.getCause(), is(equalTo(cause)));
		assertThat(exception.getFeatureId(), is(nullValue()));
	}

	@Test
	void testHandleDataReadFailure_WithNullCauseMessage() {
		IOException cause = new IOException((String) null);
		String errorDescription = "Failed to read chunk";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_PARTITION_NAME, logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("No error message available"));
		assertThat(exception.getCause(), is(equalTo(cause)));
	}
}
