package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchMetricsExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchMetricsExceptionTest.class);

	@Test
	void testHandleMetricsFailureWithFullContext() {
		String jobGuid = "test-guid-123";
		Long jobExecutionId = 456L;
		String errorDescription = "Job metrics already exists";

		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure(errorDescription, jobGuid, jobExecutionId, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("[GUID: test-guid-123, EXEID: 456]"));
		assertThat(exception.getMessage(), containsString("Metrics error: Job metrics already exists"));
		assertThat(exception.getJobGuid(), is(jobGuid));
		assertThat(exception.getJobExecutionId(), is(jobExecutionId));
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getCause(), is(nullValue()));
	}

	@Test
	void testHandleMetricsFailureWithoutJobExecutionId() {
		String jobGuid = "test-guid-789";
		String errorDescription = "No metrics found";

		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure(errorDescription, jobGuid, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("[GUID: test-guid-789]"));
		assertThat(exception.getMessage(), containsString("Metrics error: No metrics found"));
		assertThat(exception.getJobGuid(), is(jobGuid));
		assertThat(exception.getJobExecutionId(), is(nullValue()));
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testHandleMetricsFailureWithoutJobContext() {
		String errorDescription = "Keep count must be non-negative, got: -1";

		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure(errorDescription, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), is("Metrics error: Keep count must be non-negative, got: -1"));
		assertThat(exception.getJobGuid(), is(nullValue()));
		assertThat(exception.getJobExecutionId(), is(nullValue()));
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testExceptionIsNotRetryable() {
		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure("Test error", "guid-123", 456L, logger);

		assertThat(exception.isRetryable(), is(false));
	}

	@Test
	void testExceptionIsNotSkippable() {
		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure("Test error", "guid-123", 456L, logger);

		assertThat(exception.isSkippable(), is(false));
	}

	@Test
	void testExceptionHasNoCause() {
		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure("Test error", "guid-123", 456L, logger);

		assertThat(exception.getCause(), is(nullValue()));
	}

	@Test
	void testPartitionMetricsNotFound() {
		String jobGuid = "test-guid-abc";
		Long jobExecutionId = 789L;
		String partitionName = "partition-1";
		String errorDescription = "Partition metrics not found for partition " + partitionName;

		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure(errorDescription, jobGuid, jobExecutionId, logger);

		assertThat(exception.getMessage(), containsString("[GUID: test-guid-abc, EXEID: 789]"));
		assertThat(exception.getMessage(), containsString("Partition metrics not found for partition partition-1"));
		assertThat(exception.getJobGuid(), is(jobGuid));
		assertThat(exception.getJobExecutionId(), is(jobExecutionId));
	}

	@Test
	void testDuplicateJobMetricsError() {
		String jobGuid = "duplicate-guid";
		Long jobExecutionId = 999L;
		String errorDescription = "Job metrics already exists";

		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure(errorDescription, jobGuid, jobExecutionId, logger);

		assertThat(exception.getMessage(), containsString("Job metrics already exists"));
		assertThat(exception.getJobGuid(), is(jobGuid));
		assertThat(exception.getJobExecutionId(), is(jobExecutionId));
	}

	@Test
	void testInvalidParameterError() {
		int invalidValue = -1;
		String errorDescription = "Keep count must be non-negative, got: " + invalidValue;

		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure(errorDescription, logger);

		assertThat(exception.getMessage(), containsString("Keep count must be non-negative, got: -1"));
		assertThat(exception.getJobGuid(), is(nullValue()));
		assertThat(exception.getJobExecutionId(), is(nullValue()));
	}

	@Test
	void testMessageFormatWithFullContext() {
		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure("Test message", "guid-123", 456L, logger);

		String expectedMessage = "[GUID: guid-123, EXEID: 456] Metrics error: Test message";
		assertThat(exception.getMessage(), is(expectedMessage));
	}

	@Test
	void testMessageFormatWithoutExecutionId() {
		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure("Test message", "guid-123", logger);

		String expectedMessage = "[GUID: guid-123] Metrics error: Test message";
		assertThat(exception.getMessage(), is(expectedMessage));
	}

	@Test
	void testMessageFormatWithoutJobContext() {
		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure("Test message", logger);

		String expectedMessage = "Metrics error: Test message";
		assertThat(exception.getMessage(), is(expectedMessage));
	}
}
