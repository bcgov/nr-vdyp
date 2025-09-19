package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.Polygon;
import ca.bc.gov.nrs.vdyp.batch.model.Layer;
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
		assertEquals(batchRecord.toString(), retryDetail.recordData());
		assertEquals(attemptNumber, retryDetail.attemptNumber());
		assertEquals("RuntimeException", retryDetail.errorType());
		assertEquals("Test error", retryDetail.errorMessage());
		assertTrue(retryDetail.successful());
		assertEquals(PARTITION_NAME, retryDetail.partitionName());
		assertNotNull(retryDetail.timestamp());
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
		assertEquals("Invalid argument", retryDetail.errorMessage());
		assertFalse(retryDetail.successful());
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
		assertEquals("null", retryDetail.recordData());
	}

	@Test
	void testRecordRetryAttempt_NullError() {
		Long recordId = 123L;
		BatchRecord batchRecord = createTestBatchRecord("12345678901", "082G055");
		int attemptNumber = 1;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector
				.recordRetryAttempt(JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, null, true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("Unknown", retryDetail.errorType());
		assertEquals("No error message", retryDetail.errorMessage());
	}

	@Test
	void testRecordRetryAttempt_NoJobMetrics() {
		Long recordId = 123L;
		BatchRecord batchRecord = createTestBatchRecord("12345678901", "082G055");
		int attemptNumber = 1;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, true, PARTITION_NAME
		);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNull(metrics);
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
		assertEquals("null", skipDetail.recordData());
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

	/**
	 * Helper method to create a valid BatchRecord for testing with new structure.
	 */
	private BatchRecord createTestBatchRecord(String featureId, String mapId) {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(featureId);

		// Create valid polygon data
		Polygon polygon = new Polygon();
		polygon.setFeatureId(featureId);
		polygon.setMapId(mapId);
		polygon.setPolygonNumber(1234L);
		polygon.setOrgUnit("DCR");
		batchRecord.setPolygon(polygon);

		// Create valid layer data
		Layer layer = new Layer();
		layer.setFeatureId(featureId);
		layer.setMapId(mapId);
		layer.setPolygonNumber(1234L);
		layer.setLayerLevelCode("P");
		batchRecord.setLayers(java.util.List.of(layer));

		return batchRecord;
	}
}