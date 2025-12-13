package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchMetricsExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchMetricsExceptionTest.class);

	@Test
	void testHandleMetricsFailure_WithJobGuidAndExecutionId() {
		String jobGuid = "test-guid-123";
		Long jobExecutionId = 456L;
		String errorDescription = "Job metrics already exists";

		BatchMetricsException exception = BatchMetricsException
				.handleMetricsFailure(errorDescription, jobGuid, jobExecutionId, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(
				exception.getMessage(),
				is("[GUID: test-guid-123, EXEID: 456] Metrics error: Job metrics already exists")
		);
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getCause(), is(nullValue()));
	}

	@Test
	void testHandleMetricsFailure_WithJobGuidOnly() {
		String jobGuid = "test-guid-789";
		String errorDescription = "No metrics found";

		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure(errorDescription, jobGuid, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), is("[GUID: test-guid-789] Metrics error: No metrics found"));
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getCause(), is(nullValue()));
	}

	@Test
	void testHandleMetricsFailure_WithoutJobContext() {
		String errorDescription = "Keep count must be non-negative, got: -1";

		BatchMetricsException exception = BatchMetricsException.handleMetricsFailure(errorDescription, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), is("Metrics error: Keep count must be non-negative, got: -1"));
		assertThat(exception.getFeatureId(), is(nullValue()));
		assertThat(exception.isRetryable(), is(false));
		assertThat(exception.isSkippable(), is(false));
		assertThat(exception.getCause(), is(nullValue()));
	}
}
