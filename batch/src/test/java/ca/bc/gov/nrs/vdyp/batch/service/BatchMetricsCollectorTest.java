package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
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
	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
	private static final String PARTITION_NAME = "partition-1";
	private static final String EXIT_CODE = "COMPLETED";

	@BeforeEach
	void setUp() {
		// Clean up any existing metrics from previous tests
		batchMetricsCollector.cleanupOldMetrics(0);
	}

	@Test
	void testInitializeMetrics() {
		BatchMetrics metrics = batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		assertNotNull(metrics);
		assertEquals(JOB_EXECUTION_ID, metrics.getJobExecutionId());
		assertEquals("STARTING", metrics.getStatus());
		assertNotNull(metrics.getStartTime());

		BatchMetrics retrievedMetrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertSame(metrics, retrievedMetrics);
	}

	@Test
	void testInitializePartitionMetrics() {
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
	void testCompletePartitionMetrics() {
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
	void testFinalizeJobMetrics() {
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
	void testRecordRetryAttempt() {
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
	void testRecordSkip() {
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
	void testCleanupOldMetrics() {
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
	void testMultiplePartitions() {
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
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.initializePartitionMetrics(JOB_EXECUTION_ID, nonExistentGuid, PARTITION_NAME)
		);

		assertTrue(exception.getMessage().contains("No metrics found for job"));
	}

	@Test
	void testCompletePartitionMetrics_NoMetricsFound() {
		String nonExistentGuid = "non-existent-guid-for-complete";
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.completePartitionMetrics(JOB_EXECUTION_ID, nonExistentGuid, PARTITION_NAME, 100L, EXIT_CODE)
		);

		assertTrue(exception.getMessage().contains("No metrics found for job"));
	}

	@Test
	void testCompletePartitionMetrics_NoPartitionMetricsFound() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.completePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, "unknown-partition", 100L, EXIT_CODE)
		);

		assertTrue(exception.getMessage().contains("Partition metrics not found for partition"));
	}

	@Test
	void testFinalizeJobMetrics_NoMetricsFound() {
		String nonExistentGuid = "non-existent-guid-for-finalize";
		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.finalizeJobMetrics(JOB_EXECUTION_ID, nonExistentGuid, "COMPLETED", 100L, 95L)
		);

		assertTrue(exception.getMessage().contains("No metrics found for job"));
	}

	@Test
	void testRecordRetryAttempt_NullError() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		Exception exception = assertThrows(
				Exception.class,
				() -> batchMetricsCollector
						.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, 1, null, false, PARTITION_NAME)
		);

		assertNotNull(exception);
	}

	@Test
	void testRecordSkip_NullError() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		Exception exception = assertThrows(
				Exception.class,
				() -> batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432109", null, PARTITION_NAME)
		);

		assertNotNull(exception);
	}

	@Test
	void testCleanupOldMetrics_NegativeKeepCount() {
		Exception exception = assertThrows(BatchException.class, () -> batchMetricsCollector.cleanupOldMetrics(-1));

		assertTrue(exception.getMessage().contains("Keep count must be non-negative"));
	}

	@Test
	void testCleanupOldMetrics_ZeroKeepCount() {
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
	void testCleanupOldMetrics_KeepCountGreaterThanSize() {
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
	void testRecordRetryAttempt_Failed() {
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
	void testRecordSkip_MultipleSkipsWithDifferentErrors() {
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
				JOB_GUID, JOB_EXECUTION_ID, partitionName, recordCount, cause, errorDescription, logger
		);

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
				JOB_GUID, JOB_EXECUTION_ID, partitionName, recordCount, cause, errorDescription, logger
		);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("records=0"));
		assertTrue(result.getMessage().contains("No records"));
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
	void testBatchMetrics_ParameterizedConstructor() {
		Long jobId = 12345L;
		BatchMetrics metrics = new BatchMetrics(jobId, JOB_GUID);

		assertEquals(jobId, metrics.getJobExecutionId());
		assertNotNull(metrics.getStartTime());
		assertEquals("STARTING", metrics.getStatus());
	}

	@Test
	void testBatchMetrics_SettersAndGetters() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

		assertEquals(999L, metrics.getJobExecutionId());

		LocalDateTime endTime = metrics.getStartTime().plusHours(1);
		metrics.setEndTime(endTime);
		assertEquals(endTime, metrics.getEndTime());

		metrics.setStatus("COMPLETED");
		assertEquals("COMPLETED", metrics.getStatus());

		metrics.setAverageProcessingTime(123.45);
		assertEquals(123.45, metrics.getAverageProcessingTime());
	}

	@Test
	void testBatchMetrics_RetryCounters() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

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
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

		metrics.setTotalSkips(10);
		assertEquals(10, metrics.getTotalSkips());

		int newSkips = metrics.incrementSkips();
		assertEquals(11, newSkips);
		assertEquals(11, metrics.getTotalSkips());
	}

	@Test
	void testBatchMetrics_RecordCounters() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

		metrics.setTotalRecordsProcessed(1000L);
		assertEquals(1000L, metrics.getTotalRecordsProcessed());

		metrics.setTotalRecordsRead(1100L);
		assertEquals(1100L, metrics.getTotalRecordsRead());

		metrics.setTotalRecordsWritten(950L);
		assertEquals(950L, metrics.getTotalRecordsWritten());
	}

	@Test
	void testBatchMetrics_RetryDetails() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

		List<BatchMetrics.RetryDetail> retryDetails = new ArrayList<>();
		retryDetails.add(new BatchMetrics.RetryDetail(1, "RuntimeException", "Error 1", true, "partition-1"));
		retryDetails.add(new BatchMetrics.RetryDetail(2, "IllegalStateException", "Error 2", false, "partition-2"));

		metrics.setRetryDetails(retryDetails);

		assertEquals(2, metrics.getRetryDetails().size());
		List<BatchMetrics.RetryDetail> detailsList = new ArrayList<>(metrics.getRetryDetails());
		assertEquals(1, detailsList.get(0).attemptNumber());
		assertEquals("RuntimeException", detailsList.get(0).errorType());
		assertTrue(detailsList.get(0).successful());
		assertEquals("partition-1", detailsList.get(0).partitionName());

		metrics.setRetryDetails(null);
		assertTrue(metrics.getRetryDetails().isEmpty());
	}

	@Test
	void testBatchMetrics_SkipDetails() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

		List<BatchMetrics.SkipDetail> skipDetails = new ArrayList<>();
		skipDetails.add(new BatchMetrics.SkipDetail("skip-data1", "RuntimeException", "Skip error 1", "partition-1"));
		skipDetails.add(
				new BatchMetrics.SkipDetail("skip-data2", "IllegalArgumentException", "Skip error 2", "partition-2")
		);

		metrics.setSkipDetails(skipDetails);

		assertEquals(2, metrics.getSkipDetails().size());
		List<BatchMetrics.SkipDetail> detailsList = new ArrayList<>(metrics.getSkipDetails());
		assertEquals("skip-data1", detailsList.get(0).featureId());
		assertEquals("RuntimeException", detailsList.get(0).errorType());
		assertEquals("Skip error 1", detailsList.get(0).errorMessage());
		assertEquals("partition-1", detailsList.get(0).partitionName());

		metrics.setSkipDetails(null);
		assertTrue(metrics.getSkipDetails().isEmpty());
	}

	@Test
	void testBatchMetrics_SkipReasonCount() {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

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
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);

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

		LocalDateTime endTime = partitionMetrics.getStartTime().plusHours(2);
		partitionMetrics.setEndTime(endTime);
		assertEquals(endTime, partitionMetrics.getEndTime());

		partitionMetrics.setExitCode("FAILED");
		assertEquals("FAILED", partitionMetrics.getExitCode());
	}

	@Test
	void testBatchMetrics_RetryDetail() {
		LocalDateTime timestamp = LocalDateTime.now();
		BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
				2, "TestException", "Test error message", timestamp, true, "partition-test"
		);

		assertEquals(2, retryDetail.attemptNumber());
		assertEquals("TestException", retryDetail.errorType());
		assertEquals("Test error message", retryDetail.errorMessage());
		assertEquals(timestamp, retryDetail.timestamp());
		assertTrue(retryDetail.successful());
		assertEquals("partition-test", retryDetail.partitionName());
	}

	@Test
	void testBatchMetrics_SkipDetail() {
		LocalDateTime timestamp = LocalDateTime.now();
		BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
				"123456789", "TestSkipException", "Test skip error", timestamp, "partition-3"
		);

		assertEquals("123456789", skipDetail.featureId());
		assertEquals("TestSkipException", skipDetail.errorType());
		assertEquals("Test skip error", skipDetail.errorMessage());
		assertNotNull(skipDetail.timestamp());
		assertEquals("partition-3", skipDetail.partitionName());
	}

	@Test
	void testBatchMetrics_ThreadSafety_RetryCounters() throws InterruptedException {
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);
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
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);
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
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);
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
		BatchMetrics metrics = new BatchMetrics(999L, JOB_GUID);
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
	void testRecordSkip_WithNoMetricsFound_ThrowsException() {
		String nonExistentGuid = "non-existent-guid-999";
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.recordSkip(JOB_EXECUTION_ID, nonExistentGuid, "123456789", error, PARTITION_NAME)
		);

		assertTrue(exception.getMessage().contains("No metrics found for job"));
	}

	@Test
	void testRecordRetryAttempt_WithNoMetricsFound_ThrowsException() {
		String nonExistentGuid = "non-existent-guid-999";
		Throwable error = new RuntimeException("Test error");

		Exception exception = assertThrows(
				BatchException.class,
				() -> batchMetricsCollector
						.recordRetryAttempt(JOB_EXECUTION_ID, nonExistentGuid, 1, error, true, PARTITION_NAME)
		);

		assertTrue(exception.getMessage().contains("No metrics found for job"));
	}

	@Test
	void testInitializePartitionMetrics_ExceptionHandling() {
		BatchMetricsCollector collector = new BatchMetricsCollector();
		collector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		String extremelyLongPartitionName = "partition-" + "x".repeat(10000);

		assertDoesNotThrow(
				() -> collector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, extremelyLongPartitionName)
		);
	}

	@Test
	void testCompletePartitionMetrics_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME);

		long extremeWriteCount = Long.MAX_VALUE;
		String extremeExitCode = "EXIT-" + "x".repeat(10000);

		assertDoesNotThrow(
				() -> batchMetricsCollector.completePartitionMetrics(
						JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME, extremeWriteCount, extremeExitCode
				)
		);
	}

	@Test
	void testFinalizeJobMetrics_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		long extremeTotalRead = Long.MAX_VALUE;
		long extremeTotalWritten = Long.MAX_VALUE;
		String extremeStatus = "STATUS-" + "x".repeat(10000);

		assertDoesNotThrow(
				() -> batchMetricsCollector.finalizeJobMetrics(
						JOB_EXECUTION_ID, JOB_GUID, extremeStatus, extremeTotalRead, extremeTotalWritten
				)
		);
	}

	@Test
	void testRecordRetryAttempt_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		String longMessage = "Error message: " + "x".repeat(100000);
		Throwable error = new RuntimeException(longMessage);

		assertDoesNotThrow(
				() -> batchMetricsCollector
						.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, Integer.MAX_VALUE, error, true, PARTITION_NAME)
		);
	}

	@Test
	void testRecordSkip_ExceptionHandling() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		String longMessage = "Skip error: " + "x".repeat(100000);
		Throwable error = new IllegalStateException(longMessage);

		assertDoesNotThrow(
				() -> batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "123456789", error, PARTITION_NAME)
		);
	}

	@Test
	void testCleanupOldMetrics_ExceptionHandling() {
		for (long i = 1; i <= 100; i++) {
			batchMetricsCollector.initializeMetrics(i, "guid-" + i);
		}

		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(10));

		assertFalse(batchMetricsCollector.isJobMetricsPresent("guid-1"));
		assertNotNull(batchMetricsCollector.getJobMetrics("guid-100"));
	}

	@Test
	void testCleanupOldMetrics_WithEmptyMap() {
		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(5));
	}

	@Test
	void testCleanupOldMetrics_WithExactKeepCount() {
		String guid1 = "exact-keep-guid-1";
		String guid2 = "exact-keep-guid-2";
		String guid3 = "exact-keep-guid-3";
		batchMetricsCollector.initializeMetrics(1L, guid1);
		batchMetricsCollector.initializeMetrics(2L, guid2);
		batchMetricsCollector.initializeMetrics(3L, guid3);

		assertDoesNotThrow(() -> batchMetricsCollector.cleanupOldMetrics(3));

		assertNotNull(batchMetricsCollector.getJobMetrics(guid1));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid2));
		assertNotNull(batchMetricsCollector.getJobMetrics(guid3));
	}

	@Test
	void testInitializeMetrics_WithVeryLongJobGuid() {
		String veryLongGuid = "guid-" + "x".repeat(10000);

		BatchMetrics metrics = assertDoesNotThrow(
				() -> batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, veryLongGuid)
		);

		assertNotNull(metrics);
		assertEquals(JOB_EXECUTION_ID, metrics.getJobExecutionId());
	}

	@Test
	void testFinalizeJobMetrics_WithDifferentStatuses() {
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
	void testRecordRetryAttempt_MultipleRetries() {
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
	void testRecordSkip_MultipleSkipsFromSamePartition() {
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
	void testMultiplePartitions_WithDifferentMetrics() {
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

	@Test
	void testCleanupOldMetrics_WithSingleEntry() {
		String guid = "single-entry-guid";
		batchMetricsCollector.initializeMetrics(1L, guid);

		batchMetricsCollector.cleanupOldMetrics(1);

		assertNotNull(batchMetricsCollector.getJobMetrics(guid));
	}

	@Test
	void testBatchMetrics_PartitionMetrics_DefaultConstructor() {
		BatchMetrics.PartitionMetrics pm = new BatchMetrics.PartitionMetrics("test-partition");

		assertEquals("test-partition", pm.getPartitionName());
		assertNotNull(pm.getStartTime());
		assertNull(pm.getEndTime());
		assertEquals(0, pm.getRecordsProcessed());
		assertEquals(0, pm.getRecordsRead());
		assertEquals(0, pm.getRecordsWritten());
		assertEquals(0, pm.getRetryCount());
		assertEquals(0, pm.getSkipCount());
		assertNull(pm.getExitCode());
	}

	@Test
	void testBatchMetrics_JobGuidAccessor() {
		BatchMetrics metrics = new BatchMetrics(JOB_EXECUTION_ID, JOB_GUID);

		assertEquals(JOB_GUID, metrics.getJobGuid());
	}

	@Test
	void testInitializeMetrics_ReturnsCorrectMetrics() {
		BatchMetrics metrics1 = batchMetricsCollector.initializeMetrics(100L, "guid-100");
		BatchMetrics metrics2 = batchMetricsCollector.initializeMetrics(200L, "guid-200");

		assertEquals(100L, metrics1.getJobExecutionId());
		assertEquals("guid-100", metrics1.getJobGuid());
		assertEquals(200L, metrics2.getJobExecutionId());
		assertEquals("guid-200", metrics2.getJobGuid());

		assertNotSame(metrics1, metrics2);
	}

	@Test
	void testCompletePartitionMetrics_UpdatesTimestamp() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);
		batchMetricsCollector.initializePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		BatchMetrics.PartitionMetrics pm = metrics.getPartitionMetrics().get(PARTITION_NAME);

		assertNotNull(pm.getStartTime());
		assertNull(pm.getEndTime());

		batchMetricsCollector.completePartitionMetrics(JOB_EXECUTION_ID, JOB_GUID, PARTITION_NAME, 100L, "COMPLETED");

		assertNotNull(pm.getEndTime());
		assertTrue(pm.getEndTime().isAfter(pm.getStartTime()) || pm.getEndTime().isEqual(pm.getStartTime()));
	}

	@Test
	void testFinalizeJobMetrics_UpdatesTimestamp() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		assertNotNull(metrics.getStartTime());
		assertNull(metrics.getEndTime());

		batchMetricsCollector.finalizeJobMetrics(JOB_EXECUTION_ID, JOB_GUID, "COMPLETED", 100L, 95L);

		assertNotNull(metrics.getEndTime());
		assertTrue(
				metrics.getEndTime().isAfter(metrics.getStartTime())
						|| metrics.getEndTime().isEqual(metrics.getStartTime())
		);
	}

	@Test
	void testBatchMetrics_GetJobGuid() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);

		assertEquals(JOB_GUID, metrics.getJobGuid());
	}

	@Test
	void testBatchMetrics_GetRetryDetailsList() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		Exception testException = new RuntimeException("Test retry error");
		batchMetricsCollector.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, 1, testException, true, PARTITION_NAME);
		batchMetricsCollector.recordRetryAttempt(JOB_EXECUTION_ID, JOB_GUID, 2, testException, false, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		var retryDetailsList = metrics.getRetryDetailsList();

		assertNotNull(retryDetailsList);
		assertEquals(2, retryDetailsList.size());

		var firstList = metrics.getRetryDetailsList();
		var secondList = metrics.getRetryDetailsList();
		assertNotSame(firstList, secondList);
		assertEquals(firstList.size(), secondList.size());
	}

	@Test
	void testBatchMetrics_GetSkipDetailsList() {
		batchMetricsCollector.initializeMetrics(JOB_EXECUTION_ID, JOB_GUID);

		Exception testException = new RuntimeException("Test skip error");

		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "123456789", testException, PARTITION_NAME);
		batchMetricsCollector.recordSkip(JOB_EXECUTION_ID, JOB_GUID, "98765432109", testException, PARTITION_NAME);

		BatchMetrics metrics = batchMetricsCollector.getJobMetrics(JOB_GUID);
		var skipDetailsList = metrics.getSkipDetailsList();

		assertNotNull(skipDetailsList);
		assertEquals(2, skipDetailsList.size());

		var firstList = metrics.getSkipDetailsList();
		var secondList = metrics.getSkipDetailsList();
		assertNotSame(firstList, secondList);
		assertEquals(firstList.size(), secondList.size());
	}
}
