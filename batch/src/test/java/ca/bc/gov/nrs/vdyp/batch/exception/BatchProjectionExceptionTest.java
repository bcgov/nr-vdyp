package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;

class BatchProjectionExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchProjectionExceptionTest.class);

	@Test
	void testHandleProjectionFailure_WithValidException() {
		RuntimeException cause = new RuntimeException("Projection algorithm failed");
		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata("partition-1", "/tmp/job", 5, 2);
		String jobGuid = "job-guid-456";
		Long jobExecutionId = 600L;
		String partitionName = "partition-1";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, chunkMetadata, jobGuid, jobExecutionId, partitionName, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getCause(), is(sameInstance(cause)));
		assertThat(exception.getMessage(), containsString(jobGuid));
		assertThat(exception.getMessage(), containsString(String.valueOf(jobExecutionId)));
		assertThat(exception.getMessage(), containsString(partitionName));
		assertThat(exception.getMessage(), containsString("RuntimeException"));
		assertThat(exception.getMessage(), containsString("Projection algorithm failed"));
		assertThat(exception.getMessage(), containsString("recordCount=2"));
		assertThat(exception.getMessage(), containsString("startIndex=5"));
	}

	@Test
	void testHandleProjectionFailure_WithNullExceptionMessage() {
		RuntimeException cause = new RuntimeException((String) null);
		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata("partition-nomsg", "/tmp/job", 0, 1);
		String jobGuid = "job-guid-789";
		Long jobExecutionId = 700L;
		String partitionName = "partition-nomsg";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, chunkMetadata, jobGuid, jobExecutionId, partitionName, logger);

		assertThat(exception, is(notNullValue()));
		assertThat(exception.getMessage(), containsString("No error message"));
	}

	@Test
	void testHandleProjectionFailure_IsSkippableAndNotRetryable() {
		RuntimeException cause = new RuntimeException("Test");
		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata("partition-test", "/tmp/job", 0, 5);

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, chunkMetadata, "guid", 1L, "partition-test", logger);

		assertTrue(exception.isSkippable(), "Projection exceptions should be skippable");
		assertFalse(exception.isRetryable(), "Projection exceptions should not be retryable");
	}
}
