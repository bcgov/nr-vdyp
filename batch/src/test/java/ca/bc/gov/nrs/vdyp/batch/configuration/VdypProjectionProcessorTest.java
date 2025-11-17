package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

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

		// Set validation thresholds using reflection
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);

		setupStepExecution();
	}

	private void setupStepExecution() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(JOB_GUID);
		when(executionContext.getString("partitionName", "unknown")).thenReturn("test-partition");
		when(executionContext.getLong("startLine", 0)).thenReturn(1L);
		when(executionContext.getLong("endLine", 0)).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
	}

	@Test
	void testConstructor() {
		assertNotNull(processor);
	}

	@Test
	void testBeforeStep_InitializesProcessor() {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(jobExecution).getJobParameters();
		verify(jobParameters).getString("jobGuid");
		verify(stepExecution).getExecutionContext();
		verify(executionContext).getString("partitionName", "unknown");
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

	@ParameterizedTest
	@MethodSource("provideInvalidRecords")
	void testProcess_ValidationErrors_ThrowIllegalArgumentException(String testName, String featureId) {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId(featureId);

		assertThrows(IllegalArgumentException.class, () -> processor.process(batchRecord), testName);
	}

	static Stream<Arguments> provideInvalidRecords() {
		return Stream.of(Arguments.of("Missing Feature ID", (String) null), Arguments.of("Empty Feature ID", "   "));
	}

	@Test
	void testProcess_NullMetricsCollector_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(null);
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals(batchRecord, result); // Pass-through processing
	}

	@Test
	void testBeforeStep_WithNullMetricsCollector_DoesNotThrowException() {
		processor = new VdypProjectionProcessor(null);

		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName", "unknown");
		// No metrics collector calls should be made when metricsCollector is null
	}

	@Test
	void testProcess_ValidFeatureId_PassThroughProcessing() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		batchRecord.setFeatureId("VALID123");

		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals("VALID123", result.getFeatureId());
		assertEquals(batchRecord, result);
	}

	@Test
	void testProcess_EmptyFeatureId_ThrowsIllegalArgumentException() {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("");

		assertThrows(IllegalArgumentException.class, () -> processor.process(batchRecord));
	}

	@Test
	void testProcess_WhitespaceFeatureId_ThrowsIllegalArgumentException() {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("   ");

		assertThrows(IllegalArgumentException.class, () -> processor.process(batchRecord));
	}

	@Test
	void testBeforeStep_LogsPartitionInformation() {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getExecutionContext();
		verify(executionContext).getString("partitionName", "unknown");
	}

	@Test
	void testProcess_MultipleRecords_ProcessesAllSuccessfully() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord record1 = createValidBatchRecord();
		record1.setFeatureId("FEATURE001");
		BatchRecord result1 = processor.process(record1);
		assertNotNull(result1);
		assertEquals("FEATURE001", result1.getFeatureId());

		BatchRecord record2 = createValidBatchRecord();
		record2.setFeatureId("FEATURE002");
		BatchRecord result2 = processor.process(record2);
		assertNotNull(result2);
		assertEquals("FEATURE002", result2.getFeatureId());

		BatchRecord record3 = createValidBatchRecord();
		record3.setFeatureId("FEATURE003");
		BatchRecord result3 = processor.process(record3);
		assertNotNull(result3);
		assertEquals("FEATURE003", result3.getFeatureId());
	}

	private BatchRecord createValidBatchRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		return batchRecord;
	}
}
