package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;

@ExtendWith(MockitoExtension.class)
class BatchMetricsCollectorTest {

	@InjectMocks
	private BatchMetricsCollector batchMetricsCollector;

	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
	private static final String PARTITION_NAME = "partition-1";
	private static final String EXIT_CODE = "COMPLETED";

	@BeforeEach
	void setUp() throws BatchMetricsException {
		// Clean up any existing metrics from previous tests
		batchMetricsCollector.cleanupOldMetrics(0);
	}

	@Test
	void testInitializeMetrics() throws BatchMetricsException {
		BatchMetrics metrics = batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		assertNotNull(metrics);
		assertEquals(JOB_EXECUTION_ID, metrics.getJobExecutionId());
		assertEquals("STARTING", metrics.getStatus());
		assertNotNull(metrics.getStartTime());

		BatchMetrics retrievedMetrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertSame(metrics, retrievedMetrics);
	}

	@Test
	void testInitializeMetrics_DuplicateJobGuid_ThrowsException() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class,
				() -> batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID + 1, JOB_GUID)
		);

		assertTrue(exception.getMessage().contains("Job metrics already exists"));
	}

	@Test
	void testInitializePartitionMetrics() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertNotNull(metrics);

		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);
		assertNotNull(partitionMetrics);
		assertEquals(PARTITION_NAME, partitionMetrics.getPartitionName());
		assertNotNull(partitionMetrics.getStartTime());
	}

	@Test
	void testCompletePartitionMetrics() throws BatchMetricsException {
		long writeCount = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME);
		batchMetricsCollector
				.completePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);

		assertNotNull(partitionMetrics.getEndTime());
		assertEquals(writeCount, partitionMetrics.getRecordsWritten());
		assertEquals(EXIT_CODE, partitionMetrics.getExitCode());
	}

	@Test
	void testFinalizeJobMetrics() throws BatchMetricsException {
		long totalRead = 100L;
		long totalWritten = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, JOB_GUID, "COMPLETED", totalRead, totalWritten);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertNotNull(metrics.getEndTime());
		assertEquals("COMPLETED", metrics.getStatus());
		assertEquals(totalRead, metrics.getTotalRecordsRead());
		assertEquals(totalWritten, metrics.getTotalRecordsWritten());
		assertEquals(totalWritten, metrics.getTotalRecordsProcessed());
	}

	@Test
	void testRecordRetryAttempt() throws BatchMetricsException {
		int attemptNumber = 2;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, attemptNumber, error, true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(1, metrics.getSuccessfulRetries());
		assertEquals(0, metrics.getFailedRetries());

		assertEquals(1, metrics.getRetryDetails().size());
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().peek();
		assertNotNull(retryDetail);
		assertEquals(attemptNumber, retryDetail.attemptNumber());
		assertEquals("RuntimeException", retryDetail.errorType());
		assertEquals("Test error", retryDetail.errorMessage());
		assertTrue(retryDetail.successful());
	}

	@Test
	void testRecordSkip() throws BatchMetricsException {
		Throwable error = new IllegalStateException("Invalid state");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432109", error, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(1, metrics.getTotalSkips());
		assertEquals(1, metrics.getSkipReasonCount().get("IllegalStateException"));

		assertEquals(1, metrics.getSkipDetails().size());
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().peek();
		assertNotNull(skipDetail);
		assertEquals("98765432109", skipDetail.featureId());
		assertEquals("IllegalStateException", skipDetail.errorType());
		assertEquals("Invalid state", skipDetail.errorMessage());
	}

	@Test
	void testCleanupOldMetrics() throws BatchMetricsException {
		Long jobId1 = 1L;
		Long jobId2 = 2L;
		Long jobId3 = 3L;
		Long jobId4 = 4L;
		String guid1 = "guid-1";
		String guid2 = "guid-2";
		String guid3 = "guid-3";
		String guid4 = "guid-4";

		batchMetricsCollector.initializeMetrics(jobId1, guid1);
		batchMetricsCollector.initializeMetrics(jobId2, guid2);
		batchMetricsCollector.initializeMetrics(jobId3, guid3);
		batchMetricsCollector.initializeMetrics(jobId4, guid4);

		// Verify all metrics are initialized
		assertNotNull(batchMetricsCollector.getJobMetrics(guid1));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid2));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid3));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid4));

		batchMetricsCollector.cleanupOldMetrics(2);

		// After cleanup, only the 2 most recent (by arrival time) should remain
		assertFalse(batchMetricsCollector.isJobMetricsPresent(guid1));
		assertFalse(batchMetricsCollector.isJobMetricsPresent(guid2));
		assertTrue(batchMetricsCollector.isJobMetricsPresent(guid3));
		assertTrue(batchMetricsCollector.isJobMetricsPresent(guid4));
	}

	@Test
	void testGetJobMetrics_NotFound() {
		assertFalse(batchMetricsCollector.isJobMetricsPresent("non-existent-guid"));
	}

	@Test
	void testMultiplePartitions() throws BatchMetricsException {
		String partition2 = "partition-2";

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, partition2);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(2, metrics.getPartitionMetrics().size());
	}

	@Test
	void testInitializePartitionMetrics_NoMetricsFound() {
		String nonExistentGuid = "non-existent-guid-for-partition";
		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class,
				() -> batchMetricsCollector
						.initializePartitionMetrics(JOB_EXECUTION_ID, nonExistentGuid, PARTITION_NAME)
		);

		assertTrue(exception.getMessage().contains("No metrics found"));
	}

	@Test
	void testCompletePartitionMetrics_NoMetricsFound() {
		String nonExistentGuid = "non-existent-guid-for-complete";
		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class,
				() -> batchMetricsCollector
						.completePartitionMetrics(JOB_EXECUTION_ID, nonExistentGuid, PARTITION_NAME, 100L, EXIT_CODE)
		);

		assertTrue(exception.getMessage().contains("No metrics found"));
	}

	@Test
	void testCompletePartitionMetrics_NoPartitionMetricsFound() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class,
				() -> batchMetricsCollector
						.completePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, "unknown-partition", 100L, EXIT_CODE)
		);

		assertTrue(exception.getMessage().contains("Partition metrics not found for partition"));
	}

	@Test
	void testFinalizeJobMetrics_NoMetricsFound() {
		String nonExistentGuid = "non-existent-guid-for-finalize";
		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class,
				() -> batchMetricsCollector
						.finalizeJobMetrics(JOB_EXECUTION_ID, nonExistentGuid, "COMPLETED", 100L, 95L)
		);

		assertTrue(exception.getMessage().contains("No metrics found"));
	}

	@Test
	void testRecordRetryAttempt_NullError() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> batchMetricsCollector
						.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, 1, null, false, PARTITION_NAME)
		);

		assertNotNull(exception);
	}

	@Test
	void testRecordSkip_NullError() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432109", null, PARTITION_NAME)
		);

		assertNotNull(exception);
	}

	@Test
	void testCleanupOldMetrics_NegativeKeepCount() {
		BatchMetricsException exception = assertThrows(
				BatchMetricsException.class, () -> batchMetricsCollector.cleanupOldMetrics(-1)
		);

		assertTrue(exception.getMessage().contains("Keep count must be non-negative"));
	}

	@Test
	void testCleanupOldMetrics_ZeroKeepCount() throws BatchMetricsException {
		String guid1 = "cleanup-zero-guid-1";
		String guid2 = "cleanup-zero-guid-2";
		batchMetricsCollector.initializeMetrics(1L, guid1);
		batchMetricsCollector.initializeMetrics(2L, guid2);

		batchMetricsCollector.cleanupOldMetrics(0);

		// All metrics should be removed
		assertFalse(batchMetricsCollector.isJobMetricsPresent(guid1));
		assertFalse(batchMetricsCollector.isJobMetricsPresent(guid2));
	}

	@Test
	void testCleanupOldMetrics_KeepCountGreaterThanSize() throws BatchMetricsException {
		String guid1 = "cleanup-keep-guid-1";
		String guid2 = "cleanup-keep-guid-2";
		batchMetricsCollector.initializeMetrics(1L, guid1);
		batchMetricsCollector.initializeMetrics(2L, guid2);

		batchMetricsCollector.cleanupOldMetrics(10);

		// All metrics should remain
		assertTrue(batchMetricsCollector.isJobMetricsPresent(guid1));
		assertTrue(batchMetricsCollector.isJobMetricsPresent(guid2));
	}

	@Test
	void testRecordRetryAttempt_Failed() throws BatchMetricsException {
		int attemptNumber = 3;
		Throwable error = new IllegalArgumentException("Invalid argument");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, attemptNumber, error, false, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(0, metrics.getSuccessfulRetries());
		assertEquals(1, metrics.getFailedRetries());

		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().peek();
		assertNotNull(retryDetail);
		assertEquals("IllegalArgumentException", retryDetail.errorType());
		assertFalse(retryDetail.successful());
	}

	@Test
	void testRecordSkip_MultipleSkipsWithDifferentErrors() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		batchMetricsCollector
				.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432101", new RuntimeException("Error 1"), PARTITION_NAME);

		batchMetricsCollector.recordSkip(
				JOB_EXECUTION_ID, JOB_GUID, "98765432102", new IllegalStateException("Error 2"), PARTITION_NAME
		);

		batchMetricsCollector
				.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432103", new RuntimeException("Error 3"), PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(3, metrics.getTotalSkips());
		assertEquals(2, metrics.getSkipReasonCount().get("RuntimeException"));
		assertEquals(1, metrics.getSkipReasonCount().get("IllegalStateException"));
	}

	@Test
	void testCleanupOldMetrics_WithLargeNumberOfEntries() throws BatchMetricsException {
		for (long i = 1; i <= 100; i++) {
			batchMetricsCollector.initializeMetrics(i, "guid-" + i);
		}

		batchMetricsCollector.cleanupOldMetrics(10);

		assertFalse(batchMetricsCollector.isJobMetricsPresent("guid-1"));
		assertNotNull(batchMetricsCollector.getJobMetrics("guid-100"));
	}

	@Test
	void testCleanupOldMetrics_WithEmptyMap() throws BatchMetricsException {
		batchMetricsCollector.cleanupOldMetrics(5);

		// Verify that cleanup on empty map doesn't cause any issues
		assertFalse(batchMetricsCollector.isJobMetricsPresent("any-guid"));
	}

	@Test
	void testCleanupOldMetrics_WithExactKeepCount() throws BatchMetricsException {
		String guid1 = "exact-keep-guid-1";
		String guid2 = "exact-keep-guid-2";
		String guid3 = "exact-keep-guid-3";
		batchMetricsCollector.initializeMetrics(1L, guid1);
		batchMetricsCollector.initializeMetrics(2L, guid2);
		batchMetricsCollector.initializeMetrics(3L, guid3);

		batchMetricsCollector.cleanupOldMetrics(3);

		assertNotNull(batchMetricsCollector.getJobMetrics(guid1));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid2));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid3));
	}

	@Test
	void testFinalizeJobMetrics_WithDifferentStatuses() throws BatchMetricsException {
		Long jobId1 = 101L;
		Long jobId2 = 102L;
		Long jobId3 = 103L;
		String guid1 = "status-test-guid-101";
		String guid2 = "status-test-guid-102";
		String guid3 = "status-test-guid-103";

		batchMetricsCollector.initializeMetrics(jobId1, guid1);
		batchMetricsCollector.finalizeJobMetrics(jobId1, guid1, "COMPLETED", 100L, 100L);

		batchMetricsCollector.initializeMetrics(jobId2, guid2);
		batchMetricsCollector.finalizeJobMetrics(jobId2, guid2, "FAILED", 100L, 50L);

		batchMetricsCollector.initializeMetrics(jobId3, guid3);
		batchMetricsCollector.finalizeJobMetrics(jobId3, guid3, "STOPPED", 50L, 0L);

		assertEquals("COMPLETED", batchMetricsCollector.getJobMetrics(guid1).getStatus());
		assertEquals("FAILED", batchMetricsCollector.getJobMetrics(guid2).getStatus());
		assertEquals("STOPPED", batchMetricsCollector.getJobMetrics(guid3).getStatus());
	}

	@Test
	void testRecordRetryAttempt_MultipleRetries() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		for (int i = 1; i <= 5; i++) {
			boolean successful = (i % 2 == 0);
			Throwable error = new RuntimeException("Retry attempt " + i);
			batchMetricsCollector.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, i, error, successful, PARTITION_NAME);
		}

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(5, metrics.getTotalRetryAttempts());
		assertEquals(2, metrics.getSuccessfulRetries());
		assertEquals(3, metrics.getFailedRetries());
		assertEquals(5, metrics.getRetryDetails().size());
	}

	@Test
	void testRecordSkip_MultipleSkipsFromSamePartition() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		for (int i = 1; i <= 10; i++) {
			Throwable error = new RuntimeException("Skip error " + i);
			batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "123456789", error, PARTITION_NAME);
		}

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(10, metrics.getTotalSkips());
		assertEquals(10, metrics.getSkipReasonCount().get("RuntimeException"));
		assertEquals(10, metrics.getSkipDetails().size());
	}

	@Test
	void testMultiplePartitions_WithDifferentMetrics() throws BatchMetricsException {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		for (int i = 0; i < 5; i++) {
			String partitionName = "partition-" + i;
			batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, partitionName);
			batchMetricsCollector
					.completePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, partitionName, (long) (i * 100), "COMPLETED");
		}

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertEquals(5, metrics.getPartitionMetrics().size());

		for (int i = 0; i < 5; i++) {
			String partitionName = "partition-" + i;
			BatchMetrics.PartitionMetrics pm = metrics.getPartitionMetrics().get(partitionName);
			assertNotNull(pm);
			assertEquals(i * 100, pm.getRecordsWritten());
			assertEquals("COMPLETED", pm.getExitCode());
		}
	}

}
