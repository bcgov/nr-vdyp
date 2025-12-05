package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

class BatchProjectionExceptionTest {

	private static final Logger logger = LoggerFactory.getLogger(BatchProjectionExceptionTest.class);

	@Test
	void testHandleProjectionFailure_WithNullPointerException() {
		NullPointerException cause = new NullPointerException("Null polygon object");
		List<BatchRecord> batchRecords = createTestBatchRecords(3);
		String jobGuid = "job-guid-123";
		Long jobExecutionId = 500L;
		String partitionName = "partition-npe";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(jobGuid));
		assertTrue(exception.getMessage().contains(String.valueOf(jobExecutionId)));
		assertTrue(exception.getMessage().contains(partitionName));
		assertTrue(exception.getMessage().contains("NullPointerException"));
		assertTrue(exception.getMessage().contains("3 records"));
		assertEquals("FEATURE-0", exception.getFeatureId(), "Should use first feature ID as record ID");
	}

	@Test
	void testHandleProjectionFailure_WithRuntimeException() {
		RuntimeException cause = new RuntimeException("Projection algorithm failed");
		List<BatchRecord> batchRecords = createTestBatchRecords(2);
		String jobGuid = "job-guid-456";
		Long jobExecutionId = 600L;
		String partitionName = "partition-runtime";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains("RuntimeException"));
		assertTrue(exception.getMessage().contains("Projection algorithm failed"));
		assertTrue(exception.getMessage().contains("2 records"));
	}

	@Test
	void testHandleProjectionFailure_WithExceptionWithoutMessage() {
		RuntimeException cause = new RuntimeException((String) null);
		List<BatchRecord> batchRecords = createTestBatchRecords(1);
		String jobGuid = "job-guid-789";
		Long jobExecutionId = 700L;
		String partitionName = "partition-nomsg";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("No error message"));
		assertEquals("FEATURE-0", exception.getFeatureId());
	}

	@Test
	void testHandleProjectionFailure_WithMoreThanFiveFeatureIds() {
		RuntimeException cause = new RuntimeException("Test error");
		List<BatchRecord> batchRecords = createTestBatchRecords(10);
		String jobGuid = "job-guid-multi";
		Long jobExecutionId = 800L;
		String partitionName = "partition-many";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("and 5 more"), "Should truncate feature IDs preview");
		assertTrue(exception.getMessage().contains("10 records"));
		assertEquals("FEATURE-0", exception.getFeatureId(), "Should use first feature ID");
	}

	@Test
	void testHandleProjectionFailure_WithExactlyFiveFeatureIds() {
		RuntimeException cause = new RuntimeException("Test error");
		List<BatchRecord> batchRecords = createTestBatchRecords(5);
		String jobGuid = "job-guid-five";
		Long jobExecutionId = 900L;
		String partitionName = "partition-five";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertFalse(exception.getMessage().contains("and"), "Should not truncate with exactly 5 feature IDs");
		assertTrue(exception.getMessage().contains("5 records"));
	}

	@Test
	void testHandleProjectionFailure_WithEmptyBatchRecords() {
		RuntimeException cause = new RuntimeException("Empty batch");
		List<BatchRecord> batchRecords = Collections.emptyList();
		String jobGuid = "job-guid-empty";
		Long jobExecutionId = 1000L;
		String partitionName = "partition-empty";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("0 records"));
		assertNull(exception.getFeatureId(), "Should have null record ID when batch is empty");
	}

	@Test
	void testHandleProjectionFailure_FeatureIdsExtraction() {
		RuntimeException cause = new RuntimeException("Test");
		List<BatchRecord> batchRecords = Arrays
				.asList(createBatchRecord("FEATURE-A"), createBatchRecord("FEATURE-B"), createBatchRecord("FEATURE-C"));
		String jobGuid = "guid-ids";
		Long jobExecutionId = 1200L;
		String partitionName = "partition-ids";

		BatchProjectionException exception = BatchProjectionException
				.handleProjectionFailure(cause, batchRecords, jobGuid, jobExecutionId, partitionName, logger);

		String message = exception.getMessage();
		assertTrue(message.contains("FEATURE-A"));
		assertTrue(message.contains("FEATURE-B"));
		assertTrue(message.contains("FEATURE-C"));
		assertEquals("FEATURE-A", exception.getFeatureId());
	}

	private List<BatchRecord> createTestBatchRecords(int count) {
		return java.util.stream.IntStream.range(0, count).mapToObj(i -> createBatchRecord("FEATURE-" + i)).toList();
	}

	private BatchRecord createBatchRecord(String featureId) {
		return new BatchRecord(featureId, "polygonData", Collections.emptyList(), "partition-1");
	}
}
