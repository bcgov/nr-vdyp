package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypProjectionProcessorTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobParameters jobParameters;

	private VdypProjectionProcessor processor;

	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		processor = new VdypProjectionProcessor(metricsCollector);

		setupStepExecution();
	}

	private void setupStepExecution() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(JOB_GUID);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		when(executionContext.getLong("startLine", 0)).thenReturn(1L);
		when(executionContext.getLong("endLine", 0)).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
	}

	@Test
	void testConstructor() {
		assertNotNull(processor);
	}

	@Test
	void testBeforeStep_InitializesProcessor() throws BatchException {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(jobExecution).getJobParameters();
		verify(jobParameters).getString("jobGuid");
		verify(stepExecution).getExecutionContext();
		verify(executionContext).getString("partitionName");
		verify(metricsCollector).initializePartitionMetrics(1L, JOB_GUID, "test-partition");
	}

	@Test
	void testProcess_ValidRecord_ReturnsProcessedRecord() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals(batchRecord, result); // Pass-through processing
	}

	static Stream<Arguments> provideInvalidRecords() {
		return Stream.of(Arguments.of("Missing Feature ID", (String) null), Arguments.of("Empty Feature ID", "   "));
	}

	@Test
	void testProcess_NullMetricsCollector_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(null);
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals(batchRecord, result); // Pass-through processing
	}

	@Test
	void testBeforeStep_WithNullMetricsCollector_DoesNotThrowException() throws BatchException {
		processor = new VdypProjectionProcessor(null);

		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName");
		// No metrics collector calls should be made when metricsCollector is null
	}

	@Test
	void testProcess_ValidFeatureId_PassThroughProcessing() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord(
				"VALID123", "12345678901,MAP1", Collections.emptyList(), "test-partition"
		);

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals("VALID123", result.getFeatureId());
		assertEquals(batchRecord, result);
	}

	@Test
	void testBeforeStep_LogsPartitionInformation() throws BatchException {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getExecutionContext();
		verify(executionContext).getString("partitionName");
	}

	@Test
	void testProcess_MultipleRecords_ProcessesAllSuccessfully() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord record1 = new BatchRecord(
				"FEATURE001", "12345678901,MAP1", Collections.emptyList(), "test-partition"
		);
		BatchRecord result1 = processor.process(record1);
		assertNotNull(result1);
		assertEquals("FEATURE001", result1.getFeatureId());

		BatchRecord record2 = new BatchRecord(
				"FEATURE002", "12345678901,MAP1", Collections.emptyList(), "test-partition"
		);
		BatchRecord result2 = processor.process(record2);
		assertNotNull(result2);
		assertEquals("FEATURE002", result2.getFeatureId());

		BatchRecord record3 = new BatchRecord(
				"FEATURE003", "12345678901,MAP1", Collections.emptyList(), "test-partition"
		);
		BatchRecord result3 = processor.process(record3);
		assertNotNull(result3);
		assertEquals("FEATURE003", result3.getFeatureId());
	}

	@Test
	void testBeforeStep_CalledTwice_ThrowsIllegalStateException() throws BatchException {
		processor.beforeStep(stepExecution);

		assertThrows(
				IllegalStateException.class, () -> processor.beforeStep(stepExecution),
				"VdypProjectionProcessor already initialized. beforeStep() should only be called once."
		);
	}

	private BatchRecord createValidBatchRecord() {
		return new BatchRecord("12345678901", "12345678901,MAP1", Collections.emptyList(), "test-partition");
	}
}
