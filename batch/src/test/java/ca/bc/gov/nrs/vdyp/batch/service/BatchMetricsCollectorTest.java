package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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
		// Clear any existing metrics for clean test state
		BatchMetrics existingMetrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		if (existingMetrics != null) {
			// Reset test data for clean state
		}
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

		// Don't initialize job metrics first
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, startLine, endLine);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
	}

	@Test
	void testCompletePartitionMetrics() {
		long writeCount = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, 1L, 100L);
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);
		
		assertNotNull(partitionMetrics.getEndTime());
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
		BatchRecord batchRecord = createTestBatchRecord("12345678901", "082G055");
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
		assertEquals(recordId, retryDetail.recordId());
		assertEquals("RuntimeException", retryDetail.errorType());
		assertEquals("Test error", retryDetail.errorMessage());
		assertTrue(retryDetail.successful());
	}

	@Test
	void testRecordRetryAttempt_Failed() {
		Long recordId = 123L;
		BatchRecord batchRecord = createTestBatchRecord("12345678901", "082G055");
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
		assertEquals("IllegalArgumentException", retryDetail.errorType());
		assertFalse(retryDetail.successful());
	}

	@Test
	void testRecordRetryAttempt_NullError() {
		Long recordId = 123L;
		BatchRecord batchRecord = createTestBatchRecord("12345678901", "082G055");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, recordId, batchRecord, 1, null, true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("Unknown", retryDetail.errorType());
		assertEquals("No error message", retryDetail.errorMessage());
	}

	@Test
	void testRecordSkip() {
		Long recordId = 456L;
		BatchRecord batchRecord = createTestBatchRecord("98765432109", "082G055");
		Throwable error = new IllegalStateException("Invalid state");
		Long lineNumber = 15L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, error, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalSkips());
		assertEquals(1, metrics.getSkipReasonCount().get("IllegalStateException"));

		assertEquals(1, metrics.getSkipDetails().size());
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals(recordId, skipDetail.recordId());
		assertEquals("IllegalStateException", skipDetail.errorType());
		assertEquals("Invalid state", skipDetail.errorMessage());
		assertEquals(lineNumber, skipDetail.lineNumber());
	}

	@Test
	void testRecordSkip_NullError() {
		Long recordId = 456L;
		BatchRecord batchRecord = createTestBatchRecord("98765432109", "082G055");
		Long lineNumber = 15L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, null, PARTITION_NAME, lineNumber);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals("Unknown", skipDetail.errorType());
		assertEquals("No error message", skipDetail.errorMessage());
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

		// Verify all metrics are initialized
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId1));
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId2));
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId3));
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId4));

		batchMetricsCollector.cleanupOldMetrics(2);

		// After cleanup, only the 2 most recent (highest ID) should remain
		assertNull(batchMetricsCollector.getJobMetrics(jobId1));
		assertNull(batchMetricsCollector.getJobMetrics(jobId2));
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId3));
		assertNotNull(batchMetricsCollector.getJobMetrics(jobId4));
	}

	@Test
	void testGetJobMetrics_NotFound() {
		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(999L);
		assertNull(metrics);
	}

	/**
	 * Helper method to create a valid BatchRecord for testing.
	 */
	private BatchRecord createTestBatchRecord(String featureId, String mapId) {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(featureId);
		batchRecord.setRawPolygonData(featureId + "," + mapId + ",1234,DCR");
		batchRecord.setRawLayerData(java.util.List.of(featureId + "," + mapId + ",1234,P"));
		batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT");
		batchRecord.setLayerHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE");

		return batchRecord;
	}
}