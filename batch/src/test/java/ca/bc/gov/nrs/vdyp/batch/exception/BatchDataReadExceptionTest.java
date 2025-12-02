package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
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
	private static final String TEST_FEATURE_ID = "FEATURE-12345";

	@Test
	void testConstructorWithCauseAndRecordId() {
		IOException cause = new IOException("Failed to read data");
		String message = "Failed to read data";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, message, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_PARTITION_NAME, logger
		);

		assertThat(exception.getCause(), is(instanceOf(IOException.class)));
		assertThat(exception.getCause(), is(equalTo(cause)));
		assertThat(exception.getJobExecutionId(), is(equalTo(TEST_JOB_EXECUTION_ID)));
		assertThat(exception.getJobGuid(), is(equalTo(TEST_JOB_GUID)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testHandleDataReadFailureWithAllContext() {
		IOException cause = new IOException("Unexpected end of file");
		String errorDescription = "Failed to read polygon CSV";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_FEATURE_ID, TEST_PARTITION_NAME,
				logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString(TEST_JOB_GUID));
		assertThat(exception.getMessage(), containsString(String.valueOf(TEST_JOB_EXECUTION_ID)));
		assertThat(exception.getMessage(), containsString(TEST_PARTITION_NAME));
		assertThat(exception.getMessage(), containsString(errorDescription));
		assertThat(exception.getMessage(), containsString("IOException"));
		assertThat(exception.getMessage(), containsString("Unexpected end of file"));
		assertThat(exception.getCause(), is(equalTo(cause)));
		assertThat(exception.getJobExecutionId(), is(equalTo(TEST_JOB_EXECUTION_ID)));
		assertThat(exception.getFeatureId(), is(equalTo(TEST_FEATURE_ID)));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testHandleDataReadFailureWithoutRecordId() {
		IOException cause = new IOException("File not found");
		String errorDescription = "Failed to open layer file";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_PARTITION_NAME, logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString(TEST_JOB_GUID));
		assertThat(exception.getMessage(), containsString(String.valueOf(TEST_JOB_EXECUTION_ID)));
		assertThat(exception.getMessage(), containsString(TEST_PARTITION_NAME));
		assertThat(exception.getMessage(), containsString(errorDescription));
		assertThat(exception.getCause(), is(equalTo(cause)));
		assertThat(exception.getFeatureId(), is(nullValue()));
	}

	@Test
	void testHandleDataReadFailureWithNullCauseMessage() {
		IOException cause = new IOException((String) null);
		String errorDescription = "Failed to read chunk";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_FEATURE_ID, TEST_PARTITION_NAME,
				logger
		);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("No error message available"));
		assertThat(exception.getCause(), is(equalTo(cause)));
	}

	@Test
	void testMessageFormatting() {
		IOException cause = new IOException("Disk full");
		String errorDescription = "Failed to buffer chunk data";

		BatchDataReadException exception = BatchDataReadException.handleDataReadFailure(
				cause, errorDescription, TEST_JOB_GUID, TEST_JOB_EXECUTION_ID, TEST_FEATURE_ID, TEST_PARTITION_NAME,
				logger
		);

		String message = exception.getMessage();

		assertThat(message, containsString("[GUID: " + TEST_JOB_GUID));
		assertThat(message, containsString("EXEID: " + TEST_JOB_EXECUTION_ID));
		assertThat(message, containsString("Partition: " + TEST_PARTITION_NAME));
		assertThat(message, containsString(errorDescription));
		assertThat(message, containsString("Exception type: IOException"));
		assertThat(message, containsString("Root cause: Disk full"));
	}
}
