package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

@ExtendWith(MockitoExtension.class)
class ProjectionNullPointerExceptionTest {

	@Mock
	private Logger logger;

	private String jobGuid;
	private Long jobExecutionId;
	private String partitionName;
	private NullPointerException causedBy;

	@BeforeEach
	void setUp() {
		jobGuid = "test-job-guid-123";
		jobExecutionId = 456L;
		partitionName = "partition-0";
		causedBy = new NullPointerException("Test NPE");
	}

	@Test
	void testConstructor_WithAllParameters() {
		String message = "Test exception message";
		int recordCount = 5;
		List<String> featureIds = Arrays.asList("F001", "F002", "F003", "F004", "F005");

		ProjectionNullPointerException exception = new ProjectionNullPointerException(
				message, causedBy, jobGuid, jobExecutionId, partitionName, recordCount, featureIds
		);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(causedBy, exception.getCause());
		assertEquals(jobGuid, exception.getJobGuid());
		assertEquals(jobExecutionId, exception.getJobExecutionId());
		assertEquals(partitionName, exception.getPartitionName());
		assertEquals(recordCount, exception.getRecordCount());
		assertEquals(featureIds, exception.getFeatureIds());
	}

	@Test
	void testGetters_ReturnCorrectValues() {
		String message = "Another test message";
		int recordCount = 3;
		List<String> featureIds = Arrays.asList("F100", "F200", "F300");

		ProjectionNullPointerException exception = new ProjectionNullPointerException(
				message, causedBy, jobGuid, jobExecutionId, partitionName, recordCount, featureIds
		);

		assertEquals(jobGuid, exception.getJobGuid());
		assertEquals(jobExecutionId, exception.getJobExecutionId());
		assertEquals(partitionName, exception.getPartitionName());
		assertEquals(recordCount, exception.getRecordCount());
		assertNotNull(exception.getFeatureIds());
		assertEquals(3, exception.getFeatureIds().size());
		assertEquals("F100", exception.getFeatureIds().get(0));
		assertEquals("F200", exception.getFeatureIds().get(1));
		assertEquals("F300", exception.getFeatureIds().get(2));
	}

	@Test
	void testHandleProjectionNullPointer_WithSmallNumberOfRecords() {
		// Create batch records with 5 feature IDs
		List<BatchRecord> batchRecords = createBatchRecords(Arrays.asList("F001", "F002", "F003", "F004", "F005"));

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		assertNotNull(result);
		assertEquals(jobGuid, result.getJobGuid());
		assertEquals(jobExecutionId, result.getJobExecutionId());
		assertEquals(partitionName, result.getPartitionName());
		assertEquals(5, result.getRecordCount());
		assertEquals(5, result.getFeatureIds().size());
		assertSame(causedBy, result.getCause());

		// Verify message contains all feature IDs (no truncation for <= 10 records)
		String message = result.getMessage();
		assertTrue(message.contains("F001"));
		assertTrue(message.contains("F002"));
		assertTrue(message.contains("F003"));
		assertTrue(message.contains("F004"));
		assertTrue(message.contains("F005"));
		assertTrue(message.contains(jobGuid));
		assertTrue(message.contains(String.valueOf(jobExecutionId)));
		assertTrue(message.contains(partitionName));
		assertTrue(message.contains("Records: 5"));

		// Verify logger was called
		ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
		verify(logger).error(logCaptor.capture());
		verify(logger).error("NPE Stack Trace:", causedBy);
		verify(logger).debug(anyString(), eq(5), any(List.class));

		String loggedMessage = logCaptor.getValue();
		assertTrue(loggedMessage.contains("NullPointerException in projection"));
	}

	@Test
	void testHandleProjectionNullPointer_WithLargeNumberOfRecords() {
		// Create batch records with 15 feature IDs
		List<String> featureIds = Arrays.asList(
				"F001", "F002", "F003", "F004", "F005", "F006", "F007", "F008", "F009", "F010", "F011", "F012", "F013",
				"F014", "F015"
		);
		List<BatchRecord> batchRecords = createBatchRecords(featureIds);

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		assertNotNull(result);
		assertEquals(15, result.getRecordCount());
		assertEquals(15, result.getFeatureIds().size());

		// Verify message contains only first 10 feature IDs + "... and 5 more"
		String message = result.getMessage();
		assertTrue(message.contains("F001"));
		assertTrue(message.contains("F010"));
		assertTrue(message.contains("... and 5 more"));
		// F011-F015 should NOT be in the preview message
		assertTrue(!message.contains("F011") || message.contains("... and 5 more"));
	}

	@Test
	void testHandleProjectionNullPointer_WithExactly10Records() {
		// Create batch records with exactly 10 feature IDs
		List<String> featureIds = Arrays
				.asList("F001", "F002", "F003", "F004", "F005", "F006", "F007", "F008", "F009", "F010");
		List<BatchRecord> batchRecords = createBatchRecords(featureIds);

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		assertNotNull(result);
		assertEquals(10, result.getRecordCount());

		// With exactly 10 records, should show all without "... and X more"
		String message = result.getMessage();
		assertTrue(message.contains("F001"));
		assertTrue(message.contains("F010"));
		assertTrue(!message.contains("... and"));
	}

	@Test
	void testHandleProjectionNullPointer_WithNullNPEMessage() {
		// Create NPE with null message
		NullPointerException npeWithNullMessage = new NullPointerException();
		List<BatchRecord> batchRecords = createBatchRecords(Arrays.asList("F001", "F002"));

		ProjectionNullPointerException result = ProjectionNullPointerException.handleProjectionNullPointer(
				npeWithNullMessage, batchRecords, jobExecutionId, jobGuid, partitionName, logger
		);

		assertNotNull(result);
		String message = result.getMessage();
		assertTrue(message.contains("NPE message: No message"));
	}

	@Test
	void testHandleProjectionNullPointer_WithEmptyRecordList() {
		List<BatchRecord> emptyRecords = Collections.emptyList();

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, emptyRecords, jobExecutionId, jobGuid, partitionName, logger);

		assertNotNull(result);
		assertEquals(0, result.getRecordCount());
		assertEquals(0, result.getFeatureIds().size());

		String message = result.getMessage();
		assertTrue(message.contains("Records: 0"));
	}

	@Test
	void testHandleProjectionNullPointer_WithSingleRecord() {
		List<BatchRecord> singleRecord = createBatchRecords(Arrays.asList("F999"));

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, singleRecord, jobExecutionId, jobGuid, partitionName, logger);

		assertNotNull(result);
		assertEquals(1, result.getRecordCount());
		assertEquals(1, result.getFeatureIds().size());
		assertEquals("F999", result.getFeatureIds().get(0));

		String message = result.getMessage();
		assertTrue(message.contains("F999"));
		assertTrue(message.contains("Records: 1"));
	}

	@Test
	void testHandleProjectionNullPointer_LoggerCalls() {
		List<BatchRecord> batchRecords = createBatchRecords(Arrays.asList("F001", "F002", "F003"));

		ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		// Verify all three logger calls
		verify(logger, times(1)).error(anyString()); // contextual message
		verify(logger, times(1)).error("NPE Stack Trace:", causedBy); // stack trace
		verify(logger, times(1)).debug(anyString(), eq(3), any(List.class)); // debug with all feature IDs
	}

	@Test
	void testHandleProjectionNullPointer_MessageFormat() {
		List<BatchRecord> batchRecords = createBatchRecords(Arrays.asList("FEAT-001", "FEAT-002"));

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		String message = result.getMessage();

		// Verify all required components are in the message
		assertTrue(message.contains("NullPointerException in projection"));
		assertTrue(message.contains("JobGuid: " + jobGuid));
		assertTrue(message.contains("JobExeId: " + jobExecutionId));
		assertTrue(message.contains("Partition: " + partitionName));
		assertTrue(message.contains("Records: 2"));
		assertTrue(message.contains("NPE message: Test NPE"));
		assertTrue(message.contains("FEATURE_IDs in chunk:"));
		assertTrue(message.contains("FEAT-001"));
		assertTrue(message.contains("FEAT-002"));
	}

	@Test
	void testHandleProjectionNullPointer_With11Records_ShowsTruncation() {
		// Create batch records with 11 feature IDs (just over the threshold)
		List<String> featureIds = Arrays
				.asList("F001", "F002", "F003", "F004", "F005", "F006", "F007", "F008", "F009", "F010", "F011");
		List<BatchRecord> batchRecords = createBatchRecords(featureIds);

		ProjectionNullPointerException result = ProjectionNullPointerException
				.handleProjectionNullPointer(causedBy, batchRecords, jobExecutionId, jobGuid, partitionName, logger);

		String message = result.getMessage();

		// Should show first 10 + "... and 1 more"
		assertTrue(message.contains("F001"));
		assertTrue(message.contains("F010"));
		assertTrue(message.contains("... and 1 more"));
	}

	// Helper method to create batch records
	private List<BatchRecord> createBatchRecords(List<String> featureIds) {
		List<BatchRecord> batchRecords = new ArrayList<>();
		for (String featureId : featureIds) {
			BatchRecord batchRecord = new BatchRecord();
			batchRecord.setFeatureId(featureId);
			batchRecords.add(batchRecord);
		}
		return batchRecords;
	}
}
