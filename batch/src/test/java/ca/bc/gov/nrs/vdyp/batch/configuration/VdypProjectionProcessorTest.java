package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypProjectionProcessorTest {

	@Mock
	private BatchRetryPolicy retryPolicy;

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private ExecutionContext executionContext;

	private VdypProjectionProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new VdypProjectionProcessor(retryPolicy, metricsCollector);

		// Set validation thresholds using reflection
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);

		setupStepExecution();
	}

	private void setupStepExecution() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
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
		verify(executionContext).getString("partitionName", "unknown");
		verify(executionContext).getLong("startLine", 0);
		verify(executionContext).getLong("endLine", 0);
		verify(metricsCollector).initializePartitionMetrics(1L, "test-partition", 1, 100);
	}

	@Test
	void testProcess_ValidRecord_ReturnsProcessedRecord() throws Exception {
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals(batchRecord, result); // Pass-through processing
		verify(retryPolicy).registerRecord(anyLong(), eq(batchRecord));
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
		return Stream.of(
				Arguments.of("Missing Feature ID", (String) null),
				Arguments.of("Empty Feature ID", "   "));
	}

	@Test
	void testProcess_NullComponents_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(null, null);
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
	void testProcess_WithNullRetryPolicy_ProcessesSuccessfully() throws Exception {
		processor = new VdypProjectionProcessor(null, metricsCollector);
		ReflectionTestUtils.setField(processor, "maxDataLength", 50000);
		ReflectionTestUtils.setField(processor, "minPolygonIdLength", 1);
		ReflectionTestUtils.setField(processor, "maxPolygonIdLength", 50);
		processor.beforeStep(stepExecution);

		BatchRecord batchRecord = createValidBatchRecord();
		BatchRecord result = processor.process(batchRecord);

		assertNotNull(result);
		assertEquals(batchRecord, result); // Pass-through processing
		// No retry policy calls should be made since retryPolicy is null
	}

	@Test
	void testBeforeStep_WithNullMetricsCollector_DoesNotThrowException() {
		processor = new VdypProjectionProcessor(retryPolicy, null);

		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName", "unknown");
		verify(executionContext).getLong("startLine", 0);
		verify(executionContext).getLong("endLine", 0);
		// No metrics collector calls should be made
	}

	private BatchRecord createValidBatchRecord() {
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("12345678901");
		return batchRecord;
	}
}
