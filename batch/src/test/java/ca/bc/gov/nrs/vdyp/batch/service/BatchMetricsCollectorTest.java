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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	@Test
	void testBatchMetrics_DefaultConstructor() {
		BatchMetrics metrics = new BatchMetrics();

		assertNull(metrics.getJobExecutionId());
		assertNull(metrics.getStartTime());
		assertNull(metrics.getStatus());
		assertEquals(0, metrics.getTotalRetryAttempts());
		assertEquals(0, metrics.getSuccessfulRetries());
		assertEquals(0, metrics.getFailedRetries());
		assertEquals(0, metrics.getTotalSkips());
		assertEquals(0, metrics.getTotalRecordsProcessed());
		assertEquals(0, metrics.getTotalRecordsRead());
		assertEquals(0, metrics.getTotalRecordsWritten());
		assertEquals(0.0, metrics.getAverageProcessingTime());
		assertNotNull(metrics.getRetryDetails());
		assertNotNull(metrics.getSkipDetails());
		assertNotNull(metrics.getSkipReasonCount());
		assertNotNull(metrics.getPartitionMetrics());
	}

	@Test
	void testBatchMetrics_ParameterizedConstructor() {
		Long jobId = 12345L;
		BatchMetrics metrics = new BatchMetrics(jobId);

		assertEquals(jobId, metrics.getJobExecutionId());
		assertNotNull(metrics.getStartTime());
		assertEquals("STARTING", metrics.getStatus());
	}

	@Test
	void testBatchMetrics_SettersAndGetters() {
		BatchMetrics metrics = new BatchMetrics();

		metrics.setJobExecutionId(999L);
		assertEquals(999L, metrics.getJobExecutionId());

		LocalDateTime now = LocalDateTime.now();
		metrics.setStartTime(now);
		assertEquals(now, metrics.getStartTime());

		LocalDateTime endTime = now.plusHours(1);
		metrics.setEndTime(endTime);
		assertEquals(endTime, metrics.getEndTime());

		metrics.setStatus("COMPLETED");
		assertEquals("COMPLETED", metrics.getStatus());

		metrics.setAverageProcessingTime(123.45);
		assertEquals(123.45, metrics.getAverageProcessingTime());
	}

	@Test
	void testBatchMetrics_RetryCounters() {
		BatchMetrics metrics = new BatchMetrics();

		metrics.setTotalRetryAttempts(5);
		assertEquals(5, metrics.getTotalRetryAttempts());

		int newCount = metrics.incrementRetryAttempts();
		assertEquals(6, newCount);
		assertEquals(6, metrics.getTotalRetryAttempts());

		metrics.setSuccessfulRetries(3);
		assertEquals(3, metrics.getSuccessfulRetries());

		int newSuccessful = metrics.incrementSuccessfulRetries();
		assertEquals(4, newSuccessful);
		assertEquals(4, metrics.getSuccessfulRetries());

		metrics.setFailedRetries(2);
		assertEquals(2, metrics.getFailedRetries());

		int newFailed = metrics.incrementFailedRetries();
		assertEquals(3, newFailed);
		assertEquals(3, metrics.getFailedRetries());
	}

	@Test
	void testBatchMetrics_SkipCounters() {
		BatchMetrics metrics = new BatchMetrics();

		metrics.setTotalSkips(10);
		assertEquals(10, metrics.getTotalSkips());

		int newSkips = metrics.incrementSkips();
		assertEquals(11, newSkips);
		assertEquals(11, metrics.getTotalSkips());
	}

	@Test
	void testBatchMetrics_RecordCounters() {
		BatchMetrics metrics = new BatchMetrics();

		metrics.setTotalRecordsProcessed(1000L);
		assertEquals(1000L, metrics.getTotalRecordsProcessed());

		metrics.setTotalRecordsRead(1100L);
		assertEquals(1100L, metrics.getTotalRecordsRead());

		metrics.setTotalRecordsWritten(950L);
		assertEquals(950L, metrics.getTotalRecordsWritten());
	}

	@Test
	void testBatchMetrics_RetryDetails() {
		BatchMetrics metrics = new BatchMetrics();

		List<BatchMetrics.RetryDetail> retryDetails = new ArrayList<>();
		retryDetails.add(new BatchMetrics.RetryDetail(
				1L, "data1", 1, "RuntimeException", "Error 1", true, "partition-1"));
		retryDetails.add(new BatchMetrics.RetryDetail(
				2L, "data2", 2, "IllegalStateException", "Error 2", false, "partition-2"));

		metrics.setRetryDetails(retryDetails);

		assertEquals(2, metrics.getRetryDetails().size());
		assertEquals(1L, metrics.getRetryDetails().get(0).recordId());
		assertEquals("data1", metrics.getRetryDetails().get(0).recordData());
		assertEquals(1, metrics.getRetryDetails().get(0).attemptNumber());
		assertEquals("RuntimeException", metrics.getRetryDetails().get(0).errorType());
		assertTrue(metrics.getRetryDetails().get(0).successful());
		assertEquals("partition-1", metrics.getRetryDetails().get(0).partitionName());

		metrics.setRetryDetails(null);
		assertTrue(metrics.getRetryDetails().isEmpty());
	}

	@Test
	void testBatchMetrics_SkipDetails() {
		BatchMetrics metrics = new BatchMetrics();

		List<BatchMetrics.SkipDetail> skipDetails = new ArrayList<>();
		skipDetails.add(new BatchMetrics.SkipDetail(
				1L, "skip-data1", "RuntimeException", "Skip error 1", "partition-1", 10L));
		skipDetails.add(new BatchMetrics.SkipDetail(
				2L, "skip-data2", "IllegalArgumentException", "Skip error 2", "partition-2", 20L));

		metrics.setSkipDetails(skipDetails);

		assertEquals(2, metrics.getSkipDetails().size());
		assertEquals(1L, metrics.getSkipDetails().get(0).recordId());
		assertEquals("skip-data1", metrics.getSkipDetails().get(0).recordData());
		assertEquals("RuntimeException", metrics.getSkipDetails().get(0).errorType());
		assertEquals("Skip error 1", metrics.getSkipDetails().get(0).errorMessage());
		assertEquals("partition-1", metrics.getSkipDetails().get(0).partitionName());
		assertEquals(10L, metrics.getSkipDetails().get(0).lineNumber());

		metrics.setSkipDetails(null);
		assertTrue(metrics.getSkipDetails().isEmpty());
	}

	@Test
	void testBatchMetrics_SkipReasonCount() {
		BatchMetrics metrics = new BatchMetrics();

		Map<String, Integer> skipReasonCount = new ConcurrentHashMap<>();
		skipReasonCount.put("RuntimeException", 5);
		skipReasonCount.put("IllegalStateException", 3);

		metrics.setSkipReasonCount(skipReasonCount);

		assertEquals(2, metrics.getSkipReasonCount().size());
		assertEquals(5, metrics.getSkipReasonCount().get("RuntimeException"));
		assertEquals(3, metrics.getSkipReasonCount().get("IllegalStateException"));

		metrics.setSkipReasonCount(null);
		assertTrue(metrics.getSkipReasonCount().isEmpty());
	}

	@Test
	void testBatchMetrics_PartitionMetrics() {
		BatchMetrics metrics = new BatchMetrics();

		Map<String, BatchMetrics.PartitionMetrics> partitionMetrics = new ConcurrentHashMap<>();
		BatchMetrics.PartitionMetrics partition1 = new BatchMetrics.PartitionMetrics("partition-1");
		partition1.setRecordsProcessed(100);
		partition1.setRecordsRead(110);
		partition1.setRecordsWritten(95);
		partition1.setRetryCount(5);
		partition1.setSkipCount(2);
		partition1.setExitCode("COMPLETED");

		BatchMetrics.PartitionMetrics partition2 = new BatchMetrics.PartitionMetrics("partition-2");
		partition2.setRecordsProcessed(200);
		partition2.setRecordsRead(220);
		partition2.setRecordsWritten(190);

		partitionMetrics.put("partition-1", partition1);
		partitionMetrics.put("partition-2", partition2);

		metrics.setPartitionMetrics(partitionMetrics);

		assertEquals(2, metrics.getPartitionMetrics().size());
		assertEquals(100, metrics.getPartitionMetrics().get("partition-1").getRecordsProcessed());
		assertEquals(95, metrics.getPartitionMetrics().get("partition-1").getRecordsWritten());
		assertEquals("COMPLETED", metrics.getPartitionMetrics().get("partition-1").getExitCode());

		metrics.setPartitionMetrics(null);
		assertTrue(metrics.getPartitionMetrics().isEmpty());
	}

	@Test
	void testBatchMetrics_PartitionMetrics_AllGettersAndSetters() {
		BatchMetrics.PartitionMetrics partitionMetrics = new BatchMetrics.PartitionMetrics("test-partition");

		assertEquals("test-partition", partitionMetrics.getPartitionName());
		assertNotNull(partitionMetrics.getStartTime());
		assertNull(partitionMetrics.getEndTime());
		assertEquals(0, partitionMetrics.getRecordsProcessed());
		assertEquals(0, partitionMetrics.getRecordsRead());
		assertEquals(0, partitionMetrics.getRecordsWritten());
		assertEquals(0, partitionMetrics.getRetryCount());
		assertEquals(0, partitionMetrics.getSkipCount());
		assertNull(partitionMetrics.getExitCode());

		partitionMetrics.setPartitionName("updated-partition");
		assertEquals("updated-partition", partitionMetrics.getPartitionName());

		partitionMetrics.setRecordsProcessed(500);
		assertEquals(500, partitionMetrics.getRecordsProcessed());

		partitionMetrics.setRecordsRead(550);
		assertEquals(550, partitionMetrics.getRecordsRead());

		partitionMetrics.setRecordsWritten(480);
		assertEquals(480, partitionMetrics.getRecordsWritten());

		partitionMetrics.setRetryCount(10);
		assertEquals(10, partitionMetrics.getRetryCount());

		partitionMetrics.setSkipCount(5);
		assertEquals(5, partitionMetrics.getSkipCount());

		LocalDateTime now = LocalDateTime.now();
		partitionMetrics.setStartTime(now);
		assertEquals(now, partitionMetrics.getStartTime());

		LocalDateTime endTime = now.plusHours(2);
		partitionMetrics.setEndTime(endTime);
		assertEquals(endTime, partitionMetrics.getEndTime());

		partitionMetrics.setExitCode("FAILED");
		assertEquals("FAILED", partitionMetrics.getExitCode());
	}

	@Test
	void testBatchMetrics_RetryDetail_WithTimestamp() {
		LocalDateTime timestamp = LocalDateTime.now();
		BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
				123L, "test-data", 2, "TestException", "Test error message",
				timestamp, true, "partition-test");

		assertEquals(123L, retryDetail.recordId());
		assertEquals("test-data", retryDetail.recordData());
		assertEquals(2, retryDetail.attemptNumber());
		assertEquals("TestException", retryDetail.errorType());
		assertEquals("Test error message", retryDetail.errorMessage());
		assertEquals(timestamp, retryDetail.timestamp());
		assertTrue(retryDetail.successful());
		assertEquals("partition-test", retryDetail.partitionName());
	}

	@Test
	void testBatchMetrics_RetryDetail_WithoutTimestamp() {
		BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
				456L, "data-456", 3, "AnotherException", "Another error",
				false, "partition-2");

		assertEquals(456L, retryDetail.recordId());
		assertEquals("data-456", retryDetail.recordData());
		assertEquals(3, retryDetail.attemptNumber());
		assertEquals("AnotherException", retryDetail.errorType());
		assertEquals("Another error", retryDetail.errorMessage());
		assertNotNull(retryDetail.timestamp());
		assertFalse(retryDetail.successful());
		assertEquals("partition-2", retryDetail.partitionName());
	}

	@Test
	void testBatchMetrics_SkipDetail_WithTimestamp() {
		LocalDateTime timestamp = LocalDateTime.now();
		BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
				789L, "skip-data", "SkipException", "Skip error message",
				timestamp, "partition-skip", 100L);

		assertEquals(789L, skipDetail.recordId());
		assertEquals("skip-data", skipDetail.recordData());
		assertEquals("SkipException", skipDetail.errorType());
		assertEquals("Skip error message", skipDetail.errorMessage());
		assertEquals(timestamp, skipDetail.timestamp());
		assertEquals("partition-skip", skipDetail.partitionName());
		assertEquals(100L, skipDetail.lineNumber());
	}

	@Test
	void testBatchMetrics_SkipDetail_WithoutTimestamp() {
		BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
				999L, "data-999", "TestSkipException", "Test skip error",
				"partition-3", 200L);

		assertEquals(999L, skipDetail.recordId());
		assertEquals("data-999", skipDetail.recordData());
		assertEquals("TestSkipException", skipDetail.errorType());
		assertEquals("Test skip error", skipDetail.errorMessage());
		assertNotNull(skipDetail.timestamp());
		assertEquals("partition-3", skipDetail.partitionName());
		assertEquals(200L, skipDetail.lineNumber());
	}

	@Test
	void testBatchMetrics_ThreadSafety_RetryCounters() throws InterruptedException {
		BatchMetrics metrics = new BatchMetrics();
		int numThreads = 10;
		int incrementsPerThread = 100;

		Thread[] threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < incrementsPerThread; j++) {
					metrics.incrementRetryAttempts();
					if (j % 2 == 0) {
						metrics.incrementSuccessfulRetries();
					} else {
						metrics.incrementFailedRetries();
					}
				}
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		assertEquals(numThreads * incrementsPerThread, metrics.getTotalRetryAttempts());
		assertEquals(numThreads * incrementsPerThread / 2, metrics.getSuccessfulRetries());
		assertEquals(numThreads * incrementsPerThread / 2, metrics.getFailedRetries());
	}

	@Test
	void testBatchMetrics_ThreadSafety_SkipCounters() throws InterruptedException {
		BatchMetrics metrics = new BatchMetrics();
		int numThreads = 10;
		int incrementsPerThread = 100;

		Thread[] threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < incrementsPerThread; j++) {
					metrics.incrementSkips();
				}
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		assertEquals(numThreads * incrementsPerThread, metrics.getTotalSkips());
	}

	@Test
	void testBatchMetrics_ConcurrentPartitionMetricsAccess() throws InterruptedException {
		BatchMetrics metrics = new BatchMetrics();
		int numThreads = 5;

		Thread[] threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			final int threadIndex = i;
			threads[i] = new Thread(() -> {
				String partitionName = "partition-" + threadIndex;
				BatchMetrics.PartitionMetrics pm = new BatchMetrics.PartitionMetrics(partitionName);
				pm.setRecordsProcessed(threadIndex * 100);
				metrics.getPartitionMetrics().put(partitionName, pm);
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		assertEquals(numThreads, metrics.getPartitionMetrics().size());
		for (int i = 0; i < numThreads; i++) {
			String partitionName = "partition-" + i;
			assertNotNull(metrics.getPartitionMetrics().get(partitionName));
			assertEquals(i * 100, metrics.getPartitionMetrics().get(partitionName).getRecordsProcessed());
		}
	}

	@Test
	void testBatchMetrics_ConcurrentSkipReasonCount() throws InterruptedException {
		BatchMetrics metrics = new BatchMetrics();
		int numThreads = 5;
		int incrementsPerThread = 20;

		Thread[] threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			final int threadIndex = i;
			threads[i] = new Thread(() -> {
				String exceptionType = "Exception" + (threadIndex % 2);
				for (int j = 0; j < incrementsPerThread; j++) {
					metrics.getSkipReasonCount().merge(exceptionType, 1, Integer::sum);
				}
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		int totalCount = metrics.getSkipReasonCount().values().stream().mapToInt(Integer::intValue).sum();
		assertEquals(numThreads * incrementsPerThread, totalCount);
	}

	@Test
	void testRecordSkip_WithNullJobExecutionId_ThrowsException() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.recordSkip(null, recordId, batchRecord, error, PARTITION_NAME, 10L));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testRecordSkip_WithNoMetricsFound_ThrowsException() {
		Long nonExistentJobId = 999L;
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.recordSkip(
						nonExistentJobId, recordId, batchRecord, error, PARTITION_NAME, 10L));

		assertTrue(exception.getMessage().contains("No metrics found for job execution ID: " + nonExistentJobId));
	}

	@Test
	void testRecordRetryAttempt_WithNullJobExecutionId_ThrowsException() {
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.recordRetryAttempt(
						null, recordId, batchRecord, 1, error, true, PARTITION_NAME));

		assertTrue(exception.getMessage().contains("Job execution ID cannot be null"));
	}

	@Test
	void testRecordRetryAttempt_WithNoMetricsFound_ThrowsException() {
		Long nonExistentJobId = 999L;
		Long recordId = 123L;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector.recordRetryAttempt(
						nonExistentJobId, recordId, batchRecord, 1, error, true, PARTITION_NAME));

		assertTrue(
				exception.getMessage().contains("No metrics found for job execution ID: " + nonExistentJobId));
	}

	@Test
	void testInitializePartitionMetrics_ExceptionHandling() {
		BatchMetricsCollector collector = new BatchMetricsCollector();
		collector.initializeMetrics(JOB_EXECUTION_ID);

		String extremelyLongPartitionName = "partition-" + "x".repeat(10000);

		assertDoesNotThrow(() -> collector.initializePartitionMetrics(JOB_EXECUTION_ID, extremelyLongPartitionName));
	}

	@Test
	void testCompletePartitionMetrics_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, PARTITION_NAME);

		long extremeWriteCount = Long.MAX_VALUE;
		String extremeExitCode = "EXIT-" + "x".repeat(10000);

		assertDoesNotThrow(() -> batchMetricsCollector.completePartitionMetrics(
				JOB_EXECUTION_ID, PARTITION_NAME, extremeWriteCount, extremeExitCode));
	}

	@Test
	void testFinalizeJobMetrics_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		long extremeTotalRead = Long.MAX_VALUE;
		long extremeTotalWritten = Long.MAX_VALUE;
		String extremeStatus = "STATUS-" + "x".repeat(10000);

		assertDoesNotThrow(() -> batchMetricsCollector.finalizeJobMetrics(
				JOB_EXECUTION_ID, extremeStatus, extremeTotalRead, extremeTotalWritten));
	}

	@Test
	void testRecordRetryAttempt_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Long recordId = Long.MAX_VALUE;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");

		String longMessage = "Error message: " + "x".repeat(100000);
		Throwable error = new RuntimeException(longMessage);

		assertDoesNotThrow(() -> batchMetricsCollector.recordRetryAttempt(
				JOB_EXECUTION_ID, recordId, batchRecord, Integer.MAX_VALUE, error, true, PARTITION_NAME));
	}

	@Test
	void testRecordSkip_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID);

		Long recordId = Long.MAX_VALUE;
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");

		String longMessage = "Skip error: " + "x".repeat(100000);
		Throwable error = new IllegalStateException(longMessage);
		Long lineNumber = Long.MAX_VALUE;

		assertDoesNotThrow(() -> batchMetricsCollector.recordSkip(
				JOB_EXECUTION_ID, recordId, batchRecord, error, PARTITION_NAME, lineNumber));
	}

	@Test
	void testCleanupOldMetrics_ExceptionHandling() {
		for (long i = 1; i <= 100; i++) {
			batchMetricsCollector.initializeMetrics(i);
		}

		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(10));

		assertNull(batchMetricsCollector.getJobMetrics(1L));
		assertNotNull(batchMetricsCollector.getJobMetrics(100L));
	}

	@Test
	void testCleanupOldMetrics_WithEmptyMap() {
		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(5));
	}

	@Test
	void testCleanupOldMetrics_WithExactKeepCount() {
		batchMetricsCollector.initializeMetrics(1L);
		batchMetricsCollector.initializeMetrics(2L);
		batchMetricsCollector.initializeMetrics(3L);

		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(3));

		assertNotNull(batchMetricsCollector.getJobMetrics(1L));
		assertNotNull(batchMetricsCollector.getJobMetrics(2L));
		assertNotNull(batchMetricsCollector.getJobMetrics(3L));
	}
}
