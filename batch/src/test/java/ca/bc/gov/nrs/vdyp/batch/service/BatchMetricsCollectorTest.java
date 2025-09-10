package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BatchMetricsCollectorTest {

	@InjectMocks
	private BatchMetricsCollector batchMetricsCollector;

	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String PARTITION_NAME = "partition-1";
	private static final String STATUS = "COMPLETED";
	private static final String EXIT_CODE = "COMPLETED";

	@BeforeEach
	void setUp() {
		batchMetricsCollector.clearAllMetrics();
	}

	@Test
	void testInitializeMetrics() {
		BatchMetrics metrics = batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		assertNotNull(metrics);
		assertEquals(JOB_EXECUTION_ID, metrics.getJobExecutionId());
		assertEquals("STARTING", metrics.getStatus());
		assertNotNull(metrics.getStartTime());

		BatchMetrics retrievedMetrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertSame(metrics, retrievedMetrics);
	}

	@Test
	void testInitializeJobMetrics() {
		batchMetricsCollector.initializeJobMetrics(JOB_EXECUTION_ID);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics);
		assertEquals(JOB_EXECUTION_ID, metrics.getJobExecutionId());
		assertEquals("STARTING", metrics.getStatus());
	}

	@Test
	void testInitializePartitionMetrics() {
		long startLine = 1L;
		long endLine = 100L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, startLine, endLine);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics);

		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);
		assertNotNull(partitionMetrics);
		assertEquals(PARTITION_NAME, partitionMetrics.getPartitionName());
		assertNotNull(partitionMetrics.getStartTime());
	}

	@Test
	void testInitializePartitionMetrics_NoJobMetrics() {
		long startLine = 1L;
		long endLine = 100L;

		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, startLine, endLine);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testCompletePartitionMetrics() {
		long writeCount = 95L;
		LocalDateTime beforeCompletion = LocalDateTime.now();

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, 1L, 100L);
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);

		assertNotNull(partitionMetrics.getEndTime());
		assertTrue(
				partitionMetrics.getEndTime().isAfter(beforeCompletion)
						|| partitionMetrics.getEndTime().isEqual(beforeCompletion)
		);
		assertEquals((int) writeCount, partitionMetrics.getRecordsWritten());
		assertEquals(EXIT_CODE, partitionMetrics.getExitCode());
	}

	@Test
	void testCompletePartitionMetrics_NoJobMetrics() {
		long writeCount = 95L;

		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testCompletePartitionMetrics_NoPartitionMetrics() {
		long writeCount = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics);
		assertNull(metrics.getPartitionMetrics().get(PARTITION_NAME));
	}

	@Test
	void testFinalizeJobMetrics() {
		long totalRead = 100L;
		long totalWritten = 95L;
		LocalDateTime beforeFinalization = LocalDateTime.now();

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, STATUS, totalRead, totalWritten);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics.getEndTime());
		assertTrue(
				metrics.getEndTime().isAfter(beforeFinalization) || metrics.getEndTime().isEqual(beforeFinalization)
		);
		assertEquals(STATUS, metrics.getStatus());
		assertEquals(totalRead, metrics.getTotalRecordsRead());
		assertEquals(totalWritten, metrics.getTotalRecordsWritten());
		assertEquals(totalWritten, metrics.getTotalRecordsProcessed());
	}

	@Test
	void testFinalizeJobMetrics_NoJobMetrics() {
		long totalRead = 100L;
		long totalWritten = 95L;

		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, STATUS, totalRead, totalWritten);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testRecordRetryAttempt_Successful() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord(recordId, "test-data");
		int attemptNumber = 2;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, true, PARTITION_NAME
		);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(1, metrics.getSuccessfulRetries());
		assertEquals(0, metrics.getFailedRetries());

		assertEquals(1, metrics.getRetryDetails().size());
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals(recordId, retryDetail.getRecordId());
		assertEquals(batchRecord.toString(), retryDetail.getRecordData());
		assertEquals(attemptNumber, retryDetail.getAttemptNumber());
		assertEquals("RuntimeException", retryDetail.getErrorType());
		assertEquals("Test error", retryDetail.getErrorMessage());
		assertTrue(retryDetail.isSuccessful());
		assertEquals(PARTITION_NAME, retryDetail.getPartitionName());
		assertNotNull(retryDetail.getTimestamp());
	}

	@Test
	void testRecordRetryAttempt_Failed() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord(recordId, "test-data");
		int attemptNumber = 3;
		Throwable error = new IllegalArgumentException("Invalid argument");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, false, PARTITION_NAME
		);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(0, metrics.getSuccessfulRetries());
		assertEquals(1, metrics.getFailedRetries());

		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("IllegalArgumentException", retryDetail.getErrorType());
		assertEquals("Invalid argument", retryDetail.getErrorMessage());
		assertFalse(retryDetail.isSuccessful());
	}

	@Test
	void testRecordRetryAttempt_NullBatchRecord() {
		Long recordId = 123L;
		int attemptNumber = 1;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, recordId, null, attemptNumber, error, true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("null", retryDetail.getRecordData());
	}

	@Test
	void testRecordRetryAttempt_NullError() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord(recordId, "test-data");
		int attemptNumber = 1;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, null, true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("Unknown", retryDetail.getErrorType());
		assertEquals("No error message", retryDetail.getErrorMessage());
	}

	@Test
	void testRecordRetryAttempt_NoJobMetrics() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord(recordId, "test-data");
		int attemptNumber = 1;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, true, PARTITION_NAME
		);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testRecordSkip() {
		Long recordId = 456L;
		BatchRecord batchRecord = new BatchRecord(recordId, "skip-data");
		Throwable error = new IllegalStateException("Invalid state");
		Long lineNumber = 15L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, error, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalSkips());
		assertEquals(Integer.valueOf(1), metrics.getSkipReasonCount().get("IllegalStateException"));

		assertEquals(1, metrics.getSkipDetails().size());
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals(recordId, skipDetail.getRecordId());
		assertEquals(batchRecord.toString(), skipDetail.getRecordData());
		assertEquals("IllegalStateException", skipDetail.getErrorType());
		assertEquals("Invalid state", skipDetail.getErrorMessage());
		assertEquals(PARTITION_NAME, skipDetail.getPartitionName());
		assertEquals(lineNumber, skipDetail.getLineNumber());
		assertNotNull(skipDetail.getTimestamp());
	}

	@Test
	void testRecordSkip_MultipleSkipsWithSameReason() {
		Long recordId1 = 456L;
		Long recordId2 = 457L;
		BatchRecord batchRecord1 = new BatchRecord(recordId1, "skip-data-1");
		BatchRecord batchRecord2 = new BatchRecord(recordId2, "skip-data-2");
		Throwable error = new IllegalStateException("Invalid state");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId1, batchRecord1, error, PARTITION_NAME, 15L);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId2, batchRecord2, error, PARTITION_NAME, 16L);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(2, metrics.getTotalSkips());
		assertEquals(Integer.valueOf(2), metrics.getSkipReasonCount().get("IllegalStateException"));
		assertEquals(2, metrics.getSkipDetails().size());
	}

	@Test
	void testRecordSkip_NullBatchRecord() {
		Long recordId = 456L;
		Throwable error = new IllegalStateException("Invalid state");
		Long lineNumber = 15L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, null, error, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals("null", skipDetail.getRecordData());
	}

	@Test
	void testRecordSkip_NullError() {
		Long recordId = 456L;
		BatchRecord batchRecord = new BatchRecord(recordId, "skip-data");
		Long lineNumber = 15L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, null, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals("Unknown", skipDetail.getErrorType());
		assertEquals("No error message", skipDetail.getErrorMessage());
		assertEquals(Integer.valueOf(1), metrics.getSkipReasonCount().get("Unknown"));
	}

	@Test
	void testRecordSkip_NoJobMetrics() {
		Long recordId = 456L;
		BatchRecord batchRecord = new BatchRecord(recordId, "skip-data");
		Throwable error = new IllegalStateException("Invalid state");
		Long lineNumber = 15L;

		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, error, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testCleanupOldMetrics() {
		Long jobId1 = 1L;
		Long jobId2 = 2L;
		Long jobId3 = 3L;
		Long jobId4 = 4L;

		batchMetricsCollector.initializeMetrics(jobId1);
		batchMetricsCollector.initializeMetrics(jobId2);
		batchMetricsCollector.initializeMetrics(jobId3);
		batchMetricsCollector.initializeMetrics(jobId4);

		assertEquals(4, batchMetricsCollector.getAllJobMetrics().size());

		batchMetricsCollector.cleanupOldMetrics(2);

		Map<Long, BatchMetrics> remainingMetrics = batchMetricsCollector.getAllJobMetrics();
		assertEquals(2, remainingMetrics.size());
		assertTrue(remainingMetrics.containsKey(jobId3));
		assertTrue(remainingMetrics.containsKey(jobId4));
	}

	@Test
	void testCleanupOldMetrics_NoCleanupNeeded() {
		Long jobId1 = 1L;
		Long jobId2 = 2L;

		batchMetricsCollector.initializeMetrics(jobId1);
		batchMetricsCollector.initializeMetrics(jobId2);

		assertEquals(2, batchMetricsCollector.getAllJobMetrics().size());

		batchMetricsCollector.cleanupOldMetrics(5);

		assertEquals(2, batchMetricsCollector.getAllJobMetrics().size());
	}

	@Test
	void testGetJobMetrics_NotFound() {
		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(999L);
		assertNull(metrics);
	}

	@Test
	void testGetAllJobMetrics() {
		Long jobId1 = 1L;
		Long jobId2 = 2L;

		batchMetricsCollector.initializeMetrics(jobId1);
		batchMetricsCollector.initializeMetrics(jobId2);

		Map<Long, BatchMetrics> allMetrics = batchMetricsCollector.getAllJobMetrics();
		assertEquals(2, allMetrics.size());
		assertTrue(allMetrics.containsKey(jobId1));
		assertTrue(allMetrics.containsKey(jobId2));

		// Verify it's a copy (modifications don't affect the original)
		allMetrics.clear();
		assertEquals(2, batchMetricsCollector.getAllJobMetrics().size());
	}

	@Test
	void testUpdateMetrics() {
		BatchMetrics originalMetrics = batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		BatchMetrics updatedMetrics = new BatchMetrics(JOB_EXECUTION_ID);
		updatedMetrics.setStatus("UPDATED");
		updatedMetrics.setTotalRecordsRead(200L);

		batchMetricsCollector.updateMetrics(JOB_EXECUTION_ID, updatedMetrics);

		BatchMetrics retrievedMetrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotSame(originalMetrics, retrievedMetrics);
		assertEquals(updatedMetrics, retrievedMetrics);
		assertEquals("UPDATED", retrievedMetrics.getStatus());
		assertEquals(200L, retrievedMetrics.getTotalRecordsRead());
	}

	@Test
	void testRemoveMetrics() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		assertNotNull(batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID));

		batchMetricsCollector.removeMetrics(JOB_EXECUTION_ID);
		assertNull(batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID));
	}

	@Test
	void testRemoveMetrics_NotFound() {
		batchMetricsCollector.removeMetrics(999L);
		assertNull(batchMetricsCollector.getJobMetrics(999L));
	}

	@Test
	void testClearAllMetrics() {
		Long jobId1 = 1L;
		Long jobId2 = 2L;

		batchMetricsCollector.initializeMetrics(jobId1);
		batchMetricsCollector.initializeMetrics(jobId2);
		assertEquals(2, batchMetricsCollector.getAllJobMetrics().size());

		batchMetricsCollector.clearAllMetrics();
		assertEquals(0, batchMetricsCollector.getAllJobMetrics().size());
	}

	@Test
	void testComplexWorkflow() {
		String partition1 = "partition-1";
		String partition2 = "partition-2";
		Long recordId1 = 100L;
		Long recordId2 = 200L;
		Long recordId3 = 300L;

		// Initialize job
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		// Initialize partitions
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, partition1, 1L, 50L);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, partition2, 51L, 100L);

		// Record some processing events
		BatchRecord record1 = new BatchRecord(recordId1, "data1");
		BatchRecord record2 = new BatchRecord(recordId2, "data2");
		BatchRecord record3 = new BatchRecord(recordId3, "data3");

		// Record retry and skip events
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId1, record1, 1, new RuntimeException("Retry error"), false, partition1
		);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId1, record1, 2, new RuntimeException("Retry error"), true, partition1
		);
		batchMetricsCollector.recordSkip(
				JOB_EXECUTION_ID, recordId2, record2, new IllegalArgumentException("Skip error"), partition1, 25L
		);
		batchMetricsCollector.recordSkip(
				JOB_EXECUTION_ID, recordId3, record3, new IllegalArgumentException("Skip error"), partition2, 75L
		);

		// Complete partitions
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, partition1, 48L, "COMPLETED");
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, partition2, 49L, "COMPLETED");

		// Finalize job
		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, "COMPLETED", 100L, 97L);

		// Verify final state
		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics);
		assertEquals("COMPLETED", metrics.getStatus());
		assertEquals(100L, metrics.getTotalRecordsRead());
		assertEquals(97L, metrics.getTotalRecordsWritten());
		assertEquals(2, metrics.getTotalRetryAttempts());
		assertEquals(1, metrics.getSuccessfulRetries());
		assertEquals(1, metrics.getFailedRetries());
		assertEquals(2, metrics.getTotalSkips());
		assertEquals(2, metrics.getPartitionMetrics().size());
		assertEquals(2, metrics.getRetryDetails().size());
		assertEquals(2, metrics.getSkipDetails().size());
		assertEquals(Integer.valueOf(2), metrics.getSkipReasonCount().get("IllegalArgumentException"));
	}
}