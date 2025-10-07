package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BatchMetricsCollectorTest {

	@InjectMocks
	private BatchMetricsCollector batchMetricsCollector;

	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String PARTITION_NAME = "partition-1";
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
	void testInitializePartitionMetrics() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics);

		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);
		assertNotNull(partitionMetrics);
		assertEquals(PARTITION_NAME, partitionMetrics.getPartitionName());
		assertNotNull(partitionMetrics.getStartTime());
	}

	@Test
	void testCompletePartitionMetrics() {
		long writeCount = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME);
		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME, writeCount, EXIT_CODE);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(PARTITION_NAME);

		assertNotNull(partitionMetrics.getEndTime());
		assertEquals((int) writeCount, partitionMetrics.getRecordsWritten());
		assertEquals(EXIT_CODE, partitionMetrics.getExitCode());
	}

	@Test
	void testFinalizeJobMetrics() {
		long totalRead = 100L;
		long totalWritten = 95L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, "COMPLETED", totalRead, totalWritten);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertNotNull(metrics.getEndTime());
		assertEquals("COMPLETED", metrics.getStatus());
		assertEquals(totalRead, metrics.getTotalRecordsRead());
		assertEquals(totalWritten, metrics.getTotalRecordsWritten());
		assertEquals(totalWritten, metrics.getTotalRecordsProcessed());
	}

	@Test
	void testRecordRetryAttempt() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		int attemptNumber = 2;
		Throwable error = new RuntimeException("Test error");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, true, PARTITION_NAME);

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
	void testRecordSkip() {
		Long recordId = 456L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("98765432109");
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

	@Test
	void testMultiplePartitions() {
		String partition2 = "partition-2";

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, partition2);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(2, metrics.getPartitionMetrics().size());
	}

	@Test
	void testInitializeMetrics_NullJobExecutionId() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.initializeMetrics(null));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testInitializePartitionMetrics_NullJobExecutionId() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.initializePartitionMetrics(null, PARTITION_NAME));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testInitializePartitionMetrics_NullPartitionName() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, null));

		assertTrue(exception.getMessage().contains("Partition name cannot be null or blank"));
	}

	@Test
	void testInitializePartitionMetrics_BlankPartitionName() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, "   "));

		assertTrue(exception.getMessage().contains("Partition name cannot be null or blank"));
	}

	@Test
	void testInitializePartitionMetrics_NoMetricsFound() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.initializePartitionMetrics(999L, PARTITION_NAME));

		assertTrue(exception.getMessage().contains("No metrics found for job execution ID"));
	}

	@Test
	void testCompletePartitionMetrics_NullJobExecutionId() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.completePartitionMetrics(null, PARTITION_NAME, 100L, EXIT_CODE));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testCompletePartitionMetrics_NullPartitionName() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, null, 100L, EXIT_CODE));

		assertTrue(exception.getMessage().contains("Partition name cannot be null or blank"));
	}

	@Test
	void testCompletePartitionMetrics_NoMetricsFound() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.completePartitionMetrics(999L, PARTITION_NAME, 100L, EXIT_CODE));

		assertTrue(exception.getMessage().contains("No metrics found for job execution ID"));
	}

	@Test
	void testCompletePartitionMetrics_NoPartitionMetricsFound() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, "unknown-partition", 100L,
						EXIT_CODE));

		assertTrue(exception.getMessage().contains("No partition metrics found for partition"));
	}

	@Test
	void testFinalizeJobMetrics_NullJobExecutionId() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.finalizeJobMetrics(null, "COMPLETED", 100L, 95L));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testFinalizeJobMetrics_NoMetricsFound() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.finalizeJobMetrics(999L, "COMPLETED", 100L, 95L));

		assertTrue(exception.getMessage().contains("No metrics found for job execution ID"));
	}

	@Test
	void testRecordRetryAttempt_NullError() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, 1, null, false, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(0, metrics.getSuccessfulRetries());
		assertEquals(1, metrics.getFailedRetries());

		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("Unknown", retryDetail.errorType());
		assertEquals("No error message", retryDetail.errorMessage());
	}

	@Test
	void testRecordRetryAttempt_NullBatchRecord() {
		Long recordId = 123L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, null, 1, new RuntimeException("test"), true, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalRetryAttempts());

		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("null", retryDetail.recordData());
	}

	@Test
	void testRecordSkip_NullError() {
		Long recordId = 456L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("98765432109");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, batchRecord, null, PARTITION_NAME, 15L);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalSkips());

		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals("Unknown", skipDetail.errorType());
		assertEquals("No error message", skipDetail.errorMessage());
	}

	@Test
	void testRecordSkip_NullBatchRecord() {
		Long recordId = 456L;

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, recordId, null, new RuntimeException("test"),
				PARTITION_NAME, 15L);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalSkips());

		BatchMetrics.SkipDetail skipDetail = metrics.getSkipDetails().get(0);
		assertEquals("null", skipDetail.recordData());
	}

	@Test
	void testCleanupOldMetrics_NegativeKeepCount() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.cleanupOldMetrics(-1));

		assertTrue(exception.getMessage().contains("Keep count must be non-negative"));
	}

	@Test
	void testCleanupOldMetrics_ZeroKeepCount() {
		batchMetricsCollector.initializeMetrics(1L);
		batchMetricsCollector.initializeMetrics(2L);

		batchMetricsCollector.cleanupOldMetrics(0);

		// All metrics should be removed
		assertNull(batchMetricsCollector.getJobMetrics(1L));
		assertNull(batchMetricsCollector.getJobMetrics(2L));
	}

	@Test
	void testCleanupOldMetrics_KeepCountGreaterThanSize() {
		batchMetricsCollector.initializeMetrics(1L);
		batchMetricsCollector.initializeMetrics(2L);

		batchMetricsCollector.cleanupOldMetrics(10);

		// All metrics should remain
		assertNotNull(batchMetricsCollector.getJobMetrics(1L));
		assertNotNull(batchMetricsCollector.getJobMetrics(2L));
	}

	@Test
	void testGetJobMetrics_NullJobExecutionId() {
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.getJobMetrics(null));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testRecordRetryAttempt_Failed() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		int attemptNumber = 3;
		Throwable error = new IllegalArgumentException("Invalid argument");

		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, attemptNumber, error, false, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(1, metrics.getTotalRetryAttempts());
		assertEquals(0, metrics.getSuccessfulRetries());
		assertEquals(1, metrics.getFailedRetries());

		BatchMetrics.RetryDetail retryDetail = metrics.getRetryDetails().get(0);
		assertEquals("IllegalArgumentException", retryDetail.errorType());
		assertFalse(retryDetail.successful());
	}

	@Test
	void testRecordSkip_MultipleSkipsWithDifferentErrors() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		BatchRecord record1 = new BatchRecord();
		record1.setFeatureId("111111111");
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, 1L, record1, new RuntimeException("Error 1"),
				PARTITION_NAME, 10L);

		BatchRecord record2 = new BatchRecord();
		record2.setFeatureId("222222222");
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, 2L, record2, new IllegalStateException("Error 2"),
				PARTITION_NAME, 20L);

		BatchRecord record3 = new BatchRecord();
		record3.setFeatureId("333333333");
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, 3L, record3, new RuntimeException("Error 3"),
				PARTITION_NAME, 30L);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_EXECUTION_ID);
		assertEquals(3, metrics.getTotalSkips());
		assertEquals(2, metrics.getSkipReasonCount().get("RuntimeException"));
		assertEquals(1, metrics.getSkipReasonCount().get("IllegalStateException"));
	}

	@Test
	void testBatchException_MessageConstructor() {
		String message = "Test batch error message";
		BatchException exception = new BatchException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testBatchException_MessageAndCauseConstructor() {
		String message = "Test batch error message";
		Exception cause = new RuntimeException("Original cause");
		BatchException exception = new BatchException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testBatchException_CauseConstructor() {
		Exception cause = new RuntimeException("Original cause");
		BatchException exception = new BatchException(cause);

		assertEquals(cause, exception.getCause());
		assertTrue(exception.getMessage().contains("Original cause"));
	}

	@Test
	void testBatchException_HandleException_WithContext() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		Exception cause = new RuntimeException("Original error");
		String errorDescription = "Failed to process batch";
		String context = "test-context";

		BatchException result = BatchException.handleException(context, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(context));
		assertTrue(result.getMessage().contains("RuntimeException"));
		assertTrue(result.getMessage().contains("Original error"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchException_HandleException_WithNullContext() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		Exception cause = new IllegalStateException("State error");
		String errorDescription = "Failed to process batch";

		BatchException result = BatchException.handleException(null, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertFalse(result.getMessage().contains("null"));
		assertTrue(result.getMessage().contains("IllegalStateException"));
		assertTrue(result.getMessage().contains("State error"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchException_HandleException_WithNullCauseMessage() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		Exception cause = new RuntimeException((String) null);
		String errorDescription = "Failed to process batch";
		String context = "test-context";

		BatchException result = BatchException.handleException(context, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains("No error message available"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchException_HandleProjectionFailure() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		String partitionName = "partition-5";
		int recordCount = 100;
		Exception cause = new RuntimeException("Projection failed");
		String errorDescription = "Failed to project records";

		BatchException result = BatchException.handleProjectionFailure(
				partitionName, recordCount, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains("partition=partition-5"));
		assertTrue(result.getMessage().contains("records=100"));
		assertTrue(result.getMessage().contains("Projection failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchException_HandleProjectionFailure_ZeroRecords() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		String partitionName = "partition-0";
		int recordCount = 0;
		Exception cause = new IllegalArgumentException("No records");
		String errorDescription = "Failed to project empty partition";

		BatchException result = BatchException.handleProjectionFailure(
				partitionName, recordCount, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("records=0"));
		assertTrue(result.getMessage().contains("No records"));
	}

	@Test
	void testBatchException_HandleBatchProcessingFailure() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		Long jobExecutionId = 12345L;
		Exception cause = new RuntimeException("Processing failed");
		String errorDescription = "Failed to process batch job";

		BatchException result = BatchException.handleBatchProcessingFailure(
				jobExecutionId, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains("jobExecutionId=12345"));
		assertTrue(result.getMessage().contains("Processing failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchException_HandleBatchProcessingFailure_LargeJobId() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		Long jobExecutionId = 999999999L;
		Exception cause = new IllegalStateException("Invalid state");
		String errorDescription = "Batch job failed";

		BatchException result = BatchException.handleBatchProcessingFailure(
				jobExecutionId, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("jobExecutionId=999999999"));
		assertTrue(result.getMessage().contains("Invalid state"));
	}

	@Test
	void testBatchException_DifferentExceptionTypes() {
		Logger logger = LoggerFactory.getLogger(BatchMetricsCollectorTest.class);
		String context = "test";

		// Test with different exception types
		Exception runtimeException = new RuntimeException("Runtime error");
		BatchException result1 = BatchException.handleException(context, runtimeException, "Error 1", logger);
		assertTrue(result1.getMessage().contains("RuntimeException"));

		Exception illegalArgumentException = new IllegalArgumentException("Illegal argument");
		BatchException result2 = BatchException.handleException(context, illegalArgumentException, "Error 2", logger);
		assertTrue(result2.getMessage().contains("IllegalArgumentException"));

		Exception illegalStateException = new IllegalStateException("Illegal state");
		BatchException result3 = BatchException.handleException(context, illegalStateException, "Error 3", logger);
		assertTrue(result3.getMessage().contains("IllegalStateException"));
	}
}
